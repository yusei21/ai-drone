package com.sloth.registerapp.core.network

import android.content.Context
import com.sloth.registerapp.core.auth.TokenRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://luanna-retroserrulate-charleigh.ngrok-free.dev/api/" // TODO: Replace with actual IP address
    //private const val BASE_URL = "http://10.1.8.115:5000/api/"
    private lateinit var apiService: SdiaApiService

    fun baseUrl(): String = BASE_URL

    fun getInstance(context: Context): SdiaApiService {
        if (!::apiService.isInitialized) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val tokenRepository = TokenRepository.getInstance(context)
            val authInterceptor = AuthInterceptor(tokenRepository)
            val refreshAuthenticator = TokenRefreshAuthenticator(context, tokenRepository)

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(SentryNetworkInterceptor())
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .authenticator(refreshAuthenticator)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
            apiService = retrofit.create(SdiaApiService::class.java)
        }
        return apiService
    }
}
