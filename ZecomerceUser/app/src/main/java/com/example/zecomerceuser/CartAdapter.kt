package com.example.zecomerceuser

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onUpdate: (CartItem) -> Unit,
    private val onDelete: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartItemName)
       // val price: TextView = view.findViewById(R.id.cartItemPrice)
        val quantity: TextView = view.findViewById(R.id.cartItemQuantity)
        val image: ImageView = view.findViewById(R.id.cartItemImage)
        val plus: ImageButton = view.findViewById(R.id.btnPlus)
        val minus: ImageButton = view.findViewById(R.id.btnMinus)
        val delete: ImageButton = view.findViewById(R.id.btnDelete)
        val originalPrice: TextView = view.findViewById(R.id.cartItemOriginalPrice)
        val offerPrice: TextView = view.findViewById(R.id.cartItemOfferPrice)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]

        holder.name.text = item.name
       // holder.price.text = item.price   // FIXED
        holder.quantity.text = item.quantity.toString()

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.image)

        if (item.isOffer) {
            holder.originalPrice.visibility = View.VISIBLE
            holder.originalPrice.paintFlags = holder.originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.originalPrice.text = "₹${item.originalPrice}"

            holder.offerPrice.text = "₹${item.price}"
        } else {
            holder.originalPrice.visibility = View.GONE
            holder.offerPrice.text = "₹${item.price}"
        }

        holder.plus.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                updateQuantity(pos, cartItems[pos].quantity + 1)
            }
        }


        holder.minus.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && cartItems[pos].quantity > 1) {
                updateQuantity(pos, cartItems[pos].quantity - 1)
            }
        }


        holder.delete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDelete(cartItems[pos])
            }
        }
    }

    private fun updateQuantity(position: Int, newQty: Int) {
        if (position == RecyclerView.NO_POSITION) return

        val item = cartItems[position]
        item.quantity = newQty

        notifyItemChanged(position)
        onUpdate(item)
    }

    override fun getItemCount(): Int = cartItems.size
}
