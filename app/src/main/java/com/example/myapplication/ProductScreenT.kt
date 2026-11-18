package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ProductScreenT(
    uiState: ProductsUiStateT,
    onRefreshClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Text(
                text = "Katalog Produktów Demo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is ProductsUiStateT.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("loading_indicator")
                    )
                }
                is ProductsUiStateT.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("error_text")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRefreshClick,
                            modifier = Modifier.testTag("retry_button")
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
                is ProductsUiStateT.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("product_list")
                    ) {
                        items(uiState.products, key = { it.id }) { item ->
                            ProductRowT(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRowT(product: ProductT) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${product.price} PLN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}