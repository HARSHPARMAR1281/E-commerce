package com.example.firebaseauth.ui.screens
import androidx.compose.foundation.layout.*

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebaseauth.viewmodel.AuthViewModel

@Composable
fun DashboardScreen(
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    TextButton(
                        onClick = {
                            authViewModel.signOut()
                            onSignOut()
                        }
                    ) {
                        Text("Sign Out", color = MaterialTheme.colors.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Welcome to Your Dashboard",
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Email: ${currentUser?.email}",
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
} 