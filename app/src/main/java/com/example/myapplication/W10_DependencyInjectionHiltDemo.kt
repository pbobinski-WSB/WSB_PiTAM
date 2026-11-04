package pl.merito.mobile.w10

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Refresh
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
// 1. KONTRAKT I MODELE DOMENOWE (Clean Architecture)
// ============================================================================
data class DeviceItem(
    val id: String,
    val name: String,
    val sourceDescription: String,
    val price: Double
)

// Interfejs, od którego zależy ViewModel (Programowanie pod interfejsy - Slajd 10.4)
interface DeviceRepository {
    suspend fun fetchDevices(): List<DeviceItem>
    fun getSourceType(): String
}

// ============================================================================
// 2. IMPLEMENTACJA A: PRODUKCYJNA (Symulacja Retrofit / Network - Slajd 10.25)
// ============================================================================
class ProductionDeviceRepository : DeviceRepository {
    override suspend fun fetchDevices(): List<DeviceItem> {
        delay(1200) // Symulacja opóźnienia sieciowego REST API
        return listOf(
            DeviceItem("p1", "Google Pixel 9 Pro", "Zdalny Serwer REST (Chmura)", 4999.00),
            DeviceItem("p2", "Samsung Galaxy S24", "Zdalny Serwer REST (Chmura)", 4199.00),
            DeviceItem("p3", "iPhone 16 Pro Max", "Zdalny Serwer REST (Chmura)", 6599.00)
        )
    }

    override fun getSourceType(): String = "PRODUKCJA (Prawdziwe API sieciowe)"
}

// ============================================================================
// 3. IMPLEMENTACJA B: TESTOWA / MOCKOWA (Symulacja Fake / TestModule - Slajd 10.44)
// ============================================================================
class MockDeviceRepository : DeviceRepository {
    override suspend fun fetchDevices(): List<DeviceItem> {
        delay(100) // Natychmiastowa odpowiedź z pamięci testowej
        return listOf(
            DeviceItem("m1", "MOCK Urządzenie Alfa", "Lokalna Atrapa Testowa (Pamięć RAM)", 1.00),
            DeviceItem("m2", "MOCK Urządzenie Beta", "Lokalna Atrapa Testowa (Pamięć RAM)", 2.00)
        )
    }

    override fun getSourceType(): String = "TESTY (Podstawiona atrapa Mock)"
}

// ============================================================================
// 4. SYMULATOR KONTENERA DI (Zastępuje kod generowany przez Hilt w podglądzie)
// ============================================================================
class DiContainerSimulator {
    // Aktualnie wstrzykiwane repozytorium (odpowiednik podmiany modułu w Hilt)
    var activeRepository: DeviceRepository by mutableStateOf(ProductionDeviceRepository())

    fun switchToProduction() {
        activeRepository = ProductionDeviceRepository()
    }

    fun switchToMock() {
        activeRepository = MockDeviceRepository()
    }
}

// ============================================================================
// 5. EKRAN DEMONSTRACYJNY W COMPOSE (W10)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W10_DiDemoScreen(
    diContainer: DiContainerSimulator = remember { DiContainerSimulator() }
) {
    val coroutineScope = rememberCoroutineScope()
    var devicesList by remember { mutableStateOf<List<DeviceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Funkcja ładująca dane z aktualnie "wstrzykniętego" repozytorium
    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            devicesList = diContainer.activeRepository.fetchDevices()
            isLoading = false
            snackbarHostState.showSnackbar("Wstrzyknięto dane z: ${diContainer.activeRepository.getSourceType()}")
        }
    }

    // Pierwsze załadowanie
    LaunchedEffect(diContainer.activeRepository) {
        loadData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dependency Injection Demo - W10", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KARTA STEROWANIA GRAFEM ZALEŻNOŚCI (Demonstracja Inversion of Control)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kontroler Grafu Wstrzykiwania (Hilt Container):", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Aktywna zależność: ${diContainer.activeRepository.getSourceType()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Wstrzyknięcie repozytorium produkcyjnego
                        Button(
                            onClick = { diContainer.switchToProduction() },
                            enabled = diContainer.activeRepository !is ProductionDeviceRepository && !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cloud, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Wstrzyknij PROD")
                        }

                        // Wstrzyknięcie atrapy testowej (TestModule)
                        OutlinedButton(
                            onClick = { diContainer.switchToMock() },
                            enabled = diContainer.activeRepository !is MockDeviceRepository && !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DataObject, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Wstrzyknij MOCK")
                        }
                    }
                }
            }

            // PASEK STATUSU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Widok UI (Zużywa interfejs DeviceRepository):", fontWeight = FontWeight.Bold)
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            // LISTA WYŚWIETLAJĄCA WSTRZYKNIĘTE DANE
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devicesList, key = { it.id }) { item ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(item.sourceDescription, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(
                                text = "${item.price} PLN",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W10 Dependency Injection Demo")
@Composable
fun PreviewW10DiDemo() {
    MaterialTheme {
        W10_DiDemoScreen()
    }
}