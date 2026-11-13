package com.voicenote.transcriber

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

data class TranscriptResponse(val transcript: String)

interface TranscribeApi {
    @Multipart
    @POST("transcribe")
    suspend fun transcribe(@Part audio: MultipartBody.Part): TranscriptResponse
}

object ApiClient {

    fun create(baseUrl: String, debugLogging: Boolean): TranscribeApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .apply {
                if (debugLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranscribeApi::class.java)
    }
}
