package com.example.email_password.model

data class Payment(
    val id: String,
    val amount: Double,
    val currency: String = "USD",
    val paymentMethod: PaymentMethod,
    val orderId: String,
    val status: PaymentStatus,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}

enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    NET_BANKING
}

data class CardDetails(
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardHolderName: String
) 