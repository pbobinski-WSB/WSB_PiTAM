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
class ProductViewModelTestT {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRuleT()

    private val repository: ProductRepositoryT = mockk()

    @Test
    fun emits_Loading_first_then_Success_on_repository_success() = runTest {
        // GIVEN
        val dummyData = listOf(
            ProductT("1", "Klawiatura Mechaniczna", 350.0),
            ProductT("2", "Mysz Bezprzewodowa", 180.0)
        )
        coEvery { repository.fetchProducts() } returns Result.success(dummyData)

        // WHEN & THEN
        val viewModel = ProductViewModelT(repository)

        viewModel.uiState.test {
            // Pierwsza emisja
            assertEquals(ProductsUiStateT.Loading, awaitItem())

            // Zakończ pracę koprocedury
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            // Druga emisja po pobraniu danych
            val successState = awaitItem()
            assertTrue(successState is ProductsUiStateT.Success)
            assertEquals(2, (successState as ProductsUiStateT.Success).products.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emits_Error_state_when_repository_fails() = runTest {
        // GIVEN
        coEvery { repository.fetchProducts() } returns Result.failure(RuntimeException("Brak sieci"))

        // WHEN & THEN
        val viewModel = ProductViewModelT(repository)

        viewModel.uiState.test {
            assertEquals(ProductsUiStateT.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val errorState = awaitItem()
            assertTrue(errorState is ProductsUiStateT.Error)
            assertEquals("Błąd pobierania danych", (errorState as ProductsUiStateT.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }
}