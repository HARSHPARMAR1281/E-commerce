package com.example.email_password.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.email_password.model.CartItem
import com.example.email_password.viewmodel.CartViewModel
import com.example.email_password.utils.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onBackClick: () -> Unit,
    onItemQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onCheckout: () -> Unit
) {
    val totalAmount = cartItems.sumOf { it.price * it.quantity }
    var showCheckoutConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping Cart") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = PriceFormatter.formatPrice(totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCheckoutConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buy Now")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your cart is empty",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(cartItems) { item ->
                    CartItemCard(
                        item = item,
                        onQuantityChange = { onItemQuantityChange(item.id, it) },
                        onRemove = { onRemoveItem(item.id) }
                    )
                }
            }
        }
    }

    if (showCheckoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showCheckoutConfirmation = false },
            title = { Text("Confirm Checkout") },
            text = { Text("Are you sure you want to proceed with the checkout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCheckoutConfirmation = false
                        onCheckout()
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCheckoutConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
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
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PriceFormatter.formatPrice(item.price),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
} 