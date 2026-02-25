package com.example.zecomerce

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OrderFragment : BaseFragment() {

    private val orders = mutableListOf<Order>()
    private val allOrders = mutableListOf<Order>()
    private val displayedOrders = mutableListOf<Order>()
    private var currentFilter: String = "All"
    private lateinit var adapter: OrderAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var loader: View
    private lateinit var contentLayout: View
    private lateinit var emptyContainer: LinearLayout
    private lateinit var rv: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_order, container, false)

        loader = view.findViewById(R.id.orderLoader)
        contentLayout = view.findViewById(R.id.orderContentLayout)
        emptyContainer = view.findViewById(R.id.emptyContainer)
        val btnAll: Button = view.findViewById(R.id.btnAll)
        val btnPlaced: Button = view.findViewById(R.id.btnPlaced)
        val btnPending: Button = view.findViewById(R.id.btnPending)
        val btnCompleted: Button = view.findViewById(R.id.btnCompleted)
        val btnRejected: Button = view.findViewById(R.id.btnRejected)

        fun selectTab(selected: Button) {
            btnAll.isSelected = false
            btnPlaced.isSelected = false
            btnPending.isSelected = false
            btnCompleted.isSelected = false
            btnRejected.isSelected = false
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

        rv = view.findViewById(R.id.ordersRecyclerView)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrderAdapter(
            displayedOrders,
            onOrderClick = { openOrderDetails(it) },
            onCompleteClick = { completeOrder(it) },
            onRejectClick = { rejectOrder(it) }
        )

        rv.adapter = adapter

        loadOrders()
        return view
    }

    private fun loadOrders() {
        loader.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE

        db.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                allOrders.clear()


                for (doc in snapshot) {
                    val order = doc.toObject(Order::class.java)
                    order.orderId = doc.id
                    allOrders.add(order)

                }

                loader.visibility = View.GONE

                if (allOrders.isEmpty()) {
                    showEmpty()
                } else {
                    emptyContainer.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()

                }
                filterOrders(currentFilter)
            }
            .addOnFailureListener {
                loader.visibility = View.GONE
                showEmpty()
            }
    }

    private fun completeOrder(order: Order) {
        db.collection("orders")
            .document(order.orderId)
            .update("status", "Completed")
            .addOnSuccessListener { loadOrders() }
    }

    private fun rejectOrder(order: Order) {
        db.collection("orders")
            .document(order.orderId)
            .update("status", "Rejected")
            .addOnSuccessListener { loadOrders() }
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



    }

