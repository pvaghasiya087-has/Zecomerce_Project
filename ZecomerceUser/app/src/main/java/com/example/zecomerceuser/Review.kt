package com.example.zecomerceuser


data class Review(
    var id: String = "",
    var userId: String = "",
    var username: String = "",
    var message: String = "",
    var timestamp: Long = 0L,
    var rating: Float = 0f,
    var likes: Int = 0,
    var likedBy: MutableList<String> = mutableListOf()
)
