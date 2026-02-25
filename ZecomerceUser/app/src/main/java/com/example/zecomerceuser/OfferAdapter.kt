package com.example.zecomerceuser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class OfferAdapter(
    private val offers: List<Offer>,
    private val onClick: (Offer) -> Unit
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {

    inner class OfferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgOffer: ImageView = itemView.findViewById(R.id.imgOffer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun getItemCount(): Int = offers.size

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]

        Glide.with(holder.itemView.context)
            .load(offer.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(holder.imgOffer)

        holder.itemView.setOnClickListener {
            onClick(offer)
        }
    }
}
