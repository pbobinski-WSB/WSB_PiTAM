package pl.merito.mobile.w12

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ============================================================================
// EKRAN DEMONSTRACYJNY SPRZĘTU, UPRAWNIEŃ I POWIADOMIEŃ (W12)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W12_HardwarePermissionsScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ------------------------------------------------------------------------
    // STAN 1: SYMULACJA UPRAWNIEŃ RUNTIME (Slajd 12.7 - 12.9)
    // ------------------------------------------------------------------------
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }

    // ------------------------------------------------------------------------
    // STAN 2: SYMULACJA SENSORA ŚWIATŁA (Slajd 12.23 - 12.27)
    // ------------------------------------------------------------------------
    var simulatedLux by remember { mutableFloatStateOf(350f) }

    // ------------------------------------------------------------------------
    // STAN 3: SYMULACJA POWIADOMIENIA SYSTEMOWEGO (Slajd 12.43 - 12.46)
    // ------------------------------------------------------------------------
    var lastSentNotification by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sprzęt & Uprawnienia - Demo W12", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
            // ================================================================
            // KARTA 1: Activity Result API & Obsługa Rationale (Aparat)
            // ================================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Uprawnienia Runtime (Kamera / CameraX):", fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status uprawnienia CAMERA:")
                        Surface(
                            color = if (cameraPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (cameraPermissionGranted) "PRZYZNANE ✅" else "BRAK ZGODY ❌",
                                color = if (cameraPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (!cameraPermissionGranted) {
                                    showRationaleDialog = true // Wyświetlenie dialogu wyjaśniającego
                                }
                            },
                            enabled = !cameraPermissionGranted,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Poproś o zgodę")
                        }

                        if (cameraPermissionGranted) {
                            OutlinedButton(
                                onClick = { cameraPermissionGranted = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cofnij zgodę")
                            }
                        }
                    }
                }
            }

            // ================================================================
            // KARTA 2: Sensor Framework (Czujnik Światła & DisposableEffect)
            // ================================================================
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Sensor Framework (Czujnik Oświetlenia):", fontWeight = FontWeight.Bold)
                    Text("Aktualny odczyt: ${simulatedLux.toInt()} lx (Luksów)", style = MaterialTheme.typography.bodyMedium)

                    // Suwak symulujący natężenie światła padającego na sensor
                    Slider(
                        value = simulatedLux,
                        onValueChange = { simulatedLux = it },
                        valueRange = 0f..1000f
                    )

                    // Automatyczna reakcja UI na stan sensora
                    Surface(
                        color = if (simulatedLux < 200f) Color(0xFF212121) else Color(0xFFFFF9C4),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (simulatedLux < 200f) "🌙 Ciemno -> Tryb Nocny (Aparat włącza Night Mode)" else "☀️ Jasno -> Tryb Standardowy",
                            color = if (simulatedLux < 200f) Color.White else Color(0xFFF57F17),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // ================================================================
            // KARTA 3: Powiadomienia Systemowe (Notification Channel & Builder)
            // ================================================================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Powiadomienia Systemowe (Android 13+):", fontWeight = FontWeight.Bold)
                    Text("Kanał: 'orders_channel' | Priorytet: HIGH", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Button(
                        onClick = {
                            lastSentNotification = "Powiadomienie #${(100..999).random()}: Twoja przesyłka dotarła!"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Wyślij Powiadomienie Systemowe")
                    }

                    lastSentNotification?.let { notif ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(notif, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG WYJAŚNIAJĄCY (Permission Rationale Dialog - Slajd 12.9)
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = { Icon(Icons.Default.Info, null) },
            title = { Text("Wymagany Dostęp do Kamery") },
            text = { Text("Aplikacja potrzebuje aparatu, aby umożliwić skanowanie kodów QR i robienie zdjęć profilowych.") },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    cameraPermissionGranted = true // Symulacja przyznania uprawnienia przez użytkownika
                }) {
                    Text("Zezwól")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRationaleDialog = false }) {
                    Text("Odmów")
                }
            }
        )
    }
}

// ============================================================================
// PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W12 Hardware & Permissions Demo")
@Composable
fun PreviewW12HardwareDemo() {
    MaterialTheme {
        W12_HardwarePermissionsScreen()
    }
}