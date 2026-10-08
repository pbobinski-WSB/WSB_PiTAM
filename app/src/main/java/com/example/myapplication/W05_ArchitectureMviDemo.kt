package pl.merito.mobile.w05

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ============================================================================
// 1. WARSTWA DOMENOWA I DANYCH (Domain & Data Layer)
// ============================================================================

// Czysty model domenowy (używany w logice biznesowej i UI)
data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val category: String
)

// Interfejs Repozytorium (Programowanie pod interfejsy, nie implementacje - Slajd 5.36)
interface ProductRepository {
    suspend fun getProducts(): Result<List<Product>>
    suspend fun addToCart(productId: String): Result<Unit>
}

// Implementacja Repozytorium (Single Source of Truth - Slajd 5.33, 5.38)
class FakeProductRepositoryImpl : ProductRepository {
    // Symulacja bazy danych / cache'u w pamięci
    private val localProducts = listOf(
        Product("p1", "MacBook Pro M3", 8999.00, "Laptopy"),
        Product("p2", "Monitor Dell 27' 4K", 2199.00, "Monitory"),
        Product("p3", "Klawiatura Mechaniczna", 450.00, "Akcesoria"),
        Product("p4", "Mysz Bezprzewodowa", 299.00, "Akcesoria")
    )

    private var shouldFailNextCall = false // Flaga do testowania obsługi błędów

    override suspend fun getProducts(): Result<List<Product>> {
        delay(1200) // Symulacja opóźnienia sieciowego I/O
        return if (shouldFailNextCall) {
            shouldFailNextCall = false
            Result.failure(Exception("Błąd serwera HTTP 500: Połączenie odrzucone"))
        } else {
            Result.success(localProducts)
        }
    }

    override suspend fun addToCart(productId: String): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }

    fun toggleSimulatedError() {
        shouldFailNextCall = true
    }
}

// ============================================================================
// 2. KONTRAKT MVI: STAN, ZDARZENIA I EFEKTY (Slajdy 5.26, 5.30, 5.31)
// ============================================================================

// A. STAN EKRANU (Trwały, przeżywa obrót telefonu - Slajd 5.26)
sealed interface ProductsUiState {
    data object Loading : ProductsUiState
    data class Success(
        val products: List<Product>,
        val cartItemCount: Int = 0,
        val isRefreshing: Boolean = false
    ) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

// B. ZDARZENIA / INTENCJE UŻYTKOWNIKA (Płyną z UI w górę do ViewModelu - Slajd 5.30)
sealed interface ProductsUiEvent {
    data object OnRefresh : ProductsUiEvent
    data class OnAddToCart(val product: Product) : ProductsUiEvent
    data object OnSimulateErrorClick : ProductsUiEvent
}

// C. EFEKTY JEDNORAZOWE (Płyną z ViewModelu do UI przez Channel - Slajd 5.31)
sealed interface ProductsUiEffect {
    data class ShowSnackbar(val text: String) : ProductsUiEffect
    data class NavigateToDetails(val productId: String) : ProductsUiEffect
}

// ============================================================================
// 3. WARSTWA PREZENTACJI: VIEWMODEL (State Holder - Slajd 5.23 - 5.29)
// ============================================================================
class ProductsViewModel(
    private val repository: ProductRepository = FakeProductRepositoryImpl()
) : ViewModel() {

    // Prywatny mutowalny strumień stanu (enkapsulacja) + publiczny tylko do odczytu
    private val _uiState = MutableStateFlow<ProductsUiState>(ProductsUiState.Loading)
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    // Kanał na efekty jednorazowe (Channel zapobiega powtórzeniu efektu po obrocie ekranu!)
    private val _uiEffect = Channel<ProductsUiEffect>()
    val uiEffect: Flow<ProductsUiEffect> = _uiEffect.receiveAsFlow()

    private var cartCounter = 0

    init {
        loadData()
    }

    // Centralna funkcja obsługująca wszystkie akcje użytkownika (UDF)
    fun onEvent(event: ProductsUiEvent) {
        when (event) {
            is ProductsUiEvent.OnRefresh -> loadData(isSwipeToRefresh = true)
            is ProductsUiEvent.OnAddToCart -> handleAddToCart(event.product)
            is ProductsUiEvent.OnSimulateErrorClick -> {
                (repository as? FakeProductRepositoryImpl)?.toggleSimulatedError()
                loadData()
            }
        }
    }

    private fun loadData(isSwipeToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isSwipeToRefresh && _uiState.value is ProductsUiState.Success) {
                _uiState.value = (_uiState.value as ProductsUiState.Success).copy(isRefreshing = true)
            } else {
                _uiState.value = ProductsUiState.Loading
            }

            // Bezpieczne pobranie danych bez rzucania wyjątków (Result<T>)
            repository.getProducts()
                .onSuccess { productList ->
                    _uiState.value = ProductsUiState.Success(
                        products = productList,
                        cartItemCount = cartCounter,
                        isRefreshing = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = ProductsUiState.Error(
                        message = exception.message ?: "Wystąpił nieoczekiwany błąd"
                    )
                    _uiEffect.send(ProductsUiEffect.ShowSnackbar("Nie udało się pobrać danych!"))
                }
        }
    }

