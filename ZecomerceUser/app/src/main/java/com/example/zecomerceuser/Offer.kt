package com.example.zecomerceuser

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Offer(
    var id: String = "",
    var title: String = "",
    var imageUrl: String = "",
    var productId: String? = null,
    var isActive: Boolean = true,
    var products: List<Map<String, Any>> = listOf()
)

