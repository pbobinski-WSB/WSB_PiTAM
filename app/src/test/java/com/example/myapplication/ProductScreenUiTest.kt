package com.example.myapplication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class) // Dzięki temu test UI odpala się na JVM w 2 sekundy!
class ProductScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldShowLoadingIndicatorWhenStateIsLoading() {
        composeRule.setContent {
            ProductScreen(
                uiState = ProductsUiState.Loading,
                onRefreshClick = {}
            )
        }

        // Asercja: szukamy komponentu po testTagu
        composeRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun shouldRenderListAndTriggerRefreshButtonWhenError() {
        var refreshClicked = false

        composeRule.setContent {
            ProductScreen(
                uiState = ProductsUiState.Error("Awaria serwera"),
                onRefreshClick = { refreshClicked = true }
            )
        }

        // Asercja obecności tekstu błędu
        composeRule.onNodeWithText("Awaria serwera").assertIsDisplayed()

        // Kliknięcie w przycisk
        composeRule.onNodeWithTag("retry_button").performClick()
        assertTrue("Callback odświeżania powinien zostać wywołany", refreshClicked)
    }

    @Test
    fun shouldDisplayProductsInListWhenSuccess() {
        val testProducts = listOf(
            Product("1", "Monitor 4K", 1499.0),
            Product("2", "Podkładka pod mysz", 49.0)
        )

        composeRule.setContent {
            ProductScreen(
                uiState = ProductsUiState.Success(testProducts),
                onRefreshClick = {}
            )
        }

        // Weryfikacja węzłów semantycznych z tekstem
        composeRule.onNodeWithText("Monitor 4K").assertIsDisplayed()
        composeRule.onNodeWithText("1499.0 PLN").assertIsDisplayed()
        composeRule.onNodeWithText("Podkładka pod mysz").assertIsDisplayed()
    }
}