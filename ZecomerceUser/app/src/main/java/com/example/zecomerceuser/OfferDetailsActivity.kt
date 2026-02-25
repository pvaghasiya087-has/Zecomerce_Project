package com.example.zecomerceuser

import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.RatingBar

class OfferDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val productList = mutableListOf<OfferProductUI>()
    private lateinit var adapter: OfferProductAdapter

    private lateinit var imgOffer: ImageView
    private lateinit var txtTitle: TextView
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offer_details)

        imgOffer = findViewById(R.id.imgOffer)
        txtTitle = findViewById(R.id.txtTitle)
        recycler = findViewById(R.id.recyclerOfferProducts)
        val offerId = intent.getStringExtra("offerId") ?: return
        adapter = OfferProductAdapter(offerId,productList) { addToCart(it) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter


        loadOfferProducts(offerId)
    }

    private fun loadOfferProducts(offerId: String) {
        db.collection("offers").document(offerId).get()
            .addOnSuccessListener { offerDoc ->

                val offerTitle = offerDoc.getString("title") ?: ""
                val offerImage = offerDoc.getString("imageUrl") ?: ""

                txtTitle.text = offerTitle
                Glide.with(this).load(offerImage).into(imgOffer)

                val products = offerDoc.get("products") as? List<Map<String, Any>> ?: return@addOnSuccessListener

                for (p in products) {
                    val productId = p["productId"].toString()
                    val offerPrice = p["offerPrice"].toString()
                    val originalPrice = p["originalPrice"].toString()

                    db.collection("products").document(productId).get()
                        .addOnSuccessListener { productDoc ->
                            val product = productDoc.toObject(Product::class.java) ?: return@addOnSuccessListener

                            productList.add(
                                OfferProductUI(
                                    productId = productId,
                                    name = product.name,
                                    imageUrl = product.imageUrl,
                                    avgRating = product.avgRating.toFloat(),
                                    reviewCount = product.reviewCount,
                                    originalPrice = originalPrice,
                                    offerPrice = offerPrice,
                                    description = product.description,
                                    category = product.category,
                                    stock = product.stock
                                )
                            )
                            adapter.notifyDataSetChanged()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load offer", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addToCart(item: OfferProductUI) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val cartId = if (item.offerPrice != item.originalPrice) {
            "${item.productId}_offer"
        } else {
            "${item.productId}_normal"
        }

        val cartRef = db.collection("users")
            .document(userId)
            .collection("cart")
            .document(cartId)

        cartRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val currentQty = doc.getLong("quantity") ?: 1
                cartRef.update("quantity", currentQty + 1)
            } else {
                val cartItem = hashMapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "price" to item.offerPrice,
                    "originalPrice" to item.originalPrice,
                    "imageUrl" to item.imageUrl,
                    "quantity" to 1,
                    "isOffer" to (item.offerPrice != item.originalPrice)
                )
                cartRef.set(cartItem)
            }
            Toast.makeText(this, "${item.name} added to cart", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to add cart", Toast.LENGTH_SHORT).show()
        }
    }
}
