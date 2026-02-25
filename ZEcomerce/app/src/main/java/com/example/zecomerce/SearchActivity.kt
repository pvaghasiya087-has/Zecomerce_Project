package com.example.zecomerce

import android.os.Bundle
import android.view.View
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.graphics.Matrix
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class SearchActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var spinnerSort: Spinner
    private lateinit var btnFilter: Button
    private var selectedImageUri: Uri? = null
    private var currentDialogImageView: ImageView? = null
    private var productListener: ListenerRegistration? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                currentDialogImageView?.setImageURI(it)
            }
        }


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

        searchAdapter = SearchProductAdapter(
            productList,
            onUpdate = { product ->
                showProductDialog(product)
            },
            onDelete = { product ->
                confirmDeleteProduct(product)
            }
        )




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
                    val isActive = doc.getBoolean("isActive") ?: true
                    if (isActive) productList.add(product)
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

private fun deleteProduct(product: Product) {
    FirebaseFirestore.getInstance()
        .collection("products")
        .document(product.id)
        .update("active", false)
        .addOnSuccessListener {
            Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()
            loadProducts()
        }
}
private fun updateProduct(id: String, name: String, price: String, desc: String, imageUrl: String,category: String,
                          stock: Int) {
    db.collection("products").document(id).update(
        mapOf(
            "name" to name,
            "price" to price,
            "description" to desc,
            "imageUrl" to imageUrl,
            "category" to category,
            "stock" to stock
        )
    )
}
private fun uploadImageToSupabase(
    uri: Uri,
    progressBar: ProgressBar,
    tvStatus: TextView,
    callback: (String) -> Unit
) {
    val supabaseUrl = "https://jpdmkzbbeijbspmjskyz.supabase.co"
    val supabaseKey = "sb_publishable_DJHseibiO4vMvoTJnhB2xg_KbNAuj-M"
    val fileName = "product/${System.currentTimeMillis()}.jpg"
    val tempFile = File(this.cacheDir, "temp_image.jpg")
    this.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
    }

    CoroutineScope(Dispatchers.IO).launch {
        val compressed = compressImage(tempFile)
        val requestBody = ProgressRequestBody(compressed, "image/jpeg") { progress ->
            CoroutineScope(Dispatchers.Main).launch {
                progressBar.progress = progress
                tvStatus.text = "Uploading $progress%"
            }
        }
        val request = Request.Builder()
            .url("$supabaseUrl/storage/v1/object/media/$fileName?upsert=true")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .put(requestBody)
            .build()

        val response = OkHttpClient().newCall(request).execute()
        if (response.isSuccessful) {
            val publicUrl = "$supabaseUrl/storage/v1/object/public/media/$fileName"
            CoroutineScope(Dispatchers.Main).launch { callback(publicUrl) }
        } else {
            CoroutineScope(Dispatchers.Main).launch { tvStatus.text = "Upload failed" }
        }
    }
}
private fun compressImage(file: File, maxSizeKB: Int = 200): File {
    // 1️⃣ Decode bitmap safely
    val bitmap = try {
        BitmapFactory.decodeFile(file.absolutePath)
    } catch (e: Exception) {
        return file // fallback if decode fails
    } ?: return file // fallback if bitmap is null

    // 2️⃣ Fix EXIF rotation safely
    val fixedBitmap = try {
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        }
        if (!matrix.isIdentity) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
    } catch (e: Exception) {
        bitmap // fallback
    }

    // 3️⃣ Prepare output file
    val outFile = File(file.parent, "compressed_${System.currentTimeMillis()}.jpg")

    // 4️⃣ Compress by quality only (no resize)
    var quality = 95
    var currentSizeKB: Long

    do {
        FileOutputStream(outFile).use {
            fixedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
        }
        currentSizeKB = outFile.length() / 1024
        quality -= 3
        if (quality < 75) break // prevent visible quality loss
    } while (currentSizeKB > maxSizeKB)

    bitmap.recycle()
    if (fixedBitmap != bitmap) fixedBitmap.recycle()

    return outFile
}
private fun showProductDialog(product: Product?) {
    selectedImageUri = null

    val dialogView = layoutInflater.inflate(R.layout.dialog_update_product, null)

    val edtName = dialogView.findViewById<EditText>(R.id.edtName)
    val edtPrice = dialogView.findViewById<EditText>(R.id.edtPrice)
    val edtDesc = dialogView.findViewById<EditText>(R.id.edtDescription)
    val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
    val edtStock = dialogView.findViewById<EditText>(R.id.edtStock)
    val imgPick = dialogView.findViewById<ImageView>(R.id.imgPick)
    val tvError = dialogView.findViewById<TextView>(R.id.tvError)
    val tvStatus = dialogView.findViewById<TextView>(R.id.tvStatus)
    val progress = dialogView.findViewById<ProgressBar>(R.id.progressUpload)

    val categories = listOf("Electronics", "Clothing", "Shoes", "Accessories")

    val categoryAdapter = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        categories
    )

    spinnerCategory.adapter = categoryAdapter

    currentDialogImageView = imgPick

    if (product != null) {
        edtName.setText(product.name)
        edtPrice.setText(product.price)
        edtDesc.setText(product.description)
        edtStock.setText(product.stock.toString())

        val position = categories.indexOf(product.category)
        if (position >= 0) spinnerCategory.setSelection(position)

        Glide.with(this).load(product.imageUrl).into(imgPick)
    }

    imgPick.setOnClickListener {
        imagePickerLauncher.launch("image/*")
    }




    val dialog = AlertDialog.Builder(this)
        .setTitle(if (product == null) "Add Product" else "Update Product")
        .setView(dialogView)
        .setPositiveButton("Save", null)
        .setNegativeButton("Cancel", null)
        .create()

    dialog.setOnShowListener {
        val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        btnSave.setOnClickListener {

            val name = edtName.text.toString().trim()
            val price = edtPrice.text.toString().trim()
            val stockText = edtStock.text.toString().trim()
            val selectedCategory = spinnerCategory.selectedItem.toString()
            val desc = edtDesc.text.toString().trim()

            tvError.text = ""

            if (name.isEmpty() || price.isEmpty() || desc.isEmpty() || stockText.isEmpty()) {
                tvError.text = "All fields required"
                return@setOnClickListener
            }

            if (product == null && selectedImageUri == null) {
                tvError.text = "Select product image"
                return@setOnClickListener
            }

            val pricecheck = price.toDoubleOrNull()
            val stock = stockText.toIntOrNull()

            if (pricecheck == null || stock == null) {
                tvError.text = "Invalid price or stock"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            progress.visibility = View.VISIBLE
            progress.progress = 0
            tvStatus.visibility = View.VISIBLE
            tvStatus.text = "Preparing upload..."

            if (selectedImageUri != null) {
                uploadImageToSupabase(selectedImageUri!!, progress, tvStatus) { imageUrl ->
                    tvStatus.text = "Saving product..."
                    if (product == null)
                        //addProduct(name, price, desc, imageUrl,selectedCategory, stock)
                    else
                        updateProduct(product.id, name, price, desc, imageUrl,selectedCategory, stock)
                    dialog.dismiss()
                    selectedImageUri = null
                }
            } else {
                tvStatus.text = "Saving product..."
                updateProduct(product!!.id, name, price, desc, product.imageUrl,selectedCategory, stock)
                dialog.dismiss()
            }
        }
    }

    dialog.show()
}

private fun confirmDeleteProduct(product: Product) {
    AlertDialog.Builder(this)
        .setTitle("Deactivate Product")
        .setMessage("Do you want to deactivate ${product.name}? This will hide it from users.")
        .setPositiveButton("Yes") { _, _ -> deactivateProduct(product) }
        .setNegativeButton("No", null)
        .show()
}

private fun deactivateProduct(product: Product) {
    db.collection("products").document(product.id)
        .update("active", false)
        .addOnSuccessListener {
            Toast.makeText(this, "${product.name} has been deactivated", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}


    private fun addToCart(product: Product) {
        // You can copy your HomeFragment addToCart code here
    }
}
