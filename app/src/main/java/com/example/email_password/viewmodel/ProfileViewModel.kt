package com.example.email_password.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.email_password.model.Address
import com.example.email_password.model.Order
import com.example.email_password.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersRef = database.getReference("users")

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val email = auth.currentUser?.email ?: throw Exception("User email not found")

                // First, check if user exists in database
                val userSnapshot = usersRef.child(userId).get().await()
                if (!userSnapshot.exists()) {
                    // Create new user if doesn't exist
                    val newUser = User(
                        id = userId,
                        email = email
                    )
                    usersRef.child(userId).setValue(newUser).await()
                    _user.value = newUser
                } else {
                    // Load existing user
                    _user.value = userSnapshot.getValue(User::class.java)
                }

                // Set up real-time listener for user updates
                usersRef.child(userId).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _user.value = snapshot.getValue(User::class.java)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        _error.value = "Failed to load user data: ${error.message}"
                    }
                })
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(name: String, phone: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                val currentUser = _user.value ?: User(id = userId)
                val updatedUser = currentUser.copy(
                    name = name,
                    phone = phone
                )
                usersRef.child(userId).setValue(updatedUser).await()
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            }
        }
    }

    fun addAddress(address: Address) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Validate user is logged in
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                Log.d("ProfileViewModel", "Adding address for user: $userId")
                
                // Validate address fields
                if (address.street.isBlank()) throw Exception("Street address is required")
                if (address.city.isBlank()) throw Exception("City is required")
                if (address.state.isBlank()) throw Exception("State is required")
                if (address.zipCode.isBlank()) throw Exception("ZIP code is required")
                if (address.country.isBlank()) throw Exception("Country is required")
                
                // Load current user data if not already loaded
                if (_user.value == null) {
                    Log.d("ProfileViewModel", "Loading user data before adding address")
                    val userSnapshot = usersRef.child(userId).get().await()
                    val currentUser = userSnapshot.getValue(User::class.java) ?: User(id = userId)
                    _user.value = currentUser
                }
                
                val currentUser = _user.value ?: throw Exception("Failed to load user data")
                Log.d("ProfileViewModel", "Current user loaded: ${currentUser.id}")
                
                // Handle default address logic
                val updatedAddresses = if (address.isDefault) {
                    Log.d("ProfileViewModel", "Setting as default address")
                    currentUser.addresses.map { it.copy(isDefault = false) } + address
                } else {
                    currentUser.addresses + address
                }
                
                // Update user in database
                val updatedUser = currentUser.copy(addresses = updatedAddresses)
                Log.d("ProfileViewModel", "Updating user with new address")
                usersRef.child(userId).setValue(updatedUser).await()
                
                // Update local state
                _user.value = updatedUser
                Log.d("ProfileViewModel", "Address added successfully")
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error adding address", e)
                _error.value = "Failed to add address: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAddress(address: Address) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val currentUser = _user.value ?: return@launch
                val updatedAddresses = if (address.isDefault) {
                    currentUser.addresses.map {
                        if (it.id == address.id) address
                        else it.copy(isDefault = false)
                    }
                } else {
                    currentUser.addresses.map {
                        if (it.id == address.id) address else it
                    }
                }
                val updatedUser = currentUser.copy(addresses = updatedAddresses)
                usersRef.child(userId).setValue(updatedUser).await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                
                val currentUser = _user.value ?: throw Exception("User data not loaded")
                val updatedAddresses = currentUser.addresses.filter { it.id != addressId }
                val updatedUser = currentUser.copy(addresses = updatedAddresses)
                usersRef.child(userId).setValue(updatedUser).await()
            } catch (e: Exception) {
                _error.value = "Failed to delete address: ${e.message}"
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
    }

    fun clearError() {
        _error.value = null
    }
} 