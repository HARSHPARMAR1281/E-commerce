package com.example.email_password.model

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val stock: Int = 0,
    val rating: Float = 0f,
    val reviews: List<Review> = emptyList()
)

data class Review(
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = 0
) 