package com.example.zecomerce

data class Order(
    var orderId: String = "",
    var productName: String = "",
    var quantity: Int = 1,
    var items: List<CartItem> = listOf(),
    var totalPrice: String = "",
    var customerName: String = "",
    var status: String = "Pending",  // Pending, Completed, Cancelled
    var timestamp: Long = 0L,
    var userId: String = "",
    var address: String = "",
    var phone: String = "",
    var receiverName: String = "",
    var userEmail:String="",
    var isVisible: Boolean = true     // for soft-clear
)
