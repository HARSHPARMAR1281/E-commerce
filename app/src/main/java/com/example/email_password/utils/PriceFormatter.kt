package com.example.email_password.utils

import java.text.NumberFormat
import java.util.*

object PriceFormatter {
    fun formatPrice(amount: Double, currencyCode: String = "USD"): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currencyCode)
        return format.format(amount)
    }
} 