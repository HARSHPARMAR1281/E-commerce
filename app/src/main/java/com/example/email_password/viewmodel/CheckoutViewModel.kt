package com.example.email_password.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.email_password.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.*
import java.util.regex.Pattern
import java.util.Currency

class CheckoutViewModel : ViewModel() {
    private val TAG = "CheckoutViewModel"
    private val RAZORPAY_KEY_ID = "rzp_test_PBfGvJH82it8CL"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val ordersRef = database.getReference("orders")
    private val paymentsRef = database.getReference("payments")

    private val _selectedAddress = MutableStateFlow<Address?>(null)
    val selectedAddress: StateFlow<Address?> = _selectedAddress

    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethod?> = _selectedPaymentMethod

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _paymentStatus = MutableStateFlow<PaymentStatus?>(null)
    val paymentStatus: StateFlow<PaymentStatus?> = _paymentStatus

    private val _selectedCurrency = MutableStateFlow<Currency?>(null)
    val selectedCurrency: StateFlow<Currency?> = _selectedCurrency

    private val _paymentIntent = MutableStateFlow<JSONObject?>(null)
    val paymentIntent: StateFlow<JSONObject?> = _paymentIntent

    private val _paymentVerificationStatus = MutableStateFlow<PaymentVerificationStatus>(PaymentVerificationStatus.NOT_STARTED)
    val paymentVerificationStatus: StateFlow<PaymentVerificationStatus> = _paymentVerificationStatus

    // Map of country codes to currency codes
    private val countryToCurrencyMap = mapOf(
        "US" to "USD",
        "GB" to "GBP",
        "IN" to "INR",
        "EU" to "EUR",
        "JP" to "JPY",
        "AU" to "AUD",
        "CA" to "CAD",
        "CN" to "CNY",
        // Add more country-currency mappings as needed
    )

    // Map of country codes to postal code regex patterns
    private val countryPostalCodePatterns = mapOf(
        "US" to "^\\d{5}(-\\d{4})?$", // US ZIP code: 12345 or 12345-6789
        "GB" to "^[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}$", // UK postcode: AA9A 9AA or A9A 9AA
        "IN" to "^\\d{6}$", // Indian PIN code: 123456
        "CA" to "^[A-Z]\\d[A-Z] ?\\d[A-Z]\\d$", // Canadian postal code: A1A 1A1
        "AU" to "^\\d{4}$", // Australian postcode: 1234
        "JP" to "^\\d{3}-\\d{4}$", // Japanese postal code: 123-4567
        "CN" to "^\\d{6}$", // Chinese postal code: 123456
        "DE" to "^\\d{5}$", // German postal code: 12345
        "FR" to "^\\d{5}$", // French postal code: 12345
        "IT" to "^\\d{5}$", // Italian postal code: 12345
        "ES" to "^\\d{5}$", // Spanish postal code: 12345
        "BR" to "^\\d{5}-\\d{3}$", // Brazilian postal code: 12345-678
        "MX" to "^\\d{5}$", // Mexican postal code: 12345
        "RU" to "^\\d{6}$", // Russian postal code: 123456
        "ZA" to "^\\d{4}$", // South African postal code: 1234
        "SG" to "^\\d{6}$", // Singapore postal code: 123456
        "AE" to "^\\d{5}$", // UAE postal code: 12345
        "SA" to "^\\d{5}$", // Saudi Arabia postal code: 12345
        "KR" to "^\\d{5}$", // South Korean postal code: 12345
        "NZ" to "^\\d{4}$"  // New Zealand postal code: 1234
    )

    init {
        // Enable offline persistence
        ordersRef.keepSynced(true)
        paymentsRef.keepSynced(true)
    }

    fun selectAddress(address: Address) {
        _selectedAddress.value = address
        // Update currency when address is selected
        updateCurrencyForCountry(address.country)
    }
    



