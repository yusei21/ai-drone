package com.sloth.registerapp.core.network

import com.sloth.registerapp.features.auth.data.remote.dto.LoginRequestDto
import com.sloth.registerapp.core.network.dto.ApiStatusDto
import com.sloth.registerapp.core.network.dto.AuthTokensDto
import com.sloth.registerapp.core.network.dto.RefreshTokenRequestDto
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterRequestDto
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterResponseDto
import com.sloth.registerapp.features.auth.domain.model.User
import com.sloth.registerapp.features.mldetection.data.remote.dto.ModelCatalogEntryDto
import com.sloth.registerapp.features.mldetection.data.remote.dto.ModelDownloadUrlDto
import com.sloth.registerapp.features.mission.data.remote.dto.MissionCreateRequestDto
import com.sloth.registerapp.features.mission.data.remote.dto.MissionCreateResponseDto
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SdiaApiService {
    @GET(".")
    suspend fun getApiStatus(): ApiStatusDto

    @POST("auth/login")
    suspend fun login(@Header("Authorization") authHeader: String): AuthTokensDto

    @POST("auth/login")
    suspend fun loginWithBody(@Body request: LoginRequestDto): AuthTokensDto

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Header("Authorization") authHeader: String): AuthTokensDto

    @POST("auth/refresh")
    suspend fun refreshWithBody(@Body request: RefreshTokenRequestDto): AuthTokensDto

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): User

    @GET("missions")
    suspend fun getMissions(): List<ServerMissionDto>

    @GET("mission")
    suspend fun getMissionById(@Query("id") id: Int): ServerMissionDto

    @POST("mission")
    suspend fun createMission(@Body mission: MissionCreateRequestDto): MissionCreateResponseDto

    @DELETE("mission/{id}")
    suspend fun deleteMission(@Path("id") id: Int): Response<Unit>

    @DELETE("missions/{id}")
    suspend fun deleteMissionPlural(@Path("id") id: Int): Response<Unit>

    @GET("ml/models")
    suspend fun getMlModelCatalog(): List<ModelCatalogEntryDto>

    @GET("ml/models/{id}/download")
    suspend fun getMlModelDownloadUrl(@Path("id") id: String): ModelDownloadUrlDto
}
