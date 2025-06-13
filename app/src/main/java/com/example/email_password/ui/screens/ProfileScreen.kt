package com.example.email_password.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_password.model.Address
import com.example.email_password.model.Order
import com.example.email_password.model.OrderStatus
import com.example.email_password.viewmodel.ProfileViewModel
import com.example.email_password.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showOrderDetails by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.signOut(); onSignOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Profile Section
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = user?.name ?: "User",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Text(
                                            text = user?.email ?: "",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (user?.phone?.isNotEmpty() == true) {
                                            Text(
                                                text = user?.phone ?: "",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showEditProfileDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                                    }
                                }
                            }
                        }
                    }

                    // Addresses Section
                    item {
                        Text(
                            text = "Addresses",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    items(user?.addresses ?: emptyList()) { address ->
                        AddressCard(
                            address = address,
                            onEdit = { /* TODO: Implement edit address */ },
                            onDelete = { viewModel.deleteAddress(address.id) }
                        )
                    }

                    item {
                        TextButton(
                            onClick = { showAddAddressDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Address")
                        }
                    }

                    // Order History Section
                    item {
                        Text(
                            text = "Order History",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    items(user?.orders ?: emptyList()) { order ->
                        OrderCard(
                            order = order,
                            onClick = { showOrderDetails = order }
                        )
                    }
                }
            }

            // Error Dialog
            error?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var name by remember { mutableStateOf(user?.name ?: "") }
        var phone by remember { mutableStateOf(user?.phone ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProfile(name, phone)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Address Dialog
    if (showAddAddressDialog) {
        var street by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("") }
        var zipCode by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }
        var isDefault by remember { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                if (!isSubmitting) {
                    showAddAddressDialog = false
                    validationError = null
                }
            },
            title = { Text("Add New Address") },
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
                    
                    if (viewModel.isLoading.value) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
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
                        isError = validationError != null && street.isBlank(),
                        enabled = !isSubmitting
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
                        isError = validationError != null && city.isBlank(),
                        enabled = !isSubmitting
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
                        isError = validationError != null && state.isBlank(),
                        enabled = !isSubmitting
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
                        isError = validationError != null && zipCode.isBlank(),
                        enabled = !isSubmitting
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
                        isError = validationError != null && country.isBlank(),
                        enabled = !isSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it },
                            enabled = !isSubmitting
                        )
                        Text("Set as default address")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Validate fields
                        when {
                            street.isBlank() -> validationError = "Street address is required"
                            city.isBlank() -> validationError = "City is required"
                            state.isBlank() -> validationError = "State is required"
                            zipCode.isBlank() -> validationError = "ZIP code is required"
                            country.isBlank() -> validationError = "Country is required"
                            else -> {
                                isSubmitting = true
                                val address = Address(
                                    id = UUID.randomUUID().toString(),
                                    street = street.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    zipCode = zipCode.trim(),
                                    country = country.trim(),
                                    isDefault = isDefault
                                )
                                viewModel.addAddress(address)
                                // Dialog will be closed when the address is successfully added
                                // or when an error occurs (handled by the error dialog)
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (!isSubmitting) {
                            showAddAddressDialog = false
                            validationError = null
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Order Details Dialog
    showOrderDetails?.let { order ->
        OrderDetailsDialog(order, { showOrderDetails = null })
    }
}

@Composable
fun AddressCard(
    address: Address,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (address.isDefault) {
                    Text(
                        text = "Default Address",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
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
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order #${order.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = order.status.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary
                        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(order.createdAt)),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: ${PriceFormatter.formatPrice(order.totalAmount, order.currency)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Order Details Dialog
@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Details") },
        text = {
            Column {
                Text(
                    text = "Order #${order.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: ${order.status}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(order.createdAt))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Items:",
                    style = MaterialTheme.typography.titleSmall
                )
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.name)
                        Text("x${item.quantity}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Total: ${PriceFormatter.formatPrice(order.totalAmount, order.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
} 