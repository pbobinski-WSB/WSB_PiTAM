package pl.merito.mobile.w11

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ============================================================================
// MODUŁ 1: :core:designsystem (Współdzielone atomy wizualne - Slajd 11.16)
// ============================================================================
object CoreDesignSystem {
    val ModuleTagColor = Color(0xFF6750A4)
    val SuccessColor = Color(0xFF2E7D32)

    @Composable
    fun ModuleBadge(moduleName: String) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "📦 Moduł: $moduleName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// MODUŁ 2: :feature:feed (Tablica Produktów - Slajd 11.13, 11.20)
// ============================================================================
data class FeedItem(val id: String, val title: String, val price: Double)

@Composable
fun FeedModuleScreen(
    onNavigateToCart: (String) -> Unit, // Zdarzenie nawigacji wyniesione w górę (brak zależności do koszyka!)
    modifier: Modifier = Modifier
) {
    val sampleFeed = remember {
        listOf(
            FeedItem("1", "Konsola do Gier 4K", 2399.00),
            FeedItem("2", "Gamepad Bezprzewodowy Pro", 289.00),
            FeedItem("3", "Fotel Ergonomiczny", 899.00)
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        CoreDesignSystem.ModuleBadge(":feature:feed:impl")
        Text("Katalog Produktów", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sampleFeed) { item ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text("${item.price} PLN", color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { onNavigateToCart(item.title) }) {
                            Icon(Icons.Default.ShoppingCart, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Dodaj")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODUŁ 3: :feature:cart (Koszyk Zakupowy - Slajd 11.24 - 11.26)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartModuleScreen(
    addedItemName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mój Koszyk") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoreDesignSystem.ModuleBadge(":feature:cart:impl")

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pomyślnie przekazano dane między modułami!", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Wybrana pozycja: $addedItemName", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(
                "Ten moduł nie ma pojęcia o istnieniu modułu :feature:feed. Komunikacja odbyła się przez mediatora w module :app!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.weight(1f))

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Wróć do Przeglądania")
            }
        }
    }
}

// ============================================================================
// MODUŁ 4: :app (Główny Mediator i Orkiestrator - Slajd 11.18, 11.23)
// ============================================================================
enum class ActiveModuleScreen { FEED, CART }

@Composable
fun W11_ModularAppDemo() {
    // Stan nawigacji w module :app
    var currentScreen by remember { mutableStateOf(ActiveModuleScreen.FEED) }
    var selectedItemForCart by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            ActiveModuleScreen.FEED -> {
                FeedModuleScreen(
                    onNavigateToCart = { itemName ->
                        selectedItemForCart = itemName
                        currentScreen = ActiveModuleScreen.CART // Mediator przełącza moduły
                    }
                )
            }
            ActiveModuleScreen.CART -> {
                CartModuleScreen(
                    addedItemName = selectedItemForCart,
                    onBack = { currentScreen = ActiveModuleScreen.FEED }
                )
            }
        }
    }
}

// ============================================================================
// 5. PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W11 Modular Architecture Demo")
@Composable
fun PreviewW11ModularApp() {
    MaterialTheme {
        W11_ModularAppDemo()
    }
}