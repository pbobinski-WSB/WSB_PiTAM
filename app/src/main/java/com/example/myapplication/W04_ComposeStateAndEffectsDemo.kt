package pl.merito.mobile.w04

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// GŁÓWNY EKRAN DEMONSTRACYJNY STANU I EFEKTÓW
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W04_StateAndEffectsDemoScreen() {
    // ------------------------------------------------------------------------
    // 1. rememberSaveable: Przetrwa obrót ekranu i zmianę konfiguracji!
    // ------------------------------------------------------------------------
    var taskInputText by rememberSaveable { mutableStateOf("") }
    var taskList by rememberSaveable { mutableStateOf(listOf("Przygotować prezentację", "Nauczyć się Compose", "Dupa")) }
    
    // ------------------------------------------------------------------------
    // 2. remember + mutableStateOf: Stan asynchronicznego ładowania z API
    // ------------------------------------------------------------------------
    var isApiLoading by remember { mutableStateOf(true) }
    var serverStatusMessage by remember { mutableStateOf("Łączenie z serwerem...") }

    // ------------------------------------------------------------------------
    // 3. rememberCoroutineScope & Snackbar: Obsługa zdarzeń z przycisków
    // ------------------------------------------------------------------------
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Stan scrollowania listy
    val listState = rememberLazyListState()

    // ------------------------------------------------------------------------
    // 4. derivedStateOf: Optymalizacja Rekompozycji
    // Przycisk "Przewiń do góry" przelicza się TYLKO gdy przekroczymy próg 0. indeksu,
    // a nie przy każdym pojedynczym pikselu przewijania listy!
    // ------------------------------------------------------------------------
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // ------------------------------------------------------------------------
    // 5. LaunchedEffect: Efekt uboczny odpalany przy wejściu na ekran
    // Symulacja asynchronicznego zapytania sieciowego (odpowiednik useEffect(..., []))
    // ------------------------------------------------------------------------
    LaunchedEffect(Unit) {
        delay(2000) // Symulacja opóźnienia sieciowego (2 sekundy)
        isApiLoading = false
        serverStatusMessage = "Połączono z API (Wszystkie systemy sprawne)"
    }

    // ------------------------------------------------------------------------
    // 6. DisposableEffect: Efekt ze sprzątaniem po sobie (Cleanup)
    // Odpowiednik useEffect(() => { return () => cleanup() }, [])
    // ------------------------------------------------------------------------
    DisposableEffect(Unit) {
        println("--> [DisposableEffect]: Rejestracja nasłuchiwania czujników / cyklu życia")
        
        onDispose {
            // Wywoła się automatycznie, gdy ten ekran zostanie usunięty z widoku!
            println("<-- [DisposableEffect]: Wyrejestrowanie listenerów (Sprzątanie pamięci)")
        }
    }

    // STRUKTURA WIZUALNA EKRANU
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Zarządzanie Stanem - Demo W04") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        floatingActionButton = {
            // Przycisk "Wróć na górę" sterowany przez derivedStateOf
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "W górę")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BANER STATUSU (Sterowany przez LaunchedEffect)
            StatusCard(isLoading = isApiLoading, message = serverStatusMessage)

            // FORMULARZ (Demonstracja State Hoisting / Komponentu Bezstanowego)
            TaskInputSection(
                inputText = taskInputText,
                onTextChanged = { taskInputText = it },
                onAddTask = {
                    if (taskInputText.isNotBlank()) {
                        taskList = taskList + taskInputText
                        val addedItemName = taskInputText
                        taskInputText = "" // Czyszczenie formularza

                        // Wyzwalanie Snackbara w koprocedurze
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Dodano zadanie: '$addedItemName'")
                        }
                    }
                }
            )

            HorizontalDivider()

            // LISTA ZADAŃ
            Text(
                text = "Lista Zadań (${taskList.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = taskList,
                    key = { task -> task } // Klucz unikalny
                ) { task ->
                    TaskItemRow(
                        taskName = task,
                        onDelete = {
                            taskList = taskList.filter { it != task }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Usunięto: '$task'")
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// KOMPONENT BEZSTANOWY (Stateless Component - Czysty State Hoisting)
// ============================================================================
@Composable
fun TaskInputSection(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ten komponent NIE TRZYMA W SOBIE STANU – jest w 100% testowalny i reużywalny!
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChanged,
            label = { Text("Nowe zadanie...") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        Button(
            onClick = onAddTask,
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj")
        }
    }
}

// ============================================================================
// KOMPONENT ELEMENTU LISTY (Stateless)
// ============================================================================
@Composable
fun TaskItemRow(
    taskName: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = taskName, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ============================================================================
// KOMPONENT STATUSU ASYNCHRONICZNEGO
// ============================================================================
@Composable
fun StatusCard(
    isLoading: Boolean,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoading) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE8F5E9)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF2E7D32)
            )
        }
    }
}

// ============================================================================
// PODGLĄD W ANDROID STUDIO
// ============================================================================
@Preview(showBackground = true, name = "W04 Demo - State & Effects")
@Composable
fun PreviewW04Screen() {
    MaterialTheme {
        W04_StateAndEffectsDemoScreen()
    }
}