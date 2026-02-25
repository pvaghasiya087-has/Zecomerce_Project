package com.example.zecomerce

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class OfferAdminAdapter(
    private val offerList: List<Offer>,
    private val onEdit: (Offer) -> Unit,
    private val onDelete: (Offer) -> Unit
) : RecyclerView.Adapter<OfferAdminAdapter.OfferViewHolder>() {

    inner class OfferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgOffer: ImageView = itemView.findViewById(R.id.imgOffer)
        val tvTitle: TextView = itemView.findViewById(R.id.tvOfferTitle)
        val tvProductCount: TextView = itemView.findViewById(R.id.tvProductCount)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun getItemCount(): Int = offerList.size

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offerList[position]

        Glide.with(holder.itemView.context)
            .load(offer.imageUrl)
            .placeholder(R.drawable.load_image)
            .into(holder.imgOffer)

        holder.tvTitle.text = offer.title
        holder.tvProductCount.text = "${offer.products.size} product(s)"

        holder.btnEdit.setOnClickListener { onEdit(offer) }
        holder.btnDelete.setOnClickListener { onDelete(offer) }
    }
}
