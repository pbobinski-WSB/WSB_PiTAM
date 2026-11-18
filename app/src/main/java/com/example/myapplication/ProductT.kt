package com.example.myapplication

data class ProductT(
    val id: String,
    val name: String,
    val price: Double
)

interface ProductRepositoryT {
    suspend fun fetchProducts(): Result<List<ProductT>>
}