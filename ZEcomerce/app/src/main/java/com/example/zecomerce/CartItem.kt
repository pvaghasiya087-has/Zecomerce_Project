package com.example.zecomerce

data class CartItem(
    var productId: String = "",
    var name: String = "",
    var price: String = "",
    var imageUrl: String = "",
    var quantity: Int = 1
)
