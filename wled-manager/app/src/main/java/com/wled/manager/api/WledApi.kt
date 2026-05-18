package com.wled.manager.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wled.manager.model.WledInfo
import com.wled.manager.model.WledState
import com.wled.manager.model.StateCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class WledApi(private val ip: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "http://$ip"
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun getState(): Result<WledState> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/json/state").get().build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "{}" }
            gson.fromJson(body, WledState::class.java)
        }
    }

    suspend fun getInfo(): Result<WledInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/json/info").get().build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "{}" }
            gson.fromJson(body, WledInfo::class.java)
        }
    }

    suspend fun getEffects(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/json/effects").get().build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "[]" }
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(body, type)
        }
    }

    suspend fun getPalettes(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/json/palettes").get().build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "[]" }
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(body, type)
        }
    }

    suspend fun setState(command: StateCommand): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = gson.toJson(command)
            val body = json.toRequestBody(jsonType)
            val request = Request.Builder().url("$baseUrl/json/state").post(body).build()
            client.newCall(request).execute().use { }
        }
    }

    suspend fun setPower(on: Boolean) = setState(StateCommand(on = on))

    suspend fun setBrightness(bri: Int) = setState(StateCommand(brightness = bri.coerceIn(0, 255)))

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/json/info").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
