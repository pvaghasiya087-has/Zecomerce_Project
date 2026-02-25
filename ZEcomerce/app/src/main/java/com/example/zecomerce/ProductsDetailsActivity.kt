package com.example.zecomerce

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProductsDetailsActivity : BaseActivity() {

    private val reviews = mutableListOf<Review>()
    private lateinit var reviewAdapter: ReviewAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_details)

        val product = intent.getParcelableExtra<Product>("product") ?: run {
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

        // Product info
        name.text = product.name
        price.text = "₹${product.price}"
        description.text = product.description
        Glide.with(this).load(product.imageUrl).into(image)

        // RecyclerView setup
        recycler.layoutManager = LinearLayoutManager(this)
        reviewAdapter = ReviewAdapter(product.id, reviews)
        recycler.adapter = reviewAdapter
        recycler.visibility = View.GONE

        updateRatingUI(product.id, ratingBar, reviewCountText)

        btnReadReviews.setOnClickListener {
            recycler.visibility = if (recycler.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (recycler.visibility == View.VISIBLE) loadReviews(product.id)
        }

        btnWriteReview.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Please login to write a review", Toast.LENGTH_SHORT).show()
            } else {
                showAddReviewDialog(product.id)
            }
        }
    }

    private fun showAddReviewDialog(productId: String) {
        val view = layoutInflater.inflate(R.layout.dialog_add_review, null)
        val edtReview = view.findViewById<EditText>(R.id.edtReview)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBarAddReview)
        val tvError = view.findViewById<TextView>(R.id.tvErrorDialog) // Error TextView in red

        val dialog = AlertDialog.Builder(this)
            .setTitle("Write Review")
            .setView(view)
            .setPositiveButton("Post", null) // override later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val text = edtReview.text.toString().trim()
                val rating = ratingBar.rating

                // Validation
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
            username = user.email ?: "User", // store email as username
            message = message,
            timestamp = System.currentTimeMillis(),
            rating = rating,
            likes = 0,
            likedBy = mutableListOf()
        )

        db.collection("products")
            .document(productId)
            .collection("reviews")
            .add(review)
            .addOnSuccessListener {
                loadReviews(productId)
                updateRatingUI(
                    productId,
                    findViewById(R.id.detailRatingBar),
                    findViewById(R.id.detailReviewCount)
                )
            }
        updateProductRating(productId)
    }

    private fun loadReviews(productId: String) {
        db.collection("products")
            .document(productId)
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                reviews.clear()
                for (doc in snapshot) {
                    val review = doc.toObject(Review::class.java)
                    review.id = doc.id
                    reviews.add(review)
                }
                reviewAdapter.notifyDataSetChanged()
            }
    }

    private fun updateRatingUI(productId: String, ratingBar: RatingBar, reviewCountText: TextView) {
        db.collection("products")
            .document(productId)
            .collection("reviews")
            .get()
            .addOnSuccessListener { snapshot ->
                var total = 0f
                for (doc in snapshot) {
                    total += doc.getDouble("rating")?.toFloat() ?: 0f
                }
                val count = snapshot.size()
                ratingBar.rating = if (count > 0) total / count else 0f
                reviewCountText.text = "($count reviews)"
            }

    }
    private fun updateProductRating(productId: String) {
        db.collection("products")
            .document(productId)
            .collection("reviews")
            .get()
            .addOnSuccessListener { snapshot ->

                var totalRating = 0f
                for (doc in snapshot) {
                    totalRating += doc.getDouble("rating")?.toFloat() ?: 0f
                }

                val count = snapshot.size()
                val avg = if (count > 0) totalRating / count else 0f

                db.collection("products")
                    .document(productId)
                    .update(
                        "avgRating", avg,
                        "reviewCount", count
                    )
            }
    }

}