    private fun updateCurrencyForCountry(country: String) {
        try {
            val currencyCode = countryToCurrencyMap[country.uppercase()] ?: "USD" // Default to USD
            _selectedCurrency.value = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting currency for country: $country", e)
            _selectedCurrency.value = Currency.getInstance("USD") // Fallback to USD
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun processPayment(
        cartItems: List<CartItem>,
        cardDetails: CardDetails? = null,
        upiId: String? = null,
        upiApp: UpiApp? = null,
        activity: Activity? = null
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null
                _paymentStatus.value = null

                // Validate cart
                if (cartItems.isEmpty()) {
                    throw IllegalArgumentException("Cart is empty")
                }

                // Validate user
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                // Validate address and currency
                val address = _selectedAddress.value ?: throw IllegalArgumentException("Please select a delivery address")
                validateAddress(address)
                val currency = _selectedCurrency.value ?: throw IllegalArgumentException("Currency not set")
                
                // Validate payment method
                val paymentMethod = _selectedPaymentMethod.value ?: throw IllegalArgumentException("Please select a payment method")

                // Create order with currency information
                val order = Order(
                    id = UUID.randomUUID().toString(),
                    items = cartItems.map { OrderItem(it.id, it.name, it.quantity, it.price) },
                    totalAmount = cartItems.sumOf { it.price * it.quantity },
                    currency = currency.currencyCode,
                    status = OrderStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    shippingAddress = address
                )

                withContext(Dispatchers.IO) {
                    // Save order to database
                    ordersRef.child(userId).child(order.id).setValue(order).await()

                    // Process payment based on selected method
                    when (paymentMethod) {
                        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> {
                            if (cardDetails == null) throw IllegalArgumentException("Card details required")
                            validateCardDetails(cardDetails)
                            processRazorpayPayment(order, activity)
                        }
                        PaymentMethod.UPI -> {
                            if (upiId == null) throw IllegalArgumentException("UPI ID required")
                            if (upiApp == null) throw IllegalArgumentException("Please select a UPI app")
                            validateUpiId(upiId)
                            processRazorpayUPIPayment(order, upiId, upiApp, activity)
                        }
                        PaymentMethod.NET_BANKING -> {
                            processRazorpayPayment(order, activity)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment", e)
                handleError(e)
                _paymentStatus.value = PaymentStatus.FAILED
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun processRazorpayPayment(order: Order, activity: Activity?) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)

            val options = JSONObject().apply {
                put("name", "BUYPOINT")
                put("description", "Order #${order.id}")
                put("currency", order.currency)
                put("amount", (order.totalAmount * 100).toInt())
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email)
                    put("contact", auth.currentUser?.phoneNumber)
                })
                put("theme", JSONObject().apply {
                    put("color", "#3399cc")
                })
            }

            _paymentIntent.value = options

            activity?.let {
                checkout.open(it, options)
            } ?: throw IllegalArgumentException("Activity context required for payment")

        } catch (e: Exception) {
            Log.e(TAG, "Error in Razorpay payment", e)
            throw e
        }
    }

