package cn.hkim.addon.utils.skyblock

import cn.hkim.addon.Hkim
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture

object MayorData {
    private const val API_URL = "https://api.hypixel.net/v2/resources/skyblock/election"
    private val gson = Gson()
    private var data: MayorResponse? = null

    data class MayorResponse(
        val success: Boolean,
        val mayor: Mayor?
    )

    data class Mayor(
        val name: String,
        val perks: List<Perk>,
        val minister: Minister?
    )

    data class Minister(
        val name: String,
        val perk: Perk
    )

    data class Perk(
        val name: String,
        val minister: Boolean = false
    )

    fun fetchData() {
        CompletableFuture.runAsync {
            try {
                val url = URI.create(API_URL).toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()

                data = gson.fromJson(response, MayorResponse::class.java)
                Hkim.logger.info("Successfully fetched mayor data from Hypixel API!")
            } catch (e: Exception) {
                Hkim.logger.error("Failed to fetch mayor data", e)
            }
        }
    }

    fun hasPerk(mayorName: String, perkName: String): Boolean {
        val mayor = data?.mayor ?: return false
        val minister = mayor.minister

        if (mayor.name.equals(mayorName, ignoreCase = true) &&
            mayor.perks.any { it.name.equals(perkName, ignoreCase = true) }
        ) return true

        if (minister != null &&
            minister.name.equals(mayorName, ignoreCase = true) &&
            minister.perk.name.equals(perkName, ignoreCase = true) &&
            minister.perk.minister
        ) return true

        return false
    }

    val pestSpawnCooldown: Int
        get() = if (hasPerk("Finnegan", "Pest Eradicator")) 75 else 135

    val pestExchangeTime: Long
        get() = if (hasPerk("Finnegan", "Pest Eradicator")) 3_600_000 else 1_800_000

    val aatroxMultiplier: Double
        get() = if (hasPerk("Aatrox", "Slayer XP Buff")) 1.25 else 1.0
}