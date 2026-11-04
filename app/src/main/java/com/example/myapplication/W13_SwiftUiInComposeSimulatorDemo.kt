package pl.merito.mobile.w13

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// SYMULATOR WZORCÓW SWIFTUI W ŚRODOWISKU ANDROID STUDIO (W13)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W13_SwiftUiComparisonScreen() {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Symulator iOS UI, 1 = Porównanie Kodu
    var isSubscribed by remember { mutableStateOf(false) } // Stan symulujący @State w SwiftUI

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SwiftUI & iOS w Praktyce (W13)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            // SEGMENTED CONTROL W STYLU APPLE (iOS HIG Component)
            IosSegmentedControl(
                selectedIndex = selectedTab,
                options = listOf("Podgląd Stylu iOS", "Porównanie Kodu"),
                onOptionSelected = { selectedTab = it }
            )

            if (selectedTab == 0) {
                // SEKCJA A: Interfejs odwzorowujący natywne zachowania i komponenty iOS
                IosUiDemonstration(
                    isSubscribed = isSubscribed,
                    onToggleSubscription = { isSubscribed = !isSubscribed }
                )
            } else {
                // SEKCJA B: Zestawienie kodu linijka po linijce
                CodeComparisonSection()
            }
        }
    }
}

// ============================================================================
// KOMPONENT 1: Symulacja Stylu iOS (Karty, Przełączniki, HIG)
// ============================================================================
@Composable
fun IosUiDemonstration(
    isSubscribed: Boolean,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Natywny Design System iOS (Human Interface Guidelines):", fontWeight = FontWeight.Bold)

        // Grupowana karta w stylu iOS Settings / Inset Grouped List
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF2F2F7), // Oficjalny szary kolor tła w iOS (.systemGray6)
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF007AFF), // Oficjalny błękit Apple (.systemBlue)
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhoneIphone, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Apple Push Notifications", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Symulacja stanu: @Binding", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // iOS-owy zielony przełącznik Toggle
                    Switch(
                        checked = isSubscribed,
                        onCheckedChange = { onToggleSubscription() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759)) // iOS Green
                    )
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                // Wiersz listy z nawigacją i strzałką Chevron
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nawigacja: NavigationStack (iOS 16+)", fontSize = 14.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                }
            }
        }

        // Karta Statusu
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSubscribed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isSubscribed) "Status: Powiadomienia włączone przez @State" else "Status: Wyłączone (@Binding zgłosił zmianę)",
                color = if (isSubscribed) Color(0xFF2E7D32) else Color(0xFFE65100),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// ============================================================================
// KOMPONENT 2: Zestawienie Kodu (Lustrzana Symetria)
// ============================================================================
@Composable
fun CodeComparisonSection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Zestawienie Kodu: Identyczny Model Myślowy!", fontWeight = FontWeight.Bold)

        // Kafelek KODU SwiftUI
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🍎 Kod w SwiftUI (iOS):", color = Color(0xFF4FC3F7), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = """
                    struct CounterView: View {
                        @State private var count = 0
                        
                        var body: some View {
                            VStack(spacing: 8) {
                                Text("Licznik: \(count)")
                                Button("Dodaj") { count += 1 }
                            }
                            .padding()
                        }
                    }
                    """.trimIndent(),
                    color = Color(0xFFE0E0E0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Kafelek KODU Jetpack Compose
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🟢 Kod w Jetpack Compose (Android):", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = """
                    @Composable
                    fun CounterView() {
                        var count by remember { mutableStateOf(0) }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Licznik: ${'$'}count")
                            Button(onClick = { count++ }) { Text("Dodaj") }
                        }
                        .padding(16.dp)
                    }
                    """.trimIndent(),
                    color = Color(0xFFE0E0E0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ============================================================================
// KOMPONENT POMOCNICZY: iOS Segmented Control
// ============================================================================
@Composable
fun IosSegmentedControl(
    selectedIndex: Int,
    options: List<String>,
    onOptionSelected: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFFE5E5EA), // Szare tło kontrolera w iOS
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(2.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ============================================================================
// PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W13 SwiftUI Demo")
@Composable
fun PreviewW13SwiftUiSimulator() {
    MaterialTheme {
        W13_SwiftUiComparisonScreen()
    }
}