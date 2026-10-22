package com.rubengmlp.kontor

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object ActivityRoute

@Serializable
data class TransactionDetail(val id: String)

@Serializable
data object TransferDestination

@Serializable
data object ProfileDestination

@Serializable
data object AboutDestination
