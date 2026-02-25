package com.example.zecomerceuser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class OrderItemAdapter(
    private val items: List<CartItem>
) : RecyclerView.Adapter<OrderItemAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartItemName)
        val qty: TextView = view.findViewById(R.id.cartItemQuantity)
        val price: TextView = view.findViewById(R.id.cartItemPrice)
        val image: ImageView = view.findViewById(R.id.cartItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_readonly, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.qty.text = "Qty: ${item.quantity}"
        holder.price.text = "₹${item.price}"

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_profile)
            .into(holder.image)
    }

    override fun getItemCount(): Int = items.size
}
