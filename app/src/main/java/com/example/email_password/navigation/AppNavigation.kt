package com.example.email_password.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.email_password.ui.screens.*
import com.example.email_password.viewmodel.CartViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object ProductDetails : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object Cart : Screen("cart")
    object Profile : Screen("profile")
    object Checkout : Screen("checkout")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetails.createRoute(productId))
                },
                onCartClick = {
                    navController.navigate(Screen.Cart.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.ProductDetails.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailsScreen(
                productId = productId,
                onBackClick = {
                    navController.popBackStack()
                },
                onAddToCart = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        }

        composable(Screen.Cart.route) {
            val cartViewModel: CartViewModel = viewModel()
            val cartItems by cartViewModel.cartItems.collectAsState()

            CartScreen(
                cartItems = cartItems,
                onBackClick = {
                    navController.popBackStack()
                },
                onItemQuantityChange = { itemId, quantity ->
                    cartViewModel.updateItemQuantity(itemId, quantity)
                },
                onRemoveItem = { itemId ->
                    cartViewModel.removeItem(itemId)
                },
                onCheckout = {
                    navController.navigate(Screen.Checkout.route)
                }
            )
        }

        composable(Screen.Checkout.route) {
            val cartViewModel: CartViewModel = viewModel()
            val cartItems by cartViewModel.cartItems.collectAsState()

            CheckoutScreen(
                cartItems = cartItems,
                onBackClick = {
                    navController.popBackStack()
                },
                onOrderPlaced = {
                    cartViewModel.clearCart()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
} 