package com.example.myapplication

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ProductRepository = mockk()

    @Test
    fun `emits Loading first, then Success on repository success`() = runTest {
        // 1. GIVEN (Konfiguracja mocka z użyciem MockK)
        val dummyData = listOf(
            Product("1", "Klawiatura Mechaniczna", 350.0),
            Product("2", "Mysz Bezprzewodowa", 180.0)
        )
        coEvery { repository.fetchProducts() } returns Result.success(dummyData)

        // 2. WHEN & THEN (Weryfikacja strumienia z Turbine)
        val viewModel = ProductViewModel(repository)

        viewModel.uiState.test {
            // Pierwsza emisja natychmiastowa
            assertEquals(ProductsUiState.Loading, awaitItem())

            // Przewijamy czas koprocedury do zakończenia
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            // Druga emisja po powrocie z mocka
            val successState = awaitItem()
            assertTrue(successState is ProductsUiState.Success)
            assertEquals(2, (successState as ProductsUiState.Success).products.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error state when repository throws exception`() = runTest {
        coEvery { repository.fetchProducts() } returns Result.failure(RuntimeException("Network down"))

        val viewModel = ProductViewModel(repository)

        viewModel.uiState.test {
            assertEquals(ProductsUiState.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val errorState = awaitItem()
            assertTrue(errorState is ProductsUiState.Error)
            assertEquals("Błąd pobierania danych", (errorState as ProductsUiState.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }
}