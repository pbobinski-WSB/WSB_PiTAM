package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ============================================================================
// 1. SYMULACJA MODELI BAZY DANYCH I STATUSU SYNCHRONIZACJI
// ============================================================================
enum class SyncState { SYNCED, PENDING_SYNC }

data class OfflineNote(
    val id: String,
    val content: String,
    val syncState: SyncState,
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 2. SYMULACJA REPOZYTORIUM OFFLINE-FIRST (SSOT)
// ============================================================================
class MockOfflineFirstRepository {
    // Symulacja bazy danych Room emitującej stan przez Flow:
    private val _localDatabase = MutableStateFlow(
        listOf(
            OfflineNote("1", "Kupić mleko i kawę", SyncState.SYNCED),
            OfflineNote("2", "Przygotować slajdy na Wykład 10", SyncState.SYNCED)
        )
    )
    val notesStream = _localDatabase.asStateFlow()

    var isNetworkAvailable by mutableStateOf(true)

    // OPTYMISTYCZNY ZAPIS LOKALNY (Wykonuje się w ułamku sekundy w bazie Room)
    suspend fun addNote(content: String): String {
        val newId = System.currentTimeMillis().toString()
        val initialSyncState = if (isNetworkAvailable) SyncState.PENDING_SYNC else SyncState.PENDING_SYNC
        val newNote = OfflineNote(newId, content, initialSyncState)

        // 1. ZAPIS DO BAZY: UI zaktualizuje się natychmiast!
        _localDatabase.value = listOf(newNote) + _localDatabase.value

        // 2. PRÓBA SYNCHRONIZACJI Z SERWEREM W TLE:
        if (isNetworkAvailable) {
            delay(1200) // Symulacja zapytania sieciowego do chmury
            // Po udanej synchronizacji zmieniamy status w bazie na SYNCED:
            _localDatabase.value = _localDatabase.value.map {
                if (it.id == newId) it.copy(syncState = SyncState.SYNCED) else it
            }
        }
        return newId
    }

    // Ręczna synchronizacja po odzyskaniu połączenia z siecią:
    suspend fun syncPendingNotes() {
        if (!isNetworkAvailable) return
        delay(1000)
        _localDatabase.value = _localDatabase.value.map {
            it.copy(syncState = SyncState.SYNCED)
        }
    }
}

// ============================================================================
// 3. EKRAN DEMONSTRACYJNY JETPACK COMPOSE (W09)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun W09_OfflineFirstScreen(
    repository: MockOfflineFirstRepository = remember { MockOfflineFirstRepository() }
) {
    val notes by repository.notesStream.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var inputNoteText by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline-First & Room - Demo W09", fontWeight = FontWeight.Bold) },
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
            // PANEL KONTROLNY SIECI (Symulacja Trybu Samolotowego)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (repository.isNetworkAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (repository.isNetworkAvailable) "Stan: ONLINE (Połączono)" else "Stan: OFFLINE (Brak sieci)",
                            fontWeight = FontWeight.Bold,
                            color = if (repository.isNetworkAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text("Przełącz, aby przetestować działanie offline", style = MaterialTheme.typography.bodySmall)
                    }

                    Switch(
                        checked = repository.isNetworkAvailable,
                        onCheckedChange = { isOnline ->
                            repository.isNetworkAvailable = isOnline
                            if (isOnline) {
                                coroutineScope.launch {
                                    isSyncing = true
                                    repository.syncPendingNotes()
                                    isSyncing = false
                                }
                            }
                        }
                    )
                }
            }

            // FORMULARZ DODAWANIA NOTATKI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputNoteText,
                    onValueChange = { inputNoteText = it },
                    label = { Text("Treść nowej notatki...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (inputNoteText.isNotBlank()) {
                            val textToSend = inputNoteText
                            inputNoteText = ""
                            coroutineScope.launch {
                                repository.addNote(textToSend)
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }

            // NAGŁÓWEK LISTY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lokalna Baza Danych (${notes.size}):", fontWeight = FontWeight.Bold)
                if (isSyncing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Synchronizacja w tle...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // LISTA NOTATEK Z BAZY DANYCH
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes, key = { it.id }) { note ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note.content, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)

                            // ODZNAKA STANU SYNCHRONIZACJI
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (note.syncState == SyncState.SYNCED) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (note.syncState == SyncState.SYNCED) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (note.syncState == SyncState.SYNCED) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (note.syncState == SyncState.SYNCED) "Zsynchronizowano" else "Oczekuje na sieć",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (note.syncState == SyncState.SYNCED) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
@Preview(showBackground = true, name = "W09 Offline-First Demo")
@Composable
fun PreviewW09OfflineFirst() {
    MaterialTheme {
        W09_OfflineFirstScreen()
    }
}