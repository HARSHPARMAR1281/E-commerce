package com.example.email_password.model

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val imageUrl: String = ""
) 