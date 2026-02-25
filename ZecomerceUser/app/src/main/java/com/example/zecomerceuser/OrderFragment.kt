package com.example.zecomerceuser

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class OrderFragment : BaseFragment() {

    private val orders = mutableListOf<Order>()
    private val allOrders = mutableListOf<Order>()
    private var currentFilter: String = "All"
    private val displayedOrders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var btnClearHistory: Button
    private lateinit var loader: View
    private lateinit var contentLayout: View
    private lateinit var emptyContainer: LinearLayout
    private lateinit var rv: RecyclerView

    private var orderListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_order, container, false)

        emptyContainer = view.findViewById(R.id.emptyContainer)
        loader = view.findViewById(R.id.orderLoader)
        contentLayout = view.findViewById(R.id.orderContentLayout)



        loader.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE

        val btnAll: Button = view.findViewById(R.id.btnAll)
        val btnPlaced: Button = view.findViewById(R.id.btnPlaced)
        val btnPending: Button = view.findViewById(R.id.btnPending)
        val btnCompleted: Button = view.findViewById(R.id.btnCompleted)
        val btnRejected: Button = view.findViewById(R.id.btnRejected)
        val btnCancelled:Button=view.findViewById(R.id.btnCancelled)

        fun selectTab(selected: Button) {
            btnAll.isSelected = false
            btnPlaced.isSelected = false
            btnPending.isSelected = false
            btnCompleted.isSelected = false
            btnRejected.isSelected = false
            btnCancelled.isSelected= false
            selected.isSelected = true
        }

        btnAll.isSelected = true

        btnAll.setOnClickListener {
            selectTab(btnAll)
            currentFilter = "All"
            filterOrders(currentFilter) }
        btnPlaced.setOnClickListener {
            selectTab(btnPlaced)
            currentFilter = "Placed"
            filterOrders(currentFilter) }
        btnPending.setOnClickListener {
            selectTab(btnPending)
            currentFilter = "Pending"
            filterOrders(currentFilter) }
        btnCompleted.setOnClickListener {
            selectTab(btnCompleted)
            currentFilter = "Completed"
            filterOrders(currentFilter) }
        btnRejected.setOnClickListener {
            selectTab(btnRejected)
            currentFilter = "Rejected"
            filterOrders(currentFilter) }
        btnCancelled.setOnClickListener{
            selectTab(btnCancelled)
            currentFilter="Cancelled"
            filterOrders(currentFilter)
        }



        rv = view.findViewById(R.id.ordersRecyclerView)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrderAdapter(
            displayedOrders,
            onCancelClick = { cancelOrder(it) },
            onOrderClick = { openOrderDetails(it) }
        )
        rv.adapter = adapter

        btnClearHistory = view.findViewById(R.id.btnClearHistory)
        btnClearHistory.setOnClickListener { clearOrderHistory() }

        loadOrders()
        return view
    }

    private fun loadOrders() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            loader.visibility = View.GONE
            showEmpty()
            return
        }

        orderListener?.remove()
        orderListener = db.collection("orders")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                loader.visibility = View.GONE
                if (error != null) {
                    Log.e("Orders", "Firestore listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    orders.clear()
                    allOrders.clear()
                    adapter.notifyDataSetChanged()
                    showEmpty()
                    return@addSnapshotListener
                }
                orders.clear()
                allOrders.clear()


                for (change in snapshot.documentChanges) {
                    val order = change.document.toObject(Order::class.java).apply {
                        orderId = change.document.id
                    }

                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                           // if (order.isVisible) {
                                orders.add(order)
                                allOrders.add(order)
                                adapter.notifyItemInserted(orders.size - 1)
                            //}
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val index = orders.indexOfFirst { it.orderId == order.orderId }
                            if (index != -1) {
                                orders[index] = order // Update orders list
                                allOrders[index] = order
                                adapter.notifyItemChanged(index) // refresh only this item
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val index = orders.indexOfFirst { it.orderId == order.orderId }
                            if (index != -1) {
                                orders.removeAt(index)
                                allOrders.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                        }
                    }
                }
                filterOrders(currentFilter)

                if (orders.isEmpty()) showEmpty()
                else {
                    contentLayout.visibility = View.VISIBLE
                    emptyContainer.visibility = View.GONE
                }
            }
    }

    private fun cancelOrder(order: Order) {
        Log.d("Orders", "Attempting to cancel order with ID: ${order.orderId} and status: ${order.status}")

        if (order.status != "Pending" && order.status != "PLACED") {
            Log.d("Orders", "Order cannot be cancelled. Status is ${order.status}")
            return
        }

        val orderRef = db.collection("orders").document(order.orderId)
        val uid = auth.currentUser?.uid ?: return
        val adminRef = db.collection("userdetail")
            .document("ua2KE2nxZSfc0H1MHNs2nc1SnCl1") // Admin ID

        db.runTransaction { transaction ->
            // ---------- READ ALL DATA FIRST ----------

            // Get order snapshot
            val orderSnap = transaction.get(orderRef)
            val currentStatus = orderSnap.getString("status")
            Log.d("Orders", "Current status from Firestore: $currentStatus")

            if (currentStatus != "Pending" && currentStatus != "PLACED") {
                throw Exception("Order cannot be cancelled. Status is $currentStatus")
            }

            // ---------- RESTORE STOCK ----------
            val items = orderSnap.get("items") as? List<Map<String, Any>> ?: emptyList()
            val productUpdates = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

            for (item in items) {
                val productId = item["productId"] as? String ?: continue
                val quantity = (item["quantity"] as? Long ?: 0L).toInt()

                val productRef = db.collection("products").document(productId)
                val productSnap = transaction.get(productRef)
                val currentStock = (productSnap.getLong("stock") ?: 0L).toInt()

                productUpdates.add(productRef to mapOf("stock" to currentStock + quantity))
            }

            // ---------- UPDATE BALANCES ----------
            val totalAmount = orderSnap.getDouble("totalAmount") ?: 0.0

            val userRef = db.collection("userdetail").document(uid)
            val userSnap = transaction.get(userRef)
            val userBalance = userSnap.getDouble("balance") ?: 0.0

            val adminSnap = transaction.get(adminRef)
            val adminBalance = adminSnap.getDouble("balance") ?: 0.0

            // Now update all the documents at once
            productUpdates.forEach { (productRef, updateMap) ->
                transaction.update(productRef, updateMap)
            }

            transaction.update(userRef, "balance", userBalance + totalAmount)
            transaction.update(adminRef, "balance", adminBalance - totalAmount)

            // ---------- UPDATE ORDER STATUS ----------
            transaction.update(orderRef, "status", "Cancelled")

        }.addOnSuccessListener {
            Log.d("Orders", "Order cancelled and balances updated successfully.")
            val index = orders.indexOfFirst { it.orderId == order.orderId }
            if (index != -1) {
                orders[index] = order.copy(status = "Cancelled") // Update the order status locally
                adapter.notifyItemChanged(index)
            }
            filterOrders(currentFilter)
        }.addOnFailureListener { e ->
            Log.e("Orders", "Failed to cancel order", e)
        }
    }

    private fun clearOrderHistory() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val status = doc.getString("status")
                    if (status == "Completed" || status == "Cancelled") {
                        doc.reference.update("isVisible", false)
                    }
                }
            }
    }

    private fun openOrderDetails(order: Order) {
        val intent = Intent(requireContext(), OrderDetailsActivity::class.java)
        intent.putExtra("orderId", order.orderId)
        startActivity(intent)
    }

    private fun showEmpty() {
        emptyContainer.visibility = View.VISIBLE
        rv.visibility = View.GONE
    }

    private fun filterOrders(status: String) {
        val filtered = if (status == "All") {
            allOrders
        } else {
            allOrders.filter { it.status.equals(status, ignoreCase = true) }
        }

        displayedOrders.clear()
        displayedOrders.addAll(filtered)

        if (displayedOrders.isEmpty()) {
            emptyContainer.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            emptyContainer.visibility = View.GONE
            rv.visibility = View.VISIBLE

        }
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orderListener?.remove()
    }
}
