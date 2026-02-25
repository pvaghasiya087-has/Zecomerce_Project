package com.example.zecomerce

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.graphics.Matrix
import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ItemsFragment : BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productList: ArrayList<Product>
    private lateinit var adapter: ProductAdapter
    private val db = FirebaseFirestore.getInstance()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_items, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewProducts)
        val btnSearch=view.findViewById<ImageView>(R.id.btnSearch)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        productList = arrayListOf()
        adapter = ProductAdapter(
            requireContext(),
            productList,
            onUpdate = { product, _ -> showProductDialog(product) },
            onDelete = { product, _ -> confirmDeleteProduct(product) }
        )
        recyclerView.adapter = adapter

        view.findViewById<ImageView>(R.id.btnAddItem)
            .setOnClickListener { showProductDialog(null) }

        btnSearch.setOnClickListener{
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        loadProductsRealtime()
        return view
    }

    override fun onResume() {
        super.onResume()
        // Already handled by realtime listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        productListener?.remove() // stop listening when fragment is destroyed
    }

    private fun loadProductsRealtime() {
        productListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    productList.clear()
                    for (doc in snapshot.documents) {
                        val product = doc.toObject(Product::class.java)
                        if (product != null) {
                            product.id = doc.id
                            val isActive = doc.getBoolean("isActive") ?: true
                            if (isActive) productList.add(product)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
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
            requireContext(),
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




        val dialog = AlertDialog.Builder(requireContext())
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
                            addProduct(name, price, desc, imageUrl,selectedCategory, stock)
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


    private fun uploadImageToSupabase(
        uri: Uri,
        progressBar: ProgressBar,
        tvStatus: TextView,
        callback: (String) -> Unit
    ) {
        val supabaseUrl = "https://jpdmkzbbeijbspmjskyz.supabase.co"
        val supabaseKey = "sb_publishable_DJHseibiO4vMvoTJnhB2xg_KbNAuj-M"
        val fileName = "product/${System.currentTimeMillis()}.jpg"
        val tempFile = File(requireContext().cacheDir, "temp_image.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
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

    private fun addProduct(name: String, price: String, desc: String, imageUrl: String,category: String,
                           stock: Int) {
        db.collection("products").add(
            Product(name = name, price = price, description = desc, imageUrl = imageUrl, isActive = true,category = category,
                stock = stock)
        )
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

    private fun confirmDeleteProduct(product: Product) {
        AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "${product.name} has been deactivated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
