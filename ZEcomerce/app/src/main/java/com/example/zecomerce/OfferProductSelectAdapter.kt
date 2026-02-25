package com.example.zecomerce

import android.text.Editable
import android.content.Context
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class OfferProductSelectAdapter(
    private val context: Context,
    private val allProducts: List<Product>,
    selectedOfferProducts: List<OfferProduct>
) : RecyclerView.Adapter<OfferProductSelectAdapter.ProductViewHolder>() {

    private val offerProducts = mutableListOf<OfferProduct>()

    init {
        // Pre-fill selected products
        selectedOfferProducts.forEach { selected ->
            val product = allProducts.find { it.id == selected.productId }
            if (product != null) {
                offerProducts.add(selected.copy())
            }
        }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chkSelect: CheckBox = itemView.findViewById(R.id.chkSelect)
        val productImage: ImageView=itemView.findViewById(R.id.productImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvOriginalPrice: TextView = itemView.findViewById(R.id.tvOriginalPrice)
        val edtOfferPrice: EditText = itemView.findViewById(R.id.edtOfferPrice)
        var textWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer_product_select, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = allProducts.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = allProducts[position]

        if (product.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(product.imageUrl)
                .into(holder.productImage)
        } else {
            holder.productImage.setImageResource(R.drawable.load_image)
        }
        holder.tvProductName.text = product.name
        holder.tvOriginalPrice.text = "₹${product.price}"

        // Remove previous TextWatcher
        holder.textWatcher?.let { holder.edtOfferPrice.removeTextChangedListener(it) }

        val selected = offerProducts.find { it.productId == product.id }
        holder.chkSelect.isChecked = selected != null
        holder.edtOfferPrice.setText(selected?.offerPrice ?: "")
        holder.edtOfferPrice.isEnabled = selected != null

        // Checkbox listener
        holder.chkSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (offerProducts.none { it.productId == product.id }) {
                    val offerPrice = holder.edtOfferPrice.text.toString()
                    offerProducts.add(OfferProduct(product.id, product.name, product.price, offerPrice))
                }
                holder.edtOfferPrice.isEnabled = true
            } else {
                offerProducts.removeAll { it.productId == product.id }
                holder.edtOfferPrice.setText("")
                holder.edtOfferPrice.isEnabled = false
            }
        }

        // TextWatcher for offer price changes
        holder.textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val price = s?.toString() ?: ""
                offerProducts.find { it.productId == product.id }?.offerPrice = price
            }
        }
        holder.edtOfferPrice.addTextChangedListener(holder.textWatcher)
    }

    fun getSelectedProducts(): List<OfferProduct> =
        offerProducts.filter { it.offerPrice.isNotEmpty() }
}
