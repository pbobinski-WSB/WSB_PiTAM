package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// 1. SYMULACJA KONTRAKTU DANYCH (Model Domenowy i BFF Payload)
// ============================================================================
data class DashboardItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val value: String
)

data class NetworkMetrics(
    val requestCount: Int,
    val payloadSizeKb: Int,
    val latencyMs: Long,
    val radioAwakeSeconds: Int
)

// ============================================================================
// 2. SYMULOWANY SERWIS SIECIOWY (BFF vs Tradycyjne Mikroserwisy)
// ============================================================================
class MockNetworkEngine {
    var simulateOfflineMode by mutableStateOf(false)

    // A. SCENARIUSZ BEZ BFF: 3 osobne zapytania do 3 mikroserwisów (Antywzorzec)
    suspend fun fetchDirectFromMicroservices(): Pair<List<DashboardItem>, NetworkMetrics> {
        if (simulateOfflineMode) throw Exception("Błąd sieci: Brak połączenia z serwerem")

        // Kaskadowe wybudzanie modemu:
        delay(400) // Zapytanie 1 do UserService
        delay(400) // Zapytanie 2 do OrderService
        delay(400) // Zapytanie 3 do RecommendationService

        val items = listOf(
            DashboardItem("1", "Użytkownik", "Jan Kowalski", "Konto VIP"),
            DashboardItem("2", "Aktywne Zamówienie", "#ORD-9821", "W doręczeniu"),
            DashboardItem("3", "Rekomendacja", "Kurs Kotlin 2026", "Promocja -30%")
        )
        // 3 zapytania, ogromny surowy JSON z każdego mikroserwisu, modem wybudzony na długo:
        val metrics = NetworkMetrics(requestCount = 3, payloadSizeKb = 215, latencyMs = 1200, radioAwakeSeconds = 25)
        return Pair(items, metrics)
    }

    // B. SCENARIUSZ Z WZORCEM BFF: 1 zapytanie zagregowane, przycięty payload
    suspend fun fetchViaMobileBff(): Pair<List<DashboardItem>, NetworkMetrics> {
        if (simulateOfflineMode) throw Exception("Błąd sieci: Brak połączenia z serwerem")

        // Pojedyncze zapytanie do BFF (agregacja odbyła się w chmurze w 10 ms):
        delay(350)

        val items = listOf(
            DashboardItem("1", "Użytkownik", "Jan Kowalski", "Konto VIP"),
            DashboardItem("2", "Aktywne Zamówienie", "#ORD-9821", "W doręczeniu"),
            DashboardItem("3", "Rekomendacja", "Kurs Kotlin 2026", "Promocja -30%")
        )
        // 1 zapytanie, odcięte zbędne pola, 1 runda radiowa:
        val metrics = NetworkMetrics(requestCount = 1, payloadSizeKb = 8, latencyMs = 350, radioAwakeSeconds = 12)
        return Pair(items, metrics)
    }
}

// ============================================================================
// 3. EKRAN DEMONSTRACYJNY COMPOSE (W08)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W08_NetworkingBffScreen(
    networkEngine: MockNetworkEngine = remember { MockNetworkEngine() }
) {
    val coroutineScope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<DashboardItem>>(emptyList()) }
    var metrics by remember { mutableStateOf<NetworkMetrics?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentModeTitle by remember { mutableStateOf("Brak danych (Kliknij test)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sieć & Wzorzec BFF - Demo W08", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PANEL STEROWANIA I TESTÓW
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Symulacja trybu offline (Błąd sieci):", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = networkEngine.simulateOfflineMode,
                            onCheckedChange = { networkEngine.simulateOfflineMode = it }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Przycisk testujący bezpośrednie zapytania (BEZ BFF)
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    currentModeTitle = "Bezpośrednio do 3 Mikroserwisów (Antywzorzec)"
                                    try {
                                        val result = networkEngine.fetchDirectFromMicroservices()
                                        items = result.first
                                        metrics = result.second
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1. Bez BFF")
                        }

                        // Przycisk testujący zapytanie przez Mobile BFF
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    currentModeTitle = "Przez Mobile BFF (Zalecany Wzorzec)"
                                    try {
                                        val result = networkEngine.fetchViaMobileBff()
                                        items = result.first
                                        metrics = result.second
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2. Z wzorcem BFF")
                        }
                    }
                }
            }

            // WYNIKI POMIARÓW SIECIOWYCH
            metrics?.let { m ->
                Surface(
                    color = if (m.requestCount == 1) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📊 Metryki Sieciowe ($currentModeTitle):", fontWeight = FontWeight.Bold)
                        Text("• Liczba żądań HTTP: ${m.requestCount}")
                        Text("• Rozmiar danych: ${m.payloadSizeKb} KB (Oszczędność: ${if (m.requestCount == 1) "96%" else "0%"})")
                        Text("• Czas odpowiedzi: ${m.latencyMs} ms")
                        Text("• Czas wybudzenia radia komórkowego: ~${m.radioAwakeSeconds} s")
                    }
                }
            }

            // OBSŁUGA BŁĘDU
            errorMessage?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // OBSŁUGA ŁADOWANIA
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // LISTA POBRANYCH DANYCH
            if (!isLoading && errorMessage == null && items.isNotEmpty()) {
                Text("Pobrane Dane Ekranu:", fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items) { item ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.title, fontWeight = FontWeight.Bold)
                                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(item.value) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W08 Networking & BFF Demo")
@Composable
fun PreviewW08Networking() {
    MaterialTheme {
        W08_NetworkingBffScreen()
    }
}