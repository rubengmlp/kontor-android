package com.rubengmlp.kontor.core.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BankApi {

    @GET("v1/accounts")
    suspend fun getAccounts(): List<AccountDto>

    @GET("v1/transactions")
    suspend fun getTransactions(): List<TransactionDto>

    @POST("v1/transfers")
    suspend fun postTransfer(@Body request: TransferRequestDto): TransferReceiptDto
}
