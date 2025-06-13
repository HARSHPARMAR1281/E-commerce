package com.example.email_password.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.email_password.data.SampleProducts
import com.example.email_password.model.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("products")

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    init {
        loadProducts()
        initializeSampleProducts()
    }

    private fun initializeSampleProducts() {
        viewModelScope.launch {
            // Check if products already exist in Firebase
            productsRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // Add sample products to Firebase
                    SampleProducts.products.forEach { product ->
                        productsRef.child(product.id).setValue(product)
                    }
                }
            }
        }
    }

    private fun loadProducts() {
        productsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<Product>()
                snapshot.children.forEach { child ->
                    child.getValue(Product::class.java)?.let { product ->
                        productList.add(product)
                    }
                }
                _products.value = productList
                
                // Update categories based on unique product categories
                _categories.value = productList.map { it.category }.distinct()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun getProductsByCategory(category: String): List<Product> {
        return products.value.filter { it.category == category }
    }
} 