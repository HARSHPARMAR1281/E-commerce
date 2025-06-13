package com.example.email_password.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val addresses: List<Address> = emptyList(),
    val orders: List<Order> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) 