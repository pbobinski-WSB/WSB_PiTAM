package com.example.myapplication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProductScreenUiTestT {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldShowLoadingIndicatorWhenStateIsLoading() {
        composeRule.setContent {
            ProductScreenT(
                uiState = ProductsUiStateT.Loading,
                onRefreshClick = {}
            )
        }

        composeRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun shouldRenderErrorAndHandleRetryClick() {
        var refreshClicked = false

        composeRule.setContent {
            ProductScreenT(
                uiState = ProductsUiStateT.Error("Błąd serwera"),
                onRefreshClick = { refreshClicked = true }
            )
        }

        composeRule.onNodeWithText("Błąd serwera").assertIsDisplayed()
        composeRule.onNodeWithTag("retry_button").performClick()
        assertTrue("Callback odświeżania powinien zostać wywołany", refreshClicked)
    }

    @Test
    fun shouldDisplayProductsInListWhenSuccess() {
        val testProducts = listOf(
            ProductT("1", "Monitor 4K", 1499.0),
            ProductT("2", "Podkładka pod mysz", 49.0)
        )

        composeRule.setContent {
            ProductScreenT(
                uiState = ProductsUiStateT.Success(testProducts),
                onRefreshClick = {}
            )
        }

        composeRule.onNodeWithText("Monitor 4K").assertIsDisplayed()
        composeRule.onNodeWithText("1499.0 PLN").assertIsDisplayed()
        composeRule.onNodeWithText("Podkładka pod mysz").assertIsDisplayed()
    }
}