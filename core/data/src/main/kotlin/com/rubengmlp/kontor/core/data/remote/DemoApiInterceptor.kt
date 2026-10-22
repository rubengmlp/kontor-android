package com.rubengmlp.kontor.core.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * The demo backend is hermetic: Retrofit and OkHttp are real, but this interceptor serves
 * bundled JSON instead of calling a server. Replacing this one class with a real base URL
 * is the only change needed to talk to a backend.
 */
class DemoApiInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Short artificial latency so loading states are visible in the demo.
        Thread.sleep(320)

        val (code, body) = when {
            path.endsWith("/accounts") -> 200 to readAsset("demo/accounts.json")
            path.endsWith("/transactions") -> 200 to readAsset("demo/transactions.json")
            path.endsWith("/transfers") && request.method == "POST" -> 200 to transferReceipt()
            else -> 404 to """{"message":"Not found"}"""
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Not Found")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun readAsset(path: String): String = context.assets.open(path)
        .bufferedReader()
        .use { it.readText() }

    private fun transferReceipt(): String {
        val id = "trf_" + UUID.randomUUID().toString().take(8)
        return """{"id":"$id","bookedAt":"${Instant.now()}","status":"BOOKED"}"""
    }
}
