package com.example.meritomobiledemo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ============================================================================
// 1. MODELE DANYCH DLA WIDOKU
// ============================================================================
data class ProductItem(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val rating: Double
)

val sampleProducts = listOf(
    ProductItem(1, "Słuchawki Bezprzewodowe Pro", "Audio", 399.99, 4.8),
    ProductItem(2, "Smartwatch Active 2", "Wearables", 649.00, 4.5),
    ProductItem(3, "Klawiatura Mechaniczna RGB", "Akcesoria", 299.50, 4.9),
    ProductItem(4, "Mysz Ergonomiczna BT", "Akcesoria", 159.00, 4.2),
    ProductItem(5, "Monitor 4K 27 cali", "Monitory", 1499.00, 4.7)
)

// ============================================================================
// 2. GŁÓWNY EKRAN WYKŁADOWY (Scaffold + Slot API + Insety)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W03_LayoutsDemoScreen(
    products: List<ProductItem> = sampleProducts,
    modifier: Modifier = Modifier // ZŁOTA ZASADA: publiczny Composable przyjmuje domyślny Modifier
) {
    // Scaffold implementuje bazową strukturę Material 3
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sklep Mobile - Demo W03", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* akcja */ }) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Koszyk")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* akcja dodawania */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
            }
        }
    ) { innerPadding ->
        // innerPadding ze Scaffolda zapobiega nachodzeniu treści na pasek górny/dolny (Edge-to-Edge)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Zastosowanie bezpiecznych marginesów
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // GAP między elementami listy
        ) {
            // SEKCJA 1: Pojedynczy element (Nagłówek i Baner Promocyjny - Demonstracja Box i Modifier)
            item {
                Text(
                    text = "Promocja Tygodnia (Demo: Box, Gradient, Clip)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                PromoBanner()
            }

            // SEKCJA 2: Demonstracja Row i Wag (Modifier.weight)
            item {
                Text(
                    text = "Kategorie (Demo: Row & Modifier.weight)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                CategoryRowSection()
            }

            // SEKCJA 3: Lista powtarzalna z unikalnymi kluczami (items + key)
            item {
                Text(
                    text = "Katalog Produktów (Demo: LazyColumn items z key)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // items z parametrem 'key' gwarantuje wydajność i poprawne animacje przy rekompozycji
            items(
                items = products,
                key = { product -> product.id } // Klucz biznesowy unikalny dla rekordu
            ) { product ->
                ProductCard(product = product)
            }
        }
    }
}

// ============================================================================
// 3. KOMPONENT BANERA: Prezentacja Box, Alignment i Kolejności Modyfikatorów
// ============================================================================
@Composable
fun PromoBanner(modifier: Modifier = Modifier) {
    // Box układa elementy na osi Z (jeden na drugim - jak position: absolute w CSS)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(0.dp))
            .clip(RoundedCornerShape(0.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5))
                )
            )
            .clickable { println("Kliknięto w baner promocyjny!") }
            .padding(16.dp) // Ten padding jest wewnątrz gradientu i zaokrąglenia!
    ) {
        // Tekst wyrównany do lewego-górnego rogu
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = "Zniżka -20% na hasło: MERITO",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Ważne tylko do końca semestru!",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Ikona odznaki wyrównana do prawego-dolnego rogu
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.25f)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.Yellow,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

// ============================================================================
// 4. KOMPONENT KATEGORII: Prezentacja Row i Wagi (weight)
// ============================================================================
@Composable
fun CategoryRowSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dwa przyciski dzielące szerokość ekranu w proporcji 50/50 (odpowiednik flex: 1)
        OutlinedButton(
            onClick = { },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Elektronika")
        }

        Button(
            onClick = { },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Bestsellery")
        }
    }
}

// ============================================================================
// 5. KARTA PRODUKTU: Prezentacja Card, Surface, Typography w M3
// ============================================================================
@Composable
fun ProductCard(
    product: ProductItem,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lewa kolumna: Informacje o produkcie
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Kategoria: ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${product.rating}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Prawa kolumna: Cena i akcja
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${product.price} PLN",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = { println("Kupiono: ${product.name}") },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Kup", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ============================================================================
// 6. PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W03 Demo - Tryb Jasny")
@Composable
fun PreviewW03Screen() {
    MaterialTheme {
        W03_LayoutsDemoScreen()
    }
}