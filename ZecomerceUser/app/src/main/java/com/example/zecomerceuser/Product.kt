package com.example.zecomerceuser


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    var id: String = "",
    var name: String = "",
    var price: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var isActive:Boolean?= null,

    // user-generated
    var avgRating: Double = 0.0,
    var reviewCount: Long = 0,
    var category: String = "",
    var stock: Int = 0


) : Parcelable
