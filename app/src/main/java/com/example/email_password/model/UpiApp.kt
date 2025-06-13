package com.example.email_password.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector

enum class UpiApp(
    val displayName: String,
    val icon: ImageVector,
    val packageName: String,
    val deepLinkPrefix: String
) {
    PHONEPE(
        displayName = "PhonePe",
        icon = Icons.Default.PhoneAndroid,
        packageName = "com.phonepe.app",
        deepLinkPrefix = "upi://pay"
    ),
    PAYTM(
        displayName = "Paytm",
        icon = Icons.Default.AccountBalance,
        packageName = "net.one97.paytm",
        deepLinkPrefix = "upi://pay"
    ),
    GOOGLE_PAY(
        displayName = "Google Pay",
        icon = Icons.Default.Payment,
        packageName = "com.google.android.apps.nbu.paisa.user",
        deepLinkPrefix = "upi://pay"
    );

    fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun createPaymentIntent(
        upiId: String,
        amount: Double,
        merchantName: String,
        transactionId: String,
        transactionNote: String
    ): Intent {
        val uri = Uri.parse(deepLinkPrefix).buildUpon()
            .appendQueryParameter("pa", upiId) // Payee address (UPI ID)
            .appendQueryParameter("pn", merchantName) // Payee name
            .appendQueryParameter("am", amount.toString()) // Amount
            .appendQueryParameter("tr", transactionId) // Transaction reference ID
            .appendQueryParameter("tn", transactionNote) // Transaction note
            .appendQueryParameter("cu", "INR") // Currency
            .build()

        return Intent(Intent.ACTION_VIEW).apply {
            data = uri
            setPackage(packageName)
        }
    }

    companion object {
        fun fromPackageName(packageName: String): UpiApp? {
            return values().find { it.packageName == packageName }
        }
    }
} 