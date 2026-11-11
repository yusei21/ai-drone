package com.sloth.registerapp.core.network

import com.sloth.registerapp.core.observability.AppObservability
import io.sentry.SentryLevel
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException

class SentryNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = System.nanoTime()
        val operation = "${request.method} ${request.url.encodedPath}"

        AppObservability.breadcrumb(
            category = "network",
            message = "API request started",
            data = mapOf(
                "method" to request.method,
                "host" to request.url.host,
                "path" to request.url.encodedPath
            )
        )

        return try {
            val response = chain.proceed(request)
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

            AppObservability.breadcrumb(
                category = "network",
                message = "API request finished",
                level = if (response.code >= 500) SentryLevel.WARNING else SentryLevel.INFO,
                data = mapOf(
                    "method" to request.method,
                    "host" to request.url.host,
                    "path" to request.url.encodedPath,
                    "status_code" to response.code,
                    "duration_ms" to elapsedMs
                )
            )

            if (response.code >= 500) {
                AppObservability.captureMessage(
                    message = "Server error from API",
                    area = "network",
                    operation = operation,
                    tags = mapOf("http.status_code" to response.code.toString()),
                    extras = mapOf(
                        "host" to request.url.host,
                        "path" to request.url.encodedPath,
                        "duration_ms" to elapsedMs
                    )
                )
            }

            response
        } catch (e: IOException) {
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
            AppObservability.captureException(
                throwable = e,
                area = "network",
                operation = operation,
                tags = mapOf("exception.kind" to "io"),
                extras = mapOf(
                    "host" to request.url.host,
                    "path" to request.url.encodedPath,
                    "duration_ms" to elapsedMs
                )
            )
            throw e
        }
    }
}

fun Throwable.httpStatusCodeOrNull(): Int? = (this as? HttpException)?.code()
