package com.example.email_password.model

data class Cart(
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
) 