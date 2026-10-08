package com.example.meritomobiledemo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ============================================================================
// 1. GŁÓWNY INTERAKTYWNY EKRAN WYKŁADOWY (W06)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W06_CoroutinesDemoScreen(modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ------------------------------------------------------------------------
    // DEMO A: ŻYWY STRUMIEŃ FLOW (Licznik czasu w czasie rzeczywistym)
    // ------------------------------------------------------------------------
    // produceState pozwala w bezpieczny sposób przekształcić kod Flow w stan Compose
    val timerSeconds by produceState(initialValue = 0) {
        while (true) {
            delay(1000) // Wstrzymanie na 1 sekundę (nie blokuje wątku UI!)
            value += 1
        }
    }

    // ------------------------------------------------------------------------
    // DEMO B: RÓWNOLEGŁOŚĆ Z async / await + ANULOWANIE (Job.cancel)
    // ------------------------------------------------------------------------
    var isParallelRunning by remember { mutableStateOf(false) }
    var taskAStatus by remember { mutableStateOf("Oczekiwanie") }
    var taskBStatus by remember { mutableStateOf("Oczekiwanie") }
    var runningJob by remember { mutableStateOf<Job?>(null) }

    // ------------------------------------------------------------------------
    // DEMO C: WYSZUKIWARKA Z DEBOUNCE (Flow + debounce + filter)
    // ------------------------------------------------------------------------
    var searchInput by remember { mutableStateOf("") }
    val allTechnologies = remember {
        listOf(
            "Kotlin Coroutines", "Kotlin Flow", "StateFlow", "SharedFlow",
            "Jetpack Compose", "TypeScript", "React Native", "SwiftUI",
            "gRPC & Protobuf", "Ktor Client", "Room Database"
        )
    }

    // Strumień filtrujący z opóźnieniem debounce(300ms)
    val filteredTechnologies by produceState(initialValue = allTechnologies, searchInput) {
        delay(300) // Debounce: Czekamy 300ms na przerwę w pisaniu
        value = if (searchInput.isBlank()) {
            allTechnologies
        } else {
            allTechnologies.filter { it.contains(searchInput, ignoreCase = true) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Coroutines & Flow Demo (W06)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================================================================
            // KARTA 1: Strumień Czasu Rzeczywistego (Cold Flow -> State)
            // ================================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically     // <-- poprawny parametr dla Row
                ) {
                    Column {
                        Text("Strumień czasu (Flow emit co 1s):", style = MaterialTheme.typography.bodySmall)
                        Text("Aplikacja aktywna przez:", fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${timerSeconds} s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ================================================================
            // KARTA 2: Równoległe Zadania Asynchroniczne (async / await / cancel)
            // ================================================================
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Równoległość (async / await):", fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Zadanie A (API 1.5s):", style = MaterialTheme.typography.bodySmall)
                        Text(taskAStatus, fontWeight = FontWeight.SemiBold, color = if (taskAStatus == "Gotowe!") Color(0xFF2E7D32) else Color.Unspecified)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Zadanie B (Baza 1.0s):", style = MaterialTheme.typography.bodySmall)
                        Text(taskBStatus, fontWeight = FontWeight.SemiBold, color = if (taskBStatus == "Gotowe!") Color(0xFF2E7D32) else Color.Unspecified)
                    }

                    if (isParallelRunning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                runningJob = coroutineScope.launch {
                                    isParallelRunning = true
                                    taskAStatus = "Pobieranie..."
                                    taskBStatus = "Pobieranie..."

                                    // Dwa równoległe zadania async:
                                    val deferredA = async {
                                        delay(1500)
                                        taskAStatus = "Gotowe!"
                                        "Zasób A"
                                    }
                                    val deferredB = async {
                                        delay(1000)
                                        taskBStatus = "Gotowe!"
                                        "Zasób B"
                                    }

                                    // Czekamy na oba (Łącznie tylko 1.5s zamiast 2.5s!):
                                    awaitAll(deferredA, deferredB)
                                    isParallelRunning = false
                                    snackbarHostState.showSnackbar("Pobrano oba zasoby równolegle!")
                                }
                            },
                            enabled = !isParallelRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Start async")
                        }

                        // Przycisk testujący anulowanie koprocedury:
                        OutlinedButton(
                            onClick = {
                                runningJob?.cancel() // Anulowanie drzewa koprocedury
                                isParallelRunning = false
                                taskAStatus = "Anulowano ⛔"
                                taskBStatus = "Anulowano ⛔"
                            },
                            enabled = isParallelRunning
                        ) {
                            Icon(Icons.Default.Close, null)
                            Text("Anuluj")
                        }
                    }
                }
            }

            // ================================================================
            // KARTA 3: Reaktywne Wyszukiwanie (Flow Debounce 300ms)
            // ================================================================
            Text("Reaktywne Wyszukiwanie (debounce 300ms):", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                label = { Text("Wpisz technologię (np. Flow, Kot, Sc)...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Lista przefiltrowana
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredTechnologies) { tech ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tech,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. STATYCZNE I INTERAKTYWNE PODGLĄDY DLA ANDROID STUDIO (@Preview)
// ============================================================================

// Podgląd 1: Główny interaktywny pulpit (Włącz tryb łapki i testuj na żywo!)
@Preview(showBackground = true, name = "1. Interaktywny Pulpit Coroutines & Flow")
@Composable
fun PreviewInteractiveCoroutines() {
    MaterialTheme {
        W06_CoroutinesDemoScreen()
    }
}