package com.example.zecomerceuser

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class CartFragment : BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button
    private lateinit var btnProceed: Button
    private lateinit var emptyContainer: LinearLayout
    private lateinit var loader: View

    private val cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var upiLauncher: ActivityResultLauncher<Intent>
    private var isOrderPlaced = false

    private var cartListener: ListenerRegistration? = null // Real-time listener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        recyclerView = view.findViewById(R.id.cartRecyclerView)
        tvTotal = view.findViewById(R.id.tvTotal)
        emptyContainer = view.findViewById(R.id.emptyContainer)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        btnProceed = view.findViewById(R.id.btnPrced)
        loader = view.findViewById(R.id.cartLoader)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        cartAdapter = CartAdapter(
            cartItems,
            onUpdate = { updateQuantity(it); updateTotal() },
            onDelete = { deleteItem(it) }
        )
        recyclerView.adapter = cartAdapter

        upiLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> handleUPIResult(result.resultCode, result.data) }

        btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                payUsingUPI()
            }
        }

        btnProceed.setOnClickListener {
            startActivity(Intent(requireContext(), ProceedOrderActivity::class.java))
        }

        loadCartItems()
        return view
    }

    /* ---------------- CART LOGIC ---------------- */

    private fun parsePrice(price: String): Double =
        price.replace("₹", "").replace("/-", "").trim().toDoubleOrNull() ?: 0.0

    private fun loadCartItems() {
        val uid = auth.currentUser?.uid ?: run {
            showEmpty(); return
        }

        loader.visibility = View.VISIBLE

        // Remove old listener if exists
        cartListener?.remove()

        cartListener = db.collection("users").document(uid)
            .collection("cart")
            .addSnapshotListener { cartSnap, error ->

                loader.visibility = View.GONE
                if (error != null || cartSnap == null) {
                    showEmpty()
                    return@addSnapshotListener
                }
                cartItems.clear()
                if (cartSnap.isEmpty) {
                    cartItems.clear()
                    cartAdapter.notifyDataSetChanged()
                    updateTotal()
                    showEmpty()
                    return@addSnapshotListener
                }

                for (doc in cartSnap.documents) {
                    cartItems.add(
                        CartItem(
                            docId = doc.id,
                            productId = doc.getString("productId") ?: "",
                            name = doc.getString("name") ?: "",
                            price = doc.getString("price") ?: "0",
                            originalPrice = doc.getString("originalPrice") ?: "0",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            quantity = doc.getLong("quantity")?.toInt() ?: 1,
                            isOffer = doc.getBoolean("isOffer") ?: false
                        )
                    )
                }

                cartAdapter.notifyDataSetChanged()
                updateTotal()
            }
    }

    private fun updateQuantity(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("cart").document(item.docId)
            .update("quantity", item.quantity)
    }

    private fun deleteItem(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("cart").document(item.docId)
            .delete()
            .addOnSuccessListener {
                cartItems.remove(item)
                cartAdapter.notifyDataSetChanged()
                updateTotal()
            }
    }

    private fun updateTotal() {
        val total = cartItems.sumOf { parsePrice(it.price) * it.quantity }
        tvTotal.text = "₹${String.format(Locale.US, "%.2f", total)}"

        if (cartItems.isEmpty()) showEmpty()
        else {
            emptyContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmpty() {
        emptyContainer.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun clearCart() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("cart")
            .get()
            .addOnSuccessListener {
                for (doc in it) doc.reference.delete()
                cartItems.clear()
                cartAdapter.notifyDataSetChanged()
                updateTotal()
            }
    }

    /* ---------------- UPI PAYMENT ---------------- */

    private fun payUsingUPI() {
        val amount = String.format(
            Locale.US,
            "%.2f",
            cartItems.sumOf { parsePrice(it.price) * it.quantity }
        )

        val txnId = "ZECOM${System.currentTimeMillis()}"

        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", "pvaghasiya087@okicici")
            .appendQueryParameter("pn", "ZEcommerce")
            .appendQueryParameter("tn", "OrderPayment")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", txnId)
            .build()

        try {
            upiLauncher.launch(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay using UPI"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No UPI app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUPIResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            Toast.makeText(requireContext(), "Payment Cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        val response = data.getStringExtra("response") ?: ""
        if (response.isEmpty()) {
            Toast.makeText(requireContext(), "Payment Cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        val status = getUPIValue(response, "Status")
            ?: getUPIValue(response, "status")
            ?: getUPIValue(response, "txnStatus")

        val txnRef = getUPIValue(response, "ApprovalRefNo")
            ?: getUPIValue(response, "approvalrefno")
            ?: getUPIValue(response, "txnRef")
            ?: getUPIValue(response, "txnref")

        if (status.equals("SUCCESS", true) && !isOrderPlaced) {
            isOrderPlaced = true
            placeOrder(txnRef)
        } else {
            Toast.makeText(requireContext(), "Payment Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUPIValue(response: String, key: String): String? {
        response.split("&").forEach {
            val pair = it.split("=")
            if (pair.size == 2 && pair[0].equals(key, true)) {
                return Uri.decode(pair[1])
            }
        }
        return null
    }

    private fun placeOrder(txnRef: String?) {
        clearCart()
        Toast.makeText(
            requireContext(),
            "Order placed successfully\nTxn: $txnRef",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cartListener?.remove()
    }
}
