package com.example.email_password.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_password.model.*
import com.example.email_password.viewmodel.CheckoutViewModel
import com.example.email_password.viewmodel.ProfileViewModel
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.util.*
import java.text.NumberFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import com.example.email_password.utils.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    onBackClick: () -> Unit,
    onOrderPlaced: () -> Unit,
    checkoutViewModel: CheckoutViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val user by profileViewModel.user.collectAsState()
    val selectedAddress by checkoutViewModel.selectedAddress.collectAsState()
    val selectedPaymentMethod by checkoutViewModel.selectedPaymentMethod.collectAsState()
    val selectedCurrency by checkoutViewModel.selectedCurrency.collectAsState()
    val isProcessing by checkoutViewModel.isProcessing.collectAsState()
    val error by checkoutViewModel.error.collectAsState()
    val paymentStatus by checkoutViewModel.paymentStatus.collectAsState()
    val profileError by profileViewModel.error.collectAsState()
    val scope = rememberCoroutineScope()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCardDetailsDialog by remember { mutableStateOf(false) }
    var showUpiDialog by remember { mutableStateOf(false) }
    var showOrderConfirmation by remember { mutableStateOf(false) }
    var showOrderSuccess by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showEditAddressDialog by remember { mutableStateOf<Address?>(null) }

    val totalAmount = cartItems.sumOf { it.price * it.quantity }

    // Add activity result launcher for UPI payment
    val upiPaymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkoutViewModel.handleUpiPaymentResult(result.resultCode, result.data)
    }

    // Handle payment verification status
    val paymentVerificationStatus by checkoutViewModel.paymentVerificationStatus.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    // Handle payment status changes
    LaunchedEffect(paymentStatus) {
        when (paymentStatus) {
            PaymentStatus.SUCCESS -> {
                showOrderSuccess = true
            }
            PaymentStatus.FAILED -> {
                // Error is already set in the ViewModel
            }
            else -> {}
        }
    }

    LaunchedEffect(paymentVerificationStatus) {
        when (paymentVerificationStatus) {
            PaymentVerificationStatus.SUCCESS -> {
                showOrderSuccess = true
            }
            PaymentVerificationStatus.FAILED -> {
                // Error is already set in the ViewModel
            }
            PaymentVerificationStatus.CANCELLED -> {
                // User cancelled the payment
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Order Summary
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Order Summary",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            cartItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${item.name} x${item.quantity}")
                                    Text(PriceFormatter.formatPrice(item.price * item.quantity, selectedCurrency?.currencyCode ?: "USD"))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    PriceFormatter.formatPrice(totalAmount, selectedCurrency?.currencyCode ?: "USD"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Delivery Address Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delivery Address",
                            style = MaterialTheme.typography.titleLarge
                        )
                        TextButton(
                            onClick = { showAddAddressDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add New")
                        }
                    }
                }

                items(user?.addresses ?: emptyList()) { address ->
                    AddressSelectionCard(
                        address = address,
                        isSelected = address == selectedAddress,
                        onSelect = { checkoutViewModel.selectAddress(address) },
                        onEdit = { showEditAddressDialog = address }
                    )
                }

                // Payment Method
                item {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    PaymentMethodSelection(
                        selectedMethod = selectedPaymentMethod,
                        onMethodSelected = { method ->
                            checkoutViewModel.selectPaymentMethod(method)
                            when (method) {
                                PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> showCardDetailsDialog = true
                                PaymentMethod.UPI -> showUpiDialog = true
                                PaymentMethod.NET_BANKING -> showPaymentDialog = true
                            }
                        }
                    )
                }

                // Place Order Button
                item {
                    Button(
                        onClick = { showOrderConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = selectedAddress != null && selectedPaymentMethod != null && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Place Order")
                        }
                    }
                }
            }

            // Add Address Dialog
            if (showAddAddressDialog) {
                AddressDialog(
                    onDismiss = { showAddAddressDialog = false },
                    onSave = { address ->
                        profileViewModel.addAddress(address)
                        showAddAddressDialog = false
                    }
                )
            }

            // Edit Address Dialog
            showEditAddressDialog?.let { address ->
                AddressDialog(
                    address = address,
                    onDismiss = { showEditAddressDialog = null },
                    onSave = { updatedAddress ->
                        profileViewModel.updateAddress(updatedAddress)
                        showEditAddressDialog = null
                    }
                )
            }

            // Card Details Dialog
            if (showCardDetailsDialog) {
                var cardNumber by remember { mutableStateOf("") }
                var expiryMonth by remember { mutableStateOf("") }
                var expiryYear by remember { mutableStateOf("") }
                var cvv by remember { mutableStateOf("") }
                var cardHolderName by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showCardDetailsDialog = false },
                    title = { Text("Card Details") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Card Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                OutlinedTextField(
                                    value = expiryMonth,
                                    onValueChange = { expiryMonth = it },
                                    label = { Text("MM") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = expiryYear,
                                    onValueChange = { expiryYear = it },
                                    label = { Text("YY") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it },
                                label = { Text("CVV") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = cardHolderName,
                                onValueChange = { cardHolderName = it },
                                label = { Text("Card Holder Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        val cardDetails = CardDetails(
                            cardNumber = cardNumber,
                            expiryMonth = expiryMonth.toIntOrNull() ?: 0,
                            expiryYear = expiryYear.toIntOrNull() ?: 0,
                            cvv = cvv,
                            cardHolderName = cardHolderName
                        )
                        checkoutViewModel.processPayment(cartItems, cardDetails = cardDetails)
                        showCardDetailsDialog = false
                    },
                    dismissButton = {
                        TextButton(onClick = { showCardDetailsDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // UPI Dialog
            if (showUpiDialog) {
                var upiId by remember { mutableStateOf("") }
                var selectedUpiApp by remember { mutableStateOf<UpiApp?>(null) }
                val upiPaymentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    checkoutViewModel.handleUpiPaymentResult(result.resultCode, result.data)
                }
                val paymentVerificationStatus by checkoutViewModel.paymentVerificationStatus.collectAsState()

                LaunchedEffect(paymentVerificationStatus) {
                    when (paymentVerificationStatus) {
                        PaymentVerificationStatus.SUCCESS -> {
                            showOrderSuccess = true
                            showUpiDialog = false
                        }
                        PaymentVerificationStatus.FAILED, 
                        PaymentVerificationStatus.CANCELLED -> {
                            // Error is already set in the ViewModel
                        }
                        else -> {}
                    }
                }

                AlertDialog(
                    onDismissRequest = { 
                        if (paymentVerificationStatus != PaymentVerificationStatus.PENDING) {
                            showUpiDialog = false 
                        }
                    },
                    title = { Text("UPI Payment") },
                    text = {
                        Column {
                            Text(
                                text = "Select UPI App",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // UPI App Selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                UpiApp.values().forEach { app ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .padding(4.dp),
                                            onClick = { 
                                                if (paymentVerificationStatus != PaymentVerificationStatus.PENDING) {
                                                    selectedUpiApp = app 
                                                }
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = app.icon,
                                                    contentDescription = app.displayName,
                                                    modifier = Modifier.size(32.dp),
                                                    tint = if (selectedUpiApp == app) 
                                                        MaterialTheme.colorScheme.primary 
                                                    else 
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Text(
                                            text = app.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { 
                                    if (paymentVerificationStatus != PaymentVerificationStatus.PENDING) {
                                        upiId = it 
                                    }
                                },
                                label = { Text("UPI ID") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedUpiApp != null && 
                                         paymentVerificationStatus != PaymentVerificationStatus.PENDING
                            )
                            
                            if (selectedUpiApp != null) {
                                Text(
                                    text = "Enter your ${selectedUpiApp?.displayName} UPI ID",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (error != null) {
                                Text(
                                    text = error ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            if (paymentVerificationStatus == PaymentVerificationStatus.PENDING) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                                Text(
                                    text = "Processing payment...",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (activity != null && selectedUpiApp != null) {
                                    checkoutViewModel.initiateUpiPayment(
                                        cartItems = cartItems,
                                        upiId = upiId,
                                        upiApp = selectedUpiApp!!,
                                        activity = activity,
                                        activityResultLauncher = upiPaymentLauncher
                                    )
                                }
                            },
                            enabled = upiId.isNotBlank() && 
                                     selectedUpiApp != null && 
                                     !isProcessing && 
                                     paymentVerificationStatus != PaymentVerificationStatus.PENDING
                        ) {
                            if (isProcessing || paymentVerificationStatus == PaymentVerificationStatus.PENDING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Pay with ${selectedUpiApp?.displayName ?: "UPI"}")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                if (paymentVerificationStatus != PaymentVerificationStatus.PENDING) {
                                    showUpiDialog = false 
                                }
                            },
                            enabled = paymentVerificationStatus != PaymentVerificationStatus.PENDING
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Order Confirmation Dialog
            if (showOrderConfirmation) {
                AlertDialog(
                    onDismissRequest = { showOrderConfirmation = false },
                    title = { Text("Confirm Order") },
                    text = { Text("Are you sure you want to place this order?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showOrderConfirmation = false
                                checkoutViewModel.processPayment(cartItems)
                                showOrderSuccess = true
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOrderConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Order Success Dialog
            if (showOrderSuccess) {
                AlertDialog(
                    onDismissRequest = {
                        showOrderSuccess = false
                        onOrderPlaced()
                    },
                    title = { Text("Order Placed Successfully") },
                    text = { Text("Your order has been placed successfully. Thank you for shopping with us!") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showOrderSuccess = false
                                onOrderPlaced()
                            }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }

            // Error Dialogs
            error?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { checkoutViewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { checkoutViewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }

            profileError?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { profileViewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { profileViewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddressDialog(
    address: Address? = null,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var street by remember { mutableStateOf(address?.street ?: "") }
    var city by remember { mutableStateOf(address?.city ?: "") }
    var state by remember { mutableStateOf(address?.state ?: "") }
    var zipCode by remember { mutableStateOf(address?.zipCode ?: "") }
    var country by remember { mutableStateOf(address?.country ?: "") }
    var isDefault by remember { mutableStateOf(address?.isDefault ?: false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { 
            onDismiss()
            validationError = null
        },
        title = { Text(if (address == null) "Add New Address" else "Edit Address") },
        text = {
            Column {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = street,
                    onValueChange = { 
                        street = it
                        validationError = null
                    },
                    label = { Text("Street") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null && street.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { 
                        city = it
                        validationError = null
                    },
                    label = { Text("City") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null && city.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state,
                    onValueChange = { 
                        state = it
                        validationError = null
                    },
                    label = { Text("State") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null && state.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = zipCode,
                    onValueChange = { 
                        zipCode = it
                        validationError = null
                    },
                    label = { Text("ZIP Code") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null && zipCode.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = { 
                        country = it
                        validationError = null
                    },
                    label = { Text("Country") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null && country.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("Set as default address")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        street.isBlank() -> validationError = "Street address is required"
                        city.isBlank() -> validationError = "City is required"
                        state.isBlank() -> validationError = "State is required"
                        zipCode.isBlank() -> validationError = "ZIP code is required"
                        country.isBlank() -> validationError = "Country is required"
                        else -> {
                            val newAddress = Address(
                                id = address?.id ?: UUID.randomUUID().toString(),
                                street = street.trim(),
                                city = city.trim(),
                                state = state.trim(),
                                zipCode = zipCode.trim(),
                                country = country.trim(),
                                isDefault = isDefault
                            )
                            onSave(newAddress)
                        }
                    }
                }
            ) {
                Text(if (address == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { 
                    onDismiss()
                    validationError = null
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddressSelectionCard(
    address: Address,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (address.isDefault) {
                    Text(
                        text = "Default Address",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = address.street,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${address.city}, ${address.state} ${address.zipCode}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = address.country,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
            }
        }
    }
}

@Composable
fun PaymentMethodSelection(
    selectedMethod: PaymentMethod?,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        PaymentMethod.values().forEach { method ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = method == selectedMethod,
                        onClick = { onMethodSelected(method) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (method) {
                            PaymentMethod.CREDIT_CARD -> "Credit Card"
                            PaymentMethod.DEBIT_CARD -> "Debit Card"
                            PaymentMethod.UPI -> "UPI"
                            PaymentMethod.NET_BANKING -> "Net Banking"
                        }
                    )
                }
                Icon(
                    imageVector = when (method) {
                        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> Icons.Default.CreditCard
                        PaymentMethod.UPI -> Icons.Default.PhoneAndroid
                        PaymentMethod.NET_BANKING -> Icons.Default.AccountBalance
                    },
                    contentDescription = null
                )
            }
        }
    }
}