package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProductsUiStateT {
    object Loading : ProductsUiStateT
    data class Success(val products: List<ProductT>) : ProductsUiStateT
    data class Error(val message: String) : ProductsUiStateT
}

class ProductViewModelT(
    private val repository: ProductRepositoryT
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductsUiStateT>(ProductsUiStateT.Loading)
    val uiState: StateFlow<ProductsUiStateT> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = ProductsUiStateT.Loading
        viewModelScope.launch {
            repository.fetchProducts()
                .onSuccess { items ->
                    _uiState.value = ProductsUiStateT.Success(items)
                }
                .onFailure {
                    _uiState.value = ProductsUiStateT.Error("Błąd pobierania danych")
                }
        }
    }
}