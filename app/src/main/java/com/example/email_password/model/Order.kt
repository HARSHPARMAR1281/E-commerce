package com.example.email_password.model

data class Order(
    val id: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val currency: String = "USD",
    val status: OrderStatus,
    val createdAt: Long,
    val shippingAddress: Address
)

data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val price: Double
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
} 