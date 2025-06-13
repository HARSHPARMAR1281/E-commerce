package com.example.email_password.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.email_password.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val isNetworkAvailable: Boolean = true
)

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        // Check auth state
        checkAuthState()
        // Keep users reference synced
        usersRef.keepSynced(true)
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    _state.value = AuthState(isAuthenticated = true)
                    loadUserData(user.uid)
                } else {
                    _state.value = AuthState(isAuthenticated = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth state", e)
                handleError(e)
            }
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val snapshot = usersRef.child(userId).get().await()
                    _currentUser.value = snapshot.getValue(User::class.java)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
                handleError(e)
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            try {
                _state.value = AuthState(isLoading = true)
                
                // Validate input
                if (email.isBlank() || password.isBlank() || name.isBlank()) {
                    throw IllegalArgumentException("All fields are required")
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    throw IllegalArgumentException("Invalid email format")
                }
                if (password.length < 6) {
                    throw IllegalArgumentException("Password must be at least 6 characters")
                }

                withContext(Dispatchers.IO) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user ?: throw Exception("Failed to create user")
                    
                    // Create user document in database
                    val newUser = User(
                        id = user.uid,
                        email = email,
                        name = name,
                        createdAt = System.currentTimeMillis()
                    )
                    usersRef.child(user.uid).setValue(newUser).await()
                    
                    _currentUser.value = newUser
                    _state.value = AuthState(isAuthenticated = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign up", e)
                handleError(e)
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _state.value = AuthState(isLoading = true)
                
                // Validate input
                if (email.isBlank() || password.isBlank()) {
                    throw IllegalArgumentException("Email and password are required")
                }

                withContext(Dispatchers.IO) {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user ?: throw Exception("Failed to sign in")
                    loadUserData(user.uid)
                    _state.value = AuthState(isAuthenticated = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign in", e)
                handleError(e)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _state.value = AuthState(isAuthenticated = false)
                _currentUser.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out", e)
                handleError(e)
            }
        }
    }

    private fun handleError(e: Exception) {
        val errorMessage = when (e) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is IllegalArgumentException -> e.message ?: "Invalid input"
            else -> "An error occurred: ${e.message}"
        }
        _state.value = AuthState(
            error = errorMessage,
            isNetworkAvailable = e !is UnknownHostException
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
} 