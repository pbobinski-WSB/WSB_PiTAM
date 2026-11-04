package pl.merito.mobile.w14

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.sqrt

// ============================================================================
// 1. REPREZENTACJA DANYCH POMIAROWYCH Z PRACY MAGISTERSKIEJ (Tabela 6.1)
// ============================================================================
data class BenchmarkEntry(
    val patternName: String,
    val energyJoules: Double,     // M10: Energia netto [J] (Kryterium Kosztu)
    val p99FrameTimeMs: Double,   // M13: 99. percentyl klatki [ms] (Kryterium Kosztu)
    val memoryRssMb: Double,      // M1: Zużycie RAM [MB] (Kryterium Kosztu)
    val cpuTimeSeconds: Double    // M7: Czas procesora [s] (Kryterium Kosztu)
)

data class TopsisResult(
    val patternName: String,
    val scoreCi: Double,
    val rank: Int
)

enum class BusinessProfile(val label: String, val wEnergy: Double, val wSmooth: Double, val wRam: Double, val wCpu: Double) {
    ENERGY_SAVER("Energooszczędny (Bateria)", 0.55, 0.15, 0.15, 0.15),
    RESPONSIVE("Responsywny (Płynność 120 FPS)", 0.15, 0.55, 0.15, 0.15),
    BALANCED("Zrównoważony (Równe wagi)", 0.25, 0.25, 0.25, 0.25)
}

// ============================================================================
// 2. SILNIK MATEMATYCZNY METODY TOPSIS (Rozdział 2.5.1 Pracy)
// ============================================================================
object TopsisCalculator {
    fun calculateRanking(
        entries: List<BenchmarkEntry>,
        profile: BusinessProfile
    ): List<TopsisResult> {
        val m = entries.size // Liczba alternatyw (4)
        val weights = doubleArrayOf(profile.wEnergy, profile.wSmooth, profile.wRam, profile.wCpu)

        // 1. Macierz wejściowa X (m x 4)
        val matrix = Array(m) { i ->
            doubleArrayOf(
                entries[i].energyJoules,
                entries[i].p99FrameTimeMs,
                entries[i].memoryRssMb,
                entries[i].cpuTimeSeconds
            )
        }

        // 2. Normalizacja wektorowa (Wzór 1 w pracy)
        val norm = DoubleArray(4)
        for (j in 0 until 4) {
            var sumSquares = 0.0
            for (i in 0 until m) {
                sumSquares += matrix[i][j].pow(2.0)
            }
            norm[j] = sqrt(sumSquares)
        }

        // 3. Ważona macierz znormalizowana V (Wzór 2 w pracy)
        val v = Array(m) { i ->
            DoubleArray(4) { j ->
                (matrix[i][j] / norm[j]) * weights[j]
            }
        }

        // 4. Rozwiązanie Idealne (A+) i Anty-idealne (A-) dla kryteriów KOSZTU (Wzór 3 w pracy)
        val idealA = DoubleArray(4) { j -> (0 until m).minOf { i -> v[i][j] } }     // Minima = Najlepsze
        val antiIdealA = DoubleArray(4) { j -> (0 until m).maxOf { i -> v[i][j] } } // Maksima = Najgorsze

        // 5. Odległości Euklidesowe i Współczynnik Bliskości Ci (Wzory 4 i 5)
        val results = entries.indices.map { i ->
            var dPlusSum = 0.0
            var dMinusSum = 0.0
            for (j in 0 until 4) {
                dPlusSum += (v[i][j] - idealA[j]).pow(2.0)
                dMinusSum += (v[i][j] - antiIdealA[j]).pow(2.0)
            }
            val dPlus = sqrt(dPlusSum)
            val dMinus = sqrt(dMinusSum)
            val cI = if (dPlus + dMinus == 0.0) 0.0 else dMinus / (dPlus + dMinus)

            TopsisResult(patternName = entries[i].patternName, scoreCi = cI, rank = 0)
        }

        // Sortowanie malejąco według Ci i nadanie rang
        return results.sortedByDescending { it.scoreCi }
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }
}

// ============================================================================
// 3. EKRAN DEMONSTRACYJNY W JETPACK COMPOSE (W14)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W14_TopsisDemoScreen() {
    var selectedProfile by remember { mutableStateOf(BusinessProfile.BALANCED) }

    // Rzeczywiste dane empiryczne z tabeli 6.1 pracy (Pixel 6a, scenariusz dynamiczny k=250)
    val empiricalData = remember {
        listOf(
            BenchmarkEntry("ValueNotifier (Wbudowany)", 43.9, 20.7, 371.8, 37.2),
            BenchmarkEntry("BLoC / Cubit (Strumienie)", 45.7, 20.5, 370.8, 38.6),
            BenchmarkEntry("Riverpod (Graf Providerów)", 58.9, 19.8, 379.9, 44.5),
            BenchmarkEntry("Dart Signals (Drobnoziarnisty)", 44.6, 20.6, 371.9, 37.5)
        )
    }

    // Automatyczne przeliczenie rankingu TOPSIS przy każdej zmianie profilu
    val ranking by remember(selectedProfile) {
        derivedStateOf {
            TopsisCalculator.calculateRanking(empiricalData, selectedProfile)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model TOPSIS: Wybór Wzorca (W14)", fontWeight = FontWeight.Bold) },
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
            // PANEL WYBORU PROFILU BIZNESOWEGO
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wybierz Profil Wymagań Aplikacji (Wagi TOPSIS):", fontWeight = FontWeight.Bold)

                    BusinessProfile.entries.forEach { profile ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedProfile == profile,
                                onClick = { selectedProfile = profile }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(profile.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // NAGŁÓWEK WYNIKÓW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wyniki Modelu TOPSIS (Google Pixel 6a):", fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp)) {
                    Text("42 rekomendacje PW", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                }
            }

            // LISTA RANKINGOWA TOPSIS
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(ranking) { _, result ->
                    val isWinner = result.rank == 1
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isWinner) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isWinner) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50))) else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isWinner) Color(0xFF2E7D32) else Color.Gray,
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#${result.rank}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(result.patternName, fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium)
                                    Text("Wskaźnik bliskości Ci: ${String.format("%.4f", result.scoreCi)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }

                            if (isWinner) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = "Zwycięzca", tint = Color(0xFFFBC02D))
                            }
                        }
                    }
                }
            }

            // WNIOSEK NAUKOWY NA DOLE
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 Wniosek: W profilu ${selectedProfile.label} liderem jest ${ranking.first().patternName}. Zmiana wag kryteriów dynamicznie rekonfiguruje odległość od ideału A+!",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================================
// 4. PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================
@Preview(showBackground = true, name = "W14 TOPSIS Model Demo")
@Composable
fun PreviewW14Topsis() {
    MaterialTheme {
        W14_TopsisDemoScreen()
    }
}