    private fun handleAddToCart(product: Product) {
        viewModelScope.launch {
            repository.addToCart(product.id)
            cartCounter++

            // Aktualizacja trwałego stanu (zwiększenie licznika w koszyku)
            val currentState = _uiState.value
            if (currentState is ProductsUiState.Success) {
                _uiState.value = currentState.copy(cartItemCount = cartCounter)
            }

            // Wysłanie efektu jednorazowego (Snackbar)
            _uiEffect.send(ProductsUiEffect.ShowSnackbar("Dodano '${product.title}' do koszyka!"))
        }
    }
}

// ============================================================================
// 4. WARSTWA WIDOKU: JETPACK COMPOSE (Slajd 5.44, 5.45)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = remember { ProductsViewModel() }
) {
    // collectAsStateWithLifecycle automatycznie wstrzymuje nasłuch, gdy aplikacja jest w tle! (Slajd 5.44)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Obsługa efektów ubocznych z ViewModelu (Slajd 5.31)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProductsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.text)
                }
                is ProductsUiEffect.NavigateToDetails -> {
                    println("Nawigacja do szczegółów ID: ${effect.productId}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Architektura MVI / Clean - W05") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ProductsUiEvent.OnSimulateErrorClick) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Wymuś błąd API")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Renderowanie na podstawie stanu (Pattern Matching na sealed interface - Slajd 5.26)
            when (val s = state) {
                is ProductsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProductsUiState.Error -> {
                    ErrorContent(
                        errorMessage = s.message,
                        onRetry = { viewModel.onEvent(ProductsUiEvent.OnRefresh) }
                    )
                }
                is ProductsUiState.Success -> {
                    SuccessContent(
                        state = s,
                        onEvent = { event -> viewModel.onEvent(event) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// KOMPONENTY PODRZĘDNE (Czyste komponenty bezstanowe - Stateless)
// ============================================================================

@Composable
fun SuccessContent(
    state: ProductsUiState.Success,
    onEvent: (ProductsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Pasek koszyka
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Liczba pozycji w koszyku:", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("${state.cartItemCount}", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Lista produktów
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.products, key = { it.id }) { product ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${product.price} PLN", color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { onEvent(ProductsUiEvent.OnAddToCart(product)) }) {
                            Text("Kup")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚠️ Coś poszło nie tak!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Spróbuj ponownie")
        }
    }
}

// ============================================================================
// 5. PROFESJONALNY PODGLĄD W ANDROID STUDIO (@Preview)
// ============================================================================

// Podgląd 1: Zobacz od razu gotowy stan Sukcesu z danymi!
@Preview(showBackground = true, name = "1. Stan Sukcesu (Lista)")
@Composable
fun PreviewProductsSuccess() {
    MaterialTheme {
        SuccessContent(
            state = ProductsUiState.Success(
                products = listOf(
                    Product("p1", "MacBook Pro M3", 8999.00, "Laptopy"),
                    Product("p2", "Monitor Dell 27' 4K", 2199.00, "Monitory"),
                    Product("p3", "Klawiatura Mechaniczna", 450.00, "Akcesoria")
                ),
                cartItemCount = 3
            ),
            onEvent = {}
        )
    }
}

// Podgląd 2: Zobacz jak wygląda stan Błędu (z przyciskiem retry)
@Preview(showBackground = true, name = "2. Stan Błędu")
@Composable
fun PreviewProductsError() {
    MaterialTheme {
        ErrorContent(
            errorMessage = "Błąd połączenia z serwerem HTTP 500",
            onRetry = {}
        )
    }
}

// Podgląd 3: Sam kręciołek
@Preview(showBackground = true, name = "3. Stan Ładowania")
@Composable
fun PreviewProductsLoading() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
// Podgląd 4: W PEŁNI INTERAKTYWNY KOSZYK W PODGLĄDZIE!
@Preview(showBackground = true, name = "4. Interaktywny Koszyk (Klikalny)")
@Composable
fun PreviewInteractiveCart() {
    // Lokalny stan koszyka tylko na potrzeby podglądu
    var fakeCartCount by remember { mutableStateOf(0) }

    MaterialTheme {
        SuccessContent(
            state = ProductsUiState.Success(
                products = listOf(
                    Product("p1", "MacBook Pro M3", 8999.00, "Laptopy"),
                    Product("p2", "Monitor Dell 27' 4K", 2199.00, "Monitory"),
                    Product("p3", "Klawiatura Mechaniczna", 450.00, "Akcesoria")
                ),
                cartItemCount = fakeCartCount // Przekazujemy aktualny stan
            ),
            onEvent = { event ->
                // Reagujemy na zdarzenie w podglądzie!
                if (event is ProductsUiEvent.OnAddToCart) {
                    fakeCartCount++
                }
            }
        )
    }
}