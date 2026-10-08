package com.example.meritomobiledemo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

// ============================================================================
// 1. KONTRAKT TRAS: W PEŁNI BEZPIECZNE TYPOWO (Type-Safe Routes)
// ============================================================================
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class ProductDetails(
        val productId: String,
        val productName: String,
        val price: Double
    ) : Route
}

// ============================================================================
// 2. GŁÓWNA APLIKACJA: SCAFFOLD + NAVHOST + BOTTOM BAR
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W07_NavigationDemoApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Zakładka 1: Home
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<Route.Home>() == true,
                    onClick = {
                        navController.navigate(Route.Home) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Sklep") }
                )

                // Zakładka 2: Settings
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<Route.Settings>() == true,
                    onClick = {
                        navController.navigate(Route.Settings) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ustawienia") }
                )
            }
        }
    ) { innerPadding ->
        // GŁÓWNY KONTENER NAWIGACJI
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            // EKRAN 1: Główna Lista
            composable<Route.Home> {
                HomeScreenContent(
                    onProductClick = { id, name, price ->
                        navController.navigate(Route.ProductDetails(id, name, price))
                    }
                )
            }

            // EKRAN 2: Ustawienia
            composable<Route.Settings> {
                SettingsScreenContent()
            }

            // EKRAN 3: Szczegóły Produktu (Z automatycznym odczytem argumentów)
            composable<Route.ProductDetails> { backStackEntry ->
                val detailsArgs: Route.ProductDetails = backStackEntry.toRoute()
                ProductDetailsContent(
                    args = detailsArgs,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ============================================================================
// 3. EKRANY (Czyste komponenty bezstanowe z lambdami nawigacji)
// ============================================================================

@Composable
fun HomeScreenContent(
    onProductClick: (String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Katalog Produktów (Ekran Home)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Laptop Gamingowy RTX 4080", fontWeight = FontWeight.Bold)
                Text("Cena: 7499.00 PLN", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onProductClick("lap_1", "Laptop Gamingowy RTX 4080", 7499.00) }) {
                    Text("Otwórz Szczegóły (Type-Safe)")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Słuchawki Studyjne ANC", fontWeight = FontWeight.Bold)
                Text("Cena: 899.00 PLN", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onProductClick("aud_2", "Słuchawki Studyjne ANC", 899.00) }) {
                    Text("Otwórz Szczegóły (Type-Safe)")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsContent(
    args: Route.ProductDetails,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły Produktu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Otrzymano bezpieczny argument ID: ${args.productId}",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Text("Nazwa: ${args.productName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Cena katalogowa: ${args.price} PLN", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Text(
                "Opis: Produkt został pomyślnie zdeserializowany przez bibliotekę Navigation Compose bez konieczności parsowania stringów URL!",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.weight(1f))

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Wróć do Listy (popBackStack)")
            }
        }
    }
}

@Composable
fun SettingsScreenContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Ustawienia Aplikacji", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Wersja demonstracyjna: 1.0.0 (W07 Demo)", style = MaterialTheme.typography.bodyMedium)
    }
}

// ============================================================================
// 4. PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W07 Navigation Demo")
@Composable
fun PreviewW07App() {
    MaterialTheme {
        W07_NavigationDemoApp()
    }
}