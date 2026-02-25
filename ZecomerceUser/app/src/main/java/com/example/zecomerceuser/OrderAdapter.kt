package com.example.zecomerceuser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    val orders: MutableList<Order>,
    private val onCancelClick: (Order) -> Unit,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvReceiverName: TextView = view.findViewById(R.id.tvReceiverName)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvTotalPrice)
        val tvUserId: TextView = view.findViewById(R.id.tvUserId)
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvReceiverName.text = "Receiver: ${order.receiverName}"
        holder.tvAddress.text = "Address: ${order.address}"
        holder.tvProductName.text = "Products: ${order.productName}"
        holder.tvTotalPrice.text = "Total Amount: ₹${order.totalPrice}"
        holder.tvUserId.text = "User ID: ${order.userId}"
        holder.tvOrderId.text = "Order Id: ${order.orderId}"

        holder.tvStatus.text = order.status.uppercase(Locale.getDefault())
        // Set text color and background based on status
        when (order.status) {
            "Cancelled" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.WHITE)
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled)
            }
            "Pending" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.BLACK)
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
            "Completed" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.WHITE)
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_complete)
            }
            else -> {
                holder.tvStatus.setTextColor(android.graphics.Color.BLACK)
                holder.tvStatus.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }
        holder.btnCancel.visibility =
            if (order.status == "Pending"||order.status == "PLACED") View.VISIBLE else View.GONE
        holder.itemView.alpha = if (order.status == "Cancelled") 0.5f else 1f

        if (order.timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
            holder.tvTime.text = "Ordered on: ${sdf.format(Date(order.timestamp))}"
        } else {
            holder.tvTime.text = "Ordered on: N/A"
        }

        holder.btnCancel.setOnClickListener { onCancelClick(order) }
        holder.itemView.setOnClickListener { onOrderClick(order) }
    }

    override fun getItemCount(): Int = orders.size
}
