package com.example.zecomerceuser

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


class SearchActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var spinnerSort: Spinner
    private lateinit var btnFilter: Button


    private lateinit var searchAdapter: SearchProductAdapter
    private val productList = mutableListOf<Product>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        recyclerView = findViewById(R.id.recyclerViewSearch)
        searchView = findViewById(R.id.searchView)
        progressBar = findViewById(R.id.progressBar)
        spinnerSort = findViewById(R.id.spinnerSort)
        btnFilter = findViewById(R.id.btnFilter)

        val sortOptions = arrayOf(
            "Recommended",
            "Price: Low to High",
            "Price: High to Low"
        )

        val adapterSpinner = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            sortOptions
        )

        spinnerSort.adapter = adapterSpinner

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                when (position) {
                    0 -> searchAdapter.sortProducts("RECOMMENDED")
                    1 -> searchAdapter.sortProducts("LOW_HIGH")
                    2 -> searchAdapter.sortProducts("HIGH_LOW")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnFilter.setOnClickListener {

            val bottomSheet = FilterBottomSheet { minPrice, maxPrice, category, inStock, sortType ->

                searchAdapter.applyAdvancedFilter(
                    minPrice,
                    maxPrice,
                    category,
                    inStock,
                    sortType
                )
            }

            bottomSheet.show(supportFragmentManager, "FilterBottomSheet")
        }



        recyclerView.layoutManager = LinearLayoutManager(this)

        searchAdapter = SearchProductAdapter(productList) { product ->
            addToCart(product)
        }

        recyclerView.adapter = searchAdapter

        loadProducts()
        setupSearch()



    }

    private fun loadProducts() {
        progressBar.visibility = View.VISIBLE

        db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE

                productList.clear()
                for (doc in snapshot.documents) {
                    val product = doc.toObject(Product::class.java) ?: Product()
                    product.id = doc.id
                    productList.add(product)
                }

                searchAdapter.updateList(productList)
            }
    }

    private fun setupSearch() {
        searchView.isIconified = false

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchAdapter.filter(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchAdapter.filter(newText ?: "")
                return true
            }
        })
    }

    private fun addToCart(product: Product) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to add cart", Toast.LENGTH_SHORT).show()
        }
    }
}
