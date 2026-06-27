package com.example.ptmanageremployer.data

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit/OkHttp 클라이언트 구성과 전역 [ApiService] 인스턴스.
 * 에뮬레이터에서 호스트 PC 의 localhost 는 10.0.2.2 로 접근한다.
 */
object Network {

    const val BASE_URL = "http://10.0.2.2:8080/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = TokenStore.accessToken
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

/** 백엔드 공통 에러 응답(ApiError) 구조. */
data class ApiError(
    val status: Int? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val path: String? = null,
)

private val errorGson = Gson()

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let {
            runCatching { errorGson.fromJson(it, ApiError::class.java) }.getOrNull()
        }
        parsed?.message?.takeIf { it.isNotBlank() }
            ?: "요청을 처리하지 못했습니다. (${code()})"
    }
    is java.net.ConnectException,
    is java.net.SocketTimeoutException,
    is java.net.UnknownHostException ->
        "서버에 연결할 수 없습니다. 네트워크와 서버 상태를 확인해 주세요."
    else -> message ?: "알 수 없는 오류가 발생했습니다."
}
