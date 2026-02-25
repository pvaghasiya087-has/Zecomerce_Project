package com.example.zecomerceuser

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ProductsDetailsActivity : BaseActivity() {

    private val reviews = mutableListOf<Review>()
    private lateinit var reviewAdapter: ReviewAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var reviewsListener: ListenerRegistration? = null
    private var productListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_details)

        val productId = intent.getStringExtra("productId") ?: run {
            finish()
            return
        }

        val image = findViewById<ImageView>(R.id.detailImage)
        val name = findViewById<TextView>(R.id.detailName)
        val price = findViewById<TextView>(R.id.detailPrice)
        val ratingBar = findViewById<RatingBar>(R.id.detailRatingBar)
        val reviewCountText = findViewById<TextView>(R.id.detailReviewCount)
        val description = findViewById<TextView>(R.id.detailDescription)
        val btnReadReviews = findViewById<TextView>(R.id.btnReadReviews)
        val btnWriteReview = findViewById<Button>(R.id.btnWriteReview)
        val recycler = findViewById<RecyclerView>(R.id.recyclerReviews)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        recycler.layoutManager = LinearLayoutManager(this)
        reviewAdapter = ReviewAdapter(productId, reviews)
        recycler.adapter = reviewAdapter
        recycler.visibility = View.GONE

        // Listen to product itself
        productListener = db.collection("products").document(productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot == null || !snapshot.exists() || snapshot.getBoolean("isActive") == false) {
                    Toast.makeText(this, "This product is no longer available", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val product = snapshot.toObject(Product::class.java) ?: return@addSnapshotListener
                    name.text = product.name
                    price.text = "₹${product.price}"
                    description.text = product.description
                    Glide.with(this).load(product.imageUrl).into(image)
                }
            }

        // Listen to reviews in real-time
        reviewsListener = db.collection("products").document(productId)
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                reviews.clear()
                var totalRating = 0f
                for (doc in snapshot) {
                    val review = doc.toObject(Review::class.java)
                    review.id = doc.id
                    reviews.add(review)
                    totalRating += review.rating
                }

                reviewAdapter.notifyDataSetChanged()

                val count = snapshot.size()
                ratingBar.rating = if (count > 0) totalRating / count else 0f
                reviewCountText.text = "($count reviews)"
            }

        btnReadReviews.setOnClickListener {
            recycler.visibility = if (recycler.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnWriteReview.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Please login to write a review", Toast.LENGTH_SHORT).show()
            } else {
                showAddReviewDialog(productId)
            }
        }
    }

    private fun showAddReviewDialog(productId: String) {
        val view = layoutInflater.inflate(R.layout.dialog_add_review, null)
        val edtReview = view.findViewById<EditText>(R.id.edtReview)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBarAddReview)
        val tvError = view.findViewById<TextView>(R.id.tvErrorDialog)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Write Review")
            .setView(view)
            .setPositiveButton("Post", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val text = edtReview.text.toString().trim()
                val rating = ratingBar.rating
                when {
                    text.isEmpty() -> {
                        tvError.text = "Review cannot be empty"
                        tvError.visibility = View.VISIBLE
                    }
                    rating < 1f -> {
                        tvError.text = "Please give at least 1 star"
                        tvError.visibility = View.VISIBLE
                    }
                    else -> {
                        tvError.visibility = View.GONE
                        saveReview(productId, text, rating)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun saveReview(productId: String, message: String, rating: Float) {
        val user = auth.currentUser ?: return

        val review = Review(
            id = "",
            userId = user.uid,
            username = user.email ?: "User",
            message = message,
            timestamp = System.currentTimeMillis(),
            rating = rating,
            likes = 0,
            likedBy = mutableListOf()
        )

        db.collection("products").document(productId)
            .collection("reviews")
            .add(review)
            .addOnSuccessListener {
                updateProductRating(productId)
            }
    }

    private fun updateProductRating(productId: String) {
        db.collection("products").document(productId)
            .collection("reviews")
            .get()
            .addOnSuccessListener { snapshot ->
                var totalRating = 0f
                for (doc in snapshot) totalRating += doc.getDouble("rating")?.toFloat() ?: 0f
                val count = snapshot.size()
                val avg = if (count > 0) totalRating / count else 0f
                db.collection("products").document(productId)
                    .update("avgRating", avg, "reviewCount", count)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        reviewsListener?.remove()
        productListener?.remove()
    }
}
