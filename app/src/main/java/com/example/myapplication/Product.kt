package com.example.myapplication


import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep // Zabezpieczenie przed usunięciem pól przez R8
@Serializable
data class Product(
    val id: String,
    val name: String,
    val price: Double
)

interface ProductRepository {
    suspend fun fetchProducts(): Result<List<Product>>
}