    private fun processRazorpayUPIPayment(order: Order, upiId: String, upiApp: UpiApp, activity: Activity?) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)

            val options = JSONObject().apply {
                put("name", "BUYPOINT")
                put("description", "Order #${order.id}")
                put("currency", order.currency)
                put("amount", (order.totalAmount * 100).toInt()) // Convert to paise
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email)
                    put("contact", auth.currentUser?.phoneNumber)
                    put("method", "upi")
                    put("vpa", upiId)
                })
                put("theme", JSONObject().apply {
                    put("color", "#3399cc")
                })
                // Add UPI app preference
                put("preferred_app", when (upiApp) {
                    UpiApp.PHONEPE -> "phonepe"
                    UpiApp.PAYTM -> "paytm"
                    UpiApp.GOOGLE_PAY -> "google_pay"
                    else -> "upi" // Fallback to generic UPI
                })
            }

            _paymentIntent.value = options

            activity?.let {
                checkout.open(it, options)
            } ?: throw IllegalArgumentException("Activity context required for payment")

        } catch (e: Exception) {
            Log.e(TAG, "Error in Razorpay UPI payment", e)
            throw e
        }
    }

    fun handlePaymentSuccess(paymentId: String, orderId: String, signature: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                // Create payment record
                val payment = Payment(
                    id = paymentId,
                    amount = _paymentIntent.value?.getDouble("amount")?.div(100) ?: 0.0,
                    currency = _paymentIntent.value?.getString("currency") ?: "INR",
                    paymentMethod = _selectedPaymentMethod.value ?: PaymentMethod.UPI,
                    orderId = orderId,
                    status = PaymentStatus.SUCCESS,
                    createdAt = System.currentTimeMillis()
                )

                // Save payment to database
                paymentsRef.child(userId).child(payment.id).setValue(payment).await()

                // Update order status
                ordersRef.child(userId).child(orderId)
                    .child("status")
                    .setValue(OrderStatus.CONFIRMED)
                    .await()

                _paymentStatus.value = PaymentStatus.SUCCESS
            } catch (e: Exception) {
                Log.e(TAG, "Error handling payment success", e)
                handleError(e)
                _paymentStatus.value = PaymentStatus.FAILED
            }
        }
    }

    fun handlePaymentError(code: Int, message: String) {
        Log.e(TAG, "Payment failed: $code - $message")
        _error.value = "Payment failed: $message"
        _paymentStatus.value = PaymentStatus.FAILED
    }

    private fun validateAddress(address: Address) {
        if (address.street.isBlank()) throw IllegalArgumentException("Street address is required")
        if (address.city.isBlank()) throw IllegalArgumentException("City is required")
        if (address.state.isBlank()) throw IllegalArgumentException("State is required")
        if (address.zipCode.isBlank()) throw IllegalArgumentException("ZIP code is required")
        if (address.country.isBlank()) throw IllegalArgumentException("Country is required")
        
        // Get the postal code pattern for the country
        val countryCode = address.country.uppercase()
        val postalCodePattern = countryPostalCodePatterns[countryCode]
        
        if (postalCodePattern != null) {
            // Validate postal code format for specific country
            if (!address.zipCode.matches(Regex(postalCodePattern))) {
                throw IllegalArgumentException("Invalid postal code format for ${address.country}")
            }
        } else {
            // For countries not in our list, use a more lenient validation
            // Allow alphanumeric characters, spaces, and common separators
            if (!address.zipCode.matches(Regex("^[A-Z0-9\\s-]{3,10}$"))) {
                throw IllegalArgumentException("Invalid postal code format")
            }
        }
    }

    private fun validateCardDetails(cardDetails: CardDetails) {
        // Validate card number (Luhn algorithm)
        if (!isValidCardNumber(cardDetails.cardNumber)) {
            throw IllegalArgumentException("Invalid card number")
        }

        // Validate CVV
        if (!cardDetails.cvv.matches(Regex("^\\d{3,4}$"))) {
            throw IllegalArgumentException("Invalid CVV")
        }

        // Validate expiry date
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        if (cardDetails.expiryYear < currentYear || 
            (cardDetails.expiryYear == currentYear && cardDetails.expiryMonth < currentMonth)) {
            throw IllegalArgumentException("Card has expired")
        }

        if (cardDetails.expiryMonth !in 1..12) {
            throw IllegalArgumentException("Invalid expiry month")
        }
    }

    private fun validateUpiId(upiId: String) {
        val upiPattern = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z]{3,}$")
        if (!upiPattern.matcher(upiId).matches()) {
            throw IllegalArgumentException("Invalid UPI ID format")
        }
    }

    private fun isValidCardNumber(cardNumber: String): Boolean {
        if (!cardNumber.matches(Regex("^\\d{16}$"))) return false
        
        var sum = 0
        var alternate = false
        
        // Loop through values starting from the rightmost digit
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber[i].toString().toInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }

    private fun handleError(e: Exception) {
        val errorMessage = when (e) {
            is UnknownHostException -> "No internet connection. Please try again."
            is IllegalArgumentException -> e.message ?: "Invalid input"
            is kotlinx.coroutines.TimeoutCancellationException -> "Payment processing timed out. Please try again."
            else -> "An error occurred during payment processing: ${e.message}"
        }
        _error.value = errorMessage
    }

    fun clearError() {
        _error.value = null
        _paymentStatus.value = null
    }

    fun initiateUpiPayment(
        cartItems: List<CartItem>,
        upiId: String,
        upiApp: UpiApp,
        activity: Activity,
        activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null
                _paymentStatus.value = null
                _paymentVerificationStatus.value = PaymentVerificationStatus.PENDING

                // Validate cart
                if (cartItems.isEmpty()) {
                    throw IllegalArgumentException("Cart is empty")
                }

                // Validate user
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                // Validate address and currency
                val address = _selectedAddress.value ?: throw IllegalArgumentException("Please select a delivery address")
                validateAddress(address)
                val currency = _selectedCurrency.value ?: throw IllegalArgumentException("Currency not set")

                // Create order
                val order = Order(
                    id = UUID.randomUUID().toString(),
                    items = cartItems.map { OrderItem(it.id, it.name, it.quantity, it.price) },
                    totalAmount = cartItems.sumOf { it.price * it.quantity },
                    currency = currency.currencyCode,
                    status = OrderStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    shippingAddress = address
                )

                // Save order to database
                withContext(Dispatchers.IO) {
                    ordersRef.child(userId).child(order.id).setValue(order).await()
                }

                // Check if UPI app is installed
                if (!upiApp.isAppInstalled(activity)) {
                    throw IllegalArgumentException("${upiApp.displayName} is not installed on your device")
                }

                // Create payment intent
                val paymentIntent = upiApp.createPaymentIntent(
                    upiId = upiId,
                    amount = order.totalAmount,
                    merchantName = "BUYPOINT",
                    transactionId = order.id,
                    transactionNote = "Payment for order #${order.id}"
                )

                // Launch UPI app
                activityResultLauncher.launch(paymentIntent)

            } catch (e: Exception) {
                Log.e(TAG, "Error initiating UPI payment", e)
                handleError(e)
                _paymentStatus.value = PaymentStatus.FAILED
                _paymentVerificationStatus.value = PaymentVerificationStatus.FAILED
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun handleUpiPaymentResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            try {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        // Payment initiated successfully
                        val response = data?.getStringExtra("response")
                        if (response != null) {
                            // Parse UPI response
                            val responseData = Uri.parse(response)
                            val status = responseData.getQueryParameter("Status")
                            val transactionId = responseData.getQueryParameter("txnId")
                            
                            if (status?.equals("SUCCESS", ignoreCase = true) == true && transactionId != null) {
                                // Payment successful
                                _paymentStatus.value = PaymentStatus.SUCCESS
                                _paymentVerificationStatus.value = PaymentVerificationStatus.SUCCESS
                                
                                // Update order status
                                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                                val orderId = responseData.getQueryParameter("tr") ?: throw Exception("Order ID not found")
                                
                                // Create payment record
                                val payment = Payment(
                                    id = transactionId,
                                    amount = responseData.getQueryParameter("am")?.toDoubleOrNull() ?: 0.0,
                                    currency = responseData.getQueryParameter("cu") ?: "INR",
                                    paymentMethod = PaymentMethod.UPI,
                                    orderId = orderId,
                                    status = PaymentStatus.SUCCESS,
                                    createdAt = System.currentTimeMillis()
                                )

                                // Save payment and update order
                                withContext(Dispatchers.IO) {
                                    paymentsRef.child(userId).child(payment.id).setValue(payment).await()
                                    ordersRef.child(userId).child(orderId)
                                        .child("status")
                                        .setValue(OrderStatus.CONFIRMED)
                                        .await()
                                }
                            } else {
                                // Payment failed
                                _paymentStatus.value = PaymentStatus.FAILED
                                _paymentVerificationStatus.value = PaymentVerificationStatus.FAILED
                                _error.value = "Payment failed: ${status ?: "Unknown error"}"
                            }
                        } else {
                            throw Exception("No response data received")
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        // User cancelled the payment
                        _paymentStatus.value = PaymentStatus.FAILED
                        _paymentVerificationStatus.value = PaymentVerificationStatus.CANCELLED
                        _error.value = "Payment cancelled by user"
                    }
                    else -> {
                        // Payment failed
                        _paymentStatus.value = PaymentStatus.FAILED
                        _paymentVerificationStatus.value = PaymentVerificationStatus.FAILED
                        _error.value = "Payment failed: Unknown error"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling UPI payment result", e)
                handleError(e)
                _paymentStatus.value = PaymentStatus.FAILED
                _paymentVerificationStatus.value = PaymentVerificationStatus.FAILED
            }
        }
    }
} 