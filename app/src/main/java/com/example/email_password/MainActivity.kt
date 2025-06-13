package com.example.email_password

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_password.navigation.AppNavigation
import com.example.email_password.ui.theme.Email_passwordTheme
import com.example.email_password.viewmodel.CheckoutViewModel
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {
    private lateinit var checkoutViewModel: CheckoutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Razorpay
        com.razorpay.Checkout.preload(applicationContext)
        
        setContent {
            Email_passwordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Initialize ViewModel
                    checkoutViewModel = remember { CheckoutViewModel() }
                    
                    AppNavigation()
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        if (razorpayPaymentId != null) {
            // Get the current payment intent from ViewModel
            val paymentIntent = checkoutViewModel.paymentIntent.value
            val orderId = paymentIntent?.getString("description")?.substringAfter("#") ?: ""
            
            // Handle successful payment
            checkoutViewModel.handlePaymentSuccess(
                paymentId = razorpayPaymentId,
                orderId = orderId,
                signature = "" // Razorpay signature verification can be added here if needed
            )
            
            // Show success message
            Toast.makeText(
                this,
                "Payment successful! Order ID: $orderId",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPaymentError(code: Int, message: String?) {
        // Handle payment error
        checkoutViewModel.handlePaymentError(code, message ?: "Payment failed")
        
        // Show error message
        Toast.makeText(
            this,
            "Payment failed: ${message ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }
}