package com.rubengmlp.kontor.core.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * In a real build the token would come from a Keystore-backed session store and would be
 * refreshed by an OkHttp Authenticator after a 401.
 */
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer demo-session-token")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
