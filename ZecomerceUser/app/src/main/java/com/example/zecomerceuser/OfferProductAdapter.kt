package com.example.zecomerceuser

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class OfferProductAdapter(
    private val offerId: String,
    private val list: List<OfferProductUI>,
    private val onAddToCart: (OfferProductUI) -> Unit
) : RecyclerView.Adapter<OfferProductAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgProduct)
        val name: TextView = view.findViewById(R.id.txtName)
        val rating: RatingBar = view.findViewById(R.id.ratingBar)
        val original: TextView = view.findViewById(R.id.txtOriginal)
        val offer: TextView = view.findViewById(R.id.txtOffer)
        val description: TextView = view.findViewById(R.id.txtDescription)
        val btn: Button = view.findViewById(R.id.btnAdd)
        val txtStock: TextView = itemView.findViewById(R.id.txtStock)
        val txtReviews: TextView = itemView.findViewById(R.id.txtReviews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer_product, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        Glide.with(holder.itemView.context).load(item.imageUrl).into(holder.img)
        holder.name.text = item.name
        holder.description.text = item.description
        holder.rating.rating = item.avgRating
        holder.original.text = "₹${item.originalPrice}"
        holder.original.paintFlags = holder.original.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        holder.offer.text = "₹${item.offerPrice}"
        val stock = item.stock

        when {
            stock <= 0 -> {
                holder.txtStock.text = "Out of Stock"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
                )
            }

            stock in 1..10 -> {
                holder.txtStock.text = "Only $stock items left"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_orange_dark)
                )
            }

            else -> {
                holder.txtStock.text = "In Stock ($stock available)"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_green_dark)
                )
            }
        }
        val reviewCount = item.reviewCount
        holder.txtReviews.text = "($reviewCount reviews)"

        holder.btn.setOnClickListener { onAddToCart(item) }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, OfferProductDetailsActivity::class.java)
            intent.putExtra("offerId", offerId)       // Firestore document id
            intent.putExtra("productId", item.productId) // Selected product id
            intent.putExtra("originalPrice",item.originalPrice)
            intent.putExtra("offerPrice",item.offerPrice)
            context.startActivity(intent)
        }


    }
}
