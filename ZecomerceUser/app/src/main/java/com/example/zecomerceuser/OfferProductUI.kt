package com.example.zecomerceuser

data class OfferProductUI(
    val productId: String,
    val name: String,
    val imageUrl: String,
    val avgRating: Float,
    val reviewCount: Long,
    val originalPrice: String,
    val offerPrice: String,
    val description: String,
    var category: String = "",
    var stock: Int = 0
)

