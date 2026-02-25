package com.example.zecomerce

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Offer(
    var id: String = "",                  // Firestore document ID
    var title: String = "",               // Offer title
    var imageUrl: String = "",            // Banner image URL
    var isActive: Boolean = true,         // Active flag
    var products: List<OfferProduct> = listOf()  // Products in this offer
)

@IgnoreExtraProperties
data class OfferProduct(
    var productId: String = "",           // Product ID
    var productName: String = "", // Product name
    var imageUrl: String = "",
    var originalPrice: String = "",       // Original price as String
    var offerPrice: String = ""           // Offer price as String
)

