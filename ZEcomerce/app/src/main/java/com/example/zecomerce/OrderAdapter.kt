package com.example.zecomerce

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    val orders: MutableList<Order>,
    private val onOrderClick: (Order) -> Unit,
    private val onCompleteClick: (Order) -> Unit,
    private val onRejectClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvReceiverName: TextView = view.findViewById(R.id.tvReceiverName)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvTotalPrice)
        val tvUserId: TextView = view.findViewById(R.id.tvUserId)
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)

        val btnComplete: Button = view.findViewById(R.id.btnComplete)
        val btnReject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvReceiverName.text = order.receiverName
        holder.tvAddress.text = order.address
        holder.tvProductName.text = order.productName
        holder.tvTotalPrice.text = "₹${order.totalPrice}"
        holder.tvUserId.text = "User ID: ${order.userId}"
        holder.tvOrderId.text = "Order ID: ${order.orderId}"
        holder.tvStatus.text = order.status.uppercase()

        val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(order.timestamp))

        // Show buttons only for pending orders
        if (order.status == "Pending") {
            holder.btnComplete.visibility = View.VISIBLE
            holder.btnReject.visibility = View.VISIBLE
        } else {
            holder.btnComplete.visibility = View.GONE
            holder.btnReject.visibility = View.GONE
        }

        holder.btnComplete.setOnClickListener { onCompleteClick(order) }
        holder.btnReject.setOnClickListener { onRejectClick(order) }
        holder.itemView.setOnClickListener { onOrderClick(order) }
    }

    override fun getItemCount(): Int = orders.size
}
