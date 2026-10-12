package com.kalpande.algopulse.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class NotPublishedException(market: Market) :
    IOException("${market.label}: no dashboard published yet")

data class Loaded(val data: MarketData, val bytes: Int, val fetchedAt: Instant, val fromCache: Boolean)

/**
 * Downloads a market's dashboard.json.gz (public GitHub release asset), keeps the last
 * good copy on disk so the app opens instantly and works offline.
 */
class DashboardRepository(private val context: Context) {

    private val cacheDir = File(context.filesDir, "dashboards").apply { mkdirs() }

    private fun cacheFile(m: Market) = File(cacheDir, "${m.key}.json.gz")

    suspend fun loadCached(m: Market): Loaded? = withContext(Dispatchers.IO) {
        val f = cacheFile(m)
        if (!f.exists()) return@withContext null
        runCatching {
            val bytes = f.readBytes()
            Loaded(MarketData(m, parse(bytes)), bytes.size, Instant.ofEpochMilli(f.lastModified()), fromCache = true)
        }.getOrNull()
    }

    suspend fun fetch(m: Market, url: String): Loaded = withContext(Dispatchers.IO) {
        // The publisher replaces the asset every run (delete, then upload), so a 404 may just be mid-swap: look again once
        val bytes = download(m, url) ?: run { delay(3_000); download(m, url) } ?: throw NotPublishedException(m)
        val dto = parse(bytes)
        val tmp = File(cacheDir, "${m.key}.tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(cacheFile(m))
        Loaded(MarketData(m, dto), bytes.size, Instant.now(), fromCache = false)
    }

    /** The asset's bytes, or null when it isn't there (404). */
    private fun download(m: Market, url: String): ByteArray? {
        val req = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
        return client.newCall(req).execute().use { resp ->
            if (resp.code == 404) return@use null
            if (!resp.isSuccessful) throw IOException("${m.label}: HTTP ${resp.code}")
            resp.body?.bytes() ?: throw IOException("${m.label}: empty response")
        }
    }

    /**
     * Cheap freshness probe for background checks: the release asset's upload time from the
     * GitHub API (a few KB instead of the full dashboard). Null when it can't be determined.
     */
    suspend fun assetUpdatedAt(url: String): Instant? = withContext(Dispatchers.IO) {
        val m = Regex("""github\.com/([^/]+)/([^/]+)/releases/download/([^/]+)/([^/?#]+)""").find(url)
            ?: return@withContext null
        val (owner, repo, tag, asset) = m.destructured
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/tags/$tag")
            .header("Accept", "application/vnd.github+json")
            .build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
                root["assets"]?.jsonArray
                    ?.map { it.jsonObject }
                    ?.firstOrNull { (it["name"]?.jsonPrimitive?.content) == asset }
                    ?.let { a: JsonObject -> parseInstant(a["updated_at"]?.jsonPrimitive?.content) }
            }
        }.getOrNull()
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
            isLenient = true
        }

        val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        /** Accepts gzip (normal) or plain JSON. */
        fun parse(bytes: ByteArray): DashboardDto {
            val text = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
                GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes().decodeToString() }
            } else {
                bytes.decodeToString()
            }
            return json.decodeFromString(DashboardDto.serializer(), text)
        }
    }
}
