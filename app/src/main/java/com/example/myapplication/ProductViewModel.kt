package com.example.myapplication


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProductsUiState {
    object Loading : ProductsUiState
    data class Success(val products: List<Product>) : ProductsUiState
    data class Error(val message: String) : ProductsUiState
}

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductsUiState>(ProductsUiState.Loading)
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = ProductsUiState.Loading
        viewModelScope.launch {
            repository.fetchProducts()
                .onSuccess { items ->
                    _uiState.value = ProductsUiState.Success(items)
                }
                .onFailure {
                    _uiState.value = ProductsUiState.Error("Błąd pobierania danych")
                }
        }
    }
}