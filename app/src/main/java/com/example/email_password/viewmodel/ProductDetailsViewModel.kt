package com.example.email_password.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.email_password.model.CartItem
import com.example.email_password.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductDetailsViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cartRef = database.getReference("carts")
    
    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            try {
                val snapshot = database.getReference("products")
                    .child(productId)
                    .get()
                    .await()
                
                _product.value = snapshot.getValue(Product::class.java)
            } catch (e: Exception) {
                _error.value = "Failed to load product: ${e.message}"
            }
        }
    }

    fun addToCart(productId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val product = _product.value ?: throw Exception("Product not loaded")
                
                // Create cart item
                val cartItem = CartItem(
                    id = UUID.randomUUID().toString(),
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = quantity,
                    imageUrl = product.imageUrl
                )

                // Save to Firebase
                cartRef.child(userId).child(cartItem.id).setValue(cartItem).await()
            } catch (e: Exception) {
                _error.value = "Failed to add to cart: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 