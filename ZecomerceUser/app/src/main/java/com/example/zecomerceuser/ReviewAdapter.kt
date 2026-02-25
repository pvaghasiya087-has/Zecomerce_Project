package com.example.zecomerceuser


import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(
    private val productId: String,
    private val reviews: MutableList<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewVH>() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class ReviewVH(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.txtUser)
        val message: TextView = view.findViewById(R.id.txtMessage)
        val date: TextView = view.findViewById(R.id.txtDate)
        val ratingBar: RatingBar = view.findViewById(R.id.reviewRatingBar)
        val likeBtn: ImageView = view.findViewById(R.id.btnLike)
        val likeCount: TextView = view.findViewById(R.id.txtLikes)
        val deleteBtn: ImageView = view.findViewById(R.id.btnDeleteReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewVH(view)
    }

    override fun getItemCount(): Int = reviews.size

    override fun onBindViewHolder(holder: ReviewVH, position: Int) {
        val review = reviews[position]

        holder.username.text = review.username
        holder.message.text = review.message
        holder.ratingBar.rating = review.rating
        holder.likeCount.text = review.likes.toString()

        // 📅 Date format
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date(review.timestamp))

        // ❤️ Like state
        val isLiked = currentUserId != null && review.likedBy.contains(currentUserId)
        holder.likeBtn.setImageResource(
            if (isLiked) R.drawable.ic_like_filled
            else R.drawable.ic_like_outline
        )

        holder.likeBtn.setOnClickListener {
            toggleLike(position)
        }

        // 🗑 Show delete only for own review
        holder.deleteBtn.visibility =
            if (review.userId == currentUserId) View.VISIBLE else View.GONE

        holder.deleteBtn.setOnClickListener {
            showDeleteDialog(holder.itemView, position)
        }
    }

    // ================= LIKE / UNLIKE =================
    private fun toggleLike(position: Int) {
        if (position == RecyclerView.NO_POSITION) return

        val uid = currentUserId ?: return
        val review = reviews[position]

        val ref = db.collection("products")
            .document(productId)
            .collection("reviews")
            .document(review.id)

        if (review.likedBy.contains(uid)) {
            review.likedBy.remove(uid)
            review.likes--
        } else {
            review.likedBy.add(uid)
            review.likes++
        }

        ref.update(
            mapOf(
                "likes" to review.likes,
                "likedBy" to review.likedBy
            )
        )

        notifyItemChanged(position)
    }

    // ================= DELETE WITH ALERT =================
    private fun showDeleteDialog(view: View, position: Int) {
        AlertDialog.Builder(view.context)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReview(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReview(position: Int) {
        if (position == RecyclerView.NO_POSITION) return

        val review = reviews[position]

        db.collection("products")
            .document(productId)
            .collection("reviews")
            .document(review.id)
            .delete()
            .addOnSuccessListener {
                reviews.removeAt(position)
                notifyItemRemoved(position)

                updateProductRating()

            }
    }
    private fun updateProductRating() {
        val productRef = db.collection("products").document(productId)

        productRef.collection("reviews")
            .get()
            .addOnSuccessListener { snapshot ->

                var totalRating = 0f
                for (doc in snapshot) {
                    totalRating += doc.getDouble("rating")?.toFloat() ?: 0f
                }

                val count = snapshot.size()
                val avg = if (count > 0) totalRating / count else 0f

                productRef.update(
                    mapOf(
                        "avgRating" to avg,
                        "reviewCount" to count
                    )
                )
            }
    }

}
