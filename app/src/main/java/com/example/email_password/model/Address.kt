package com.example.email_password.model

data class Address(
    val id: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
    val isDefault: Boolean = false
) 