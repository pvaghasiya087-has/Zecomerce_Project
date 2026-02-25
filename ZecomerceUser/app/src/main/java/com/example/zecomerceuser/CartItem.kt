package com.example.zecomerceuser

data class CartItem(
    var docId: String = "",
    var productId: String = "",
    var name: String = "",
    var price: String = "", // final price user pays
    var originalPrice: String = "",  // original price (for strike-through if offer)
    var imageUrl: String = "",
    var quantity: Int = 1,
    var isOffer: Boolean = false     // true if added via an offer

)
