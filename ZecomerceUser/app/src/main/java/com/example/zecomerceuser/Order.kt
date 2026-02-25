package com.example.zecomerceuser

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
    var totalAmount: Double = 0.0,
    var paymentMode: String = "",
    var paymentStatus: String = "",
    var isVisible: Boolean = true     // for soft-clear
)
