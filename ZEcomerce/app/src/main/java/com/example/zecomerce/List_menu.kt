package com.example.zecomerce

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class List_menu : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productList: ArrayList<Product>
    private lateinit var adapter: ProductAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_menu)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        productList = arrayListOf()
        adapter = ProductAdapter(this, productList,
            onUpdate = { product, position -> showProductDialog(product, position) },
            onDelete = { product, _ -> deleteProduct(product) }
        )

        recyclerView.adapter = adapter

        loadProducts()

        findViewById<Button>(R.id.btnAddProduct).setOnClickListener {
            showProductDialog(null, null)
        }
    }

    // 🔹 RETRIEVE
    private fun loadProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                productList.clear()
                for (doc in result) {
                    val product = doc.toObject(Product::class.java)
                    product.id = doc.id
                    productList.add(product)
                }
                adapter.notifyDataSetChanged()
            }
    }

    // 🔹 ADD & UPDATE DIALOG
    private fun showProductDialog(product: Product?, position: Int?) {
        val view = layoutInflater.inflate(R.layout.dialog_update_product, null)

        val edtName = view.findViewById<EditText>(R.id.edtName)
        val edtPrice = view.findViewById<EditText>(R.id.edtPrice)
        val edtDesc = view.findViewById<EditText>(R.id.edtDescription)

        if (product != null) {
            edtName.setText(product.name)
            edtPrice.setText(product.price)
            edtDesc.setText(product.description)
        }

        AlertDialog.Builder(this)
            .setTitle(if (product == null) "Add Product" else "Update Product")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = edtName.text.toString()
                val price = edtPrice.text.toString()
                val desc = edtDesc.text.toString()

                if (product == null) {
                    addProduct(name, price, desc)
                } else {
                    updateProduct(product.id, name, price, desc)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 🔹 ADD
    private fun addProduct(name: String, price: String, desc: String) {
        val product = Product(
            name = name,
            price = price,
            description = desc,
            imageUrl = "" // default image
        )

        db.collection("products")
            .add(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Product Added", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
    }

    // 🔹 UPDATE
    private fun updateProduct(id: String, name: String, price: String, desc: String) {
        db.collection("products").document(id)
            .update(
                mapOf(
                    "name" to name,
                    "price" to price,
                    "description" to desc
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Product Updated", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
    }

    // 🔹 DELETE
    private fun deleteProduct(product: Product) {
        db.collection("products").document(product.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Product Deleted", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
    }
}
