package com.example.zecomerceuser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class HomeFragment : BaseFragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator

    private val offers = mutableListOf<Offer>()
    private lateinit var offerAdapter: OfferAdapter
    private val offerHandler = Handler(Looper.getMainLooper())

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var productAdapter: ProductAdapter
    private lateinit var btnSearch: Button
    private val productList = mutableListOf<Product>()
    private val db = FirebaseFirestore.getInstance()
    private var productListener: ListenerRegistration? = null  // Firestore listener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        viewPager = view.findViewById(R.id.offerViewPager)
        dotsIndicator = view.findViewById(R.id.dotsIndicator)
        btnSearch=view.findViewById<Button>(R.id.btnSearch)

        btnSearch.setOnClickListener{
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)

        }

        offerAdapter = OfferAdapter(offers) { offer ->
            offer.id?.let { id ->
                val intent = android.content.Intent(
                    requireContext(),
                    OfferDetailsActivity::class.java
                )
                intent.putExtra("offerId", offer.id)
                startActivity(intent)
            }
        }

        viewPager.adapter = offerAdapter
        dotsIndicator.attachTo(viewPager)

        loadOffers()
        startAutoScroll()

        recyclerView = view.findViewById(R.id.recyclerProducts)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(false)

        productAdapter = ProductAdapter(productList) { product ->
            addToCart(product)
        }
        recyclerView.adapter = productAdapter

        loadProductsRealtime()  // Use real-time listener

        return view
    }

    // ================== OFFER LOGIC ==================

    private fun loadOffers() {
        db.collection("offers")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                Log.d("HOME_OFFERS", "Snapshot = ${snapshot?.documents?.size}")

                if (error != null) {
                    android.util.Log.e("HOME_OFFERS", "Failed to load offers", error)
                    viewPager.visibility = View.GONE
                    dotsIndicator.visibility = View.GONE
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    android.util.Log.w("HOME_OFFERS", "Snapshot is null")
                    viewPager.visibility = View.GONE
                    dotsIndicator.visibility = View.GONE
                    return@addSnapshotListener
                }

                if (snapshot.isEmpty) {
                    android.util.Log.d("HOME_OFFERS", "No active offers found")
                    viewPager.visibility = View.GONE
                    dotsIndicator.visibility = View.GONE
                    offerAdapter.notifyDataSetChanged()
                    return@addSnapshotListener
                }

                offers.clear()
                for (doc in snapshot) {
                    val offer = doc.toObject(Offer::class.java)
                    if (offer != null) {
                        offer.id = doc.id
                        offers.add(offer)
                    }
                    else {
                        android.util.Log.w("HOME_OFFERS", "Failed to parse offer ${doc.id}")
                    }
                }
                if (offers.isEmpty()) {
                    viewPager.visibility = View.GONE
                    dotsIndicator.visibility = View.GONE
                } else {
                    viewPager.visibility = View.VISIBLE
                    dotsIndicator.visibility = View.VISIBLE
                }
                android.util.Log.d("HOME_OFFERS", "Loaded ${offers.size} offers")
                offerAdapter.notifyDataSetChanged()
            }
    }

    private fun startAutoScroll() {
        val runnable = object : Runnable {
            override fun run() {
                if (offers.isNotEmpty()) {
                    viewPager.currentItem =
                        (viewPager.currentItem + 1) % offers.size
                }

                offerHandler.postDelayed(this, 3000)
            }
        }
        offerHandler.postDelayed(runnable, 3000)
    }

    // ================== EXISTING PRODUCT LOGIC (UNCHANGED) ==================

    private fun loadProductsRealtime() {
        progressBar.visibility = View.VISIBLE

        // Remove previous listener if any
        productListener?.remove()

        // Real-time listener for products collection
        productListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    productList.clear()
                    for (doc in snapshot.documents) {
                        val product = doc.toObject(Product::class.java) ?: Product()
                        product.id = doc.id
                        productList.add(product)
                    }
                    productAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun addToCart(product: Product) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val cartRef = db
            .collection("users")
            .document(userId)
            .collection("cart")
            .document(product.id)

        cartRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val currentQty = doc.getLong("quantity") ?: 1
                cartRef.update("quantity", currentQty + 1)
            } else {
                val cartItem = CartItem(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    quantity = 1
                )
                cartRef.set(cartItem)
            }
            Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to add cart", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listener to avoid memory leaks
        productListener?.remove()
    }
}
