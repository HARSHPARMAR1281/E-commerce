package com.example.email_password.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.email_password.model.CartItem
import com.example.email_password.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.UnknownHostException

class CartViewModel : ViewModel() {
    private val TAG = "CartViewModel"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cartRef = database.getReference("carts")
    private val mutex = Mutex() // For preventing race conditions

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    init {
        // Enable offline persistence
        cartRef.keepSynced(true)
        loadCartItems()
    }

    private fun loadCartItems() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting to load cart items")
                _isLoading.value = true
                _error.value = null
                
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "User not logged in during cart load")
                    throw Exception("User not logged in")
                }

                withContext(Dispatchers.IO) {
                    Log.d(TAG, "Loading cart for user: $userId")
                    val cartSnapshot = cartRef.child(userId).get().await()
                    val items = cartSnapshot.children.mapNotNull { it.getValue(CartItem::class.java) }
                    Log.d(TAG, "Loaded ${items.size} items from cart")
                    _cartItems.value = items
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cart items", e)
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateItemQuantity(itemId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                mutex.withLock {
                    _error.value = null
                    if (quantity <= 0) {
                        removeItem(itemId)
                        return@withLock
                    }

                    val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                    val currentItems = _cartItems.value.toMutableList()
                    val itemIndex = currentItems.indexOfFirst { it.id == itemId }
                    
                    if (itemIndex != -1) {
                        val updatedItem = currentItems[itemIndex].copy(quantity = quantity)
                        currentItems[itemIndex] = updatedItem
                        _cartItems.value = currentItems
                        
                        withContext(Dispatchers.IO) {
                            cartRef.child(userId).child(itemId).setValue(updatedItem).await()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating item quantity", e)
                handleError(e)
            }
        }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            try {
                mutex.withLock {
                    _error.value = null
                    val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                    
                    withContext(Dispatchers.IO) {
                        cartRef.child(userId).child(itemId).removeValue().await()
                    }
                    
                    _cartItems.value = _cartItems.value.filter { it.id != itemId }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing item", e)
                handleError(e)
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                mutex.withLock {
                    _error.value = null
                    val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                    
                    withContext(Dispatchers.IO) {
                        cartRef.child(userId).removeValue().await()
                    }
                    
                    _cartItems.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cart", e)
                handleError(e)
            }
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                mutex.withLock {
                    Log.d(TAG, "Starting addToCart for product: ${product.name}")
                    _error.value = null
                    
                    if (quantity <= 0) {
                        throw IllegalArgumentException("Quantity must be greater than 0")
                    }

                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        Log.e(TAG, "User not logged in")
                        throw Exception("User not logged in")
                    }

                    val currentItems = _cartItems.value.toMutableList()
                    val existingItemIndex = currentItems.indexOfFirst { it.productId == product.id }
                    
                    withContext(Dispatchers.IO) {
                        if (existingItemIndex != -1) {
                            Log.d(TAG, "Product exists in cart, updating quantity")
                            val existingItem = currentItems[existingItemIndex]
                            val updatedItem = existingItem.copy(
                                quantity = existingItem.quantity + quantity
                            )
                            currentItems[existingItemIndex] = updatedItem
                            
                            cartRef.child(userId).child(existingItem.id).setValue(updatedItem).await()
                        } else {
                            Log.d(TAG, "Adding new product to cart")
                            val cartItem = CartItem(
                                id = java.util.UUID.randomUUID().toString(),
                                productId = product.id,
                                name = product.name,
                                price = product.price,
                                quantity = quantity,
                                imageUrl = product.imageUrl
                            )
                            currentItems.add(cartItem)
                            
                            cartRef.child(userId).child(cartItem.id).setValue(cartItem).await()
                        }
                    }
                    
                    _cartItems.value = currentItems
                    Log.d(TAG, "Cart updated successfully. New item count: ${currentItems.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to cart", e)
                handleError(e)
            }
        }
    }

    private fun handleError(e: Exception) {
        val errorMessage = when (e) {
            is UnknownHostException -> {
                _isOffline.value = true
                "No internet connection. Changes will be synced when online."
            }
            is IllegalArgumentException -> e.message ?: "Invalid input"
            else -> "An error occurred: ${e.message}"
        }
        _error.value = errorMessage
    }

    fun clearError() {
        _error.value = null
    }

    fun retryOfflineOperations() {
        if (_isOffline.value) {
            _isOffline.value = false
            loadCartItems()
        }
    }
} 