package com.example.zecomerce

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.FileOutputStream

class OfferFragment : BaseFragment() {

    private val db = FirebaseFirestore.getInstance()
    private val offers = mutableListOf<Offer>()
    private val allProducts = mutableListOf<Product>()
    private lateinit var offerAdapter: OfferAdminAdapter
    private var offersListener: ListenerRegistration? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddOffer: ImageView
    private lateinit var progressBar: ProgressBar
    private var selectedImageUri: Uri? = null
    private var currentDialogImageView: ImageView? = null


    private var productsLoaded = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                currentDialogImageView?.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_offer, container, false)

        recyclerView = view.findViewById(R.id.recyclerOffers)
        btnAddOffer = view.findViewById(R.id.btnAddOffer)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        offerAdapter = OfferAdminAdapter(
            offers,
            onEdit = { offer -> showOfferDialog(offer) },
            onDelete = { offer -> deleteOffer(offer) }
        )
        recyclerView.adapter = offerAdapter

        btnAddOffer.setOnClickListener {
            if (!productsLoaded) {
                Toast.makeText(requireContext(), "Please wait, products are loading...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOfferDialog(null)
        }

        loadProducts()
        loadOffersRealtime()

        return view
    }

    private fun loadProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                allProducts.clear()
                for (doc in snapshot.documents) {
                    val p = doc.toObject(Product::class.java)
                    p?.id = doc.id
                    if (p != null) allProducts.add(p)
                }
                productsLoaded = true
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadOffersRealtime() {
        progressBar.visibility = View.VISIBLE
        offersListener?.remove()
        offersListener = db.collection("offers")
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    offers.clear()
                    for (doc in snapshot.documents) {
                        val offer = doc.toObject(Offer::class.java) ?: continue
                        offer.id = doc.id
                        offers.add(offer)
                    }
                    offerAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun showOfferDialog(existingOffer: Offer?) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_offer, null)

        val edtTitle = dialogView.findViewById<EditText>(R.id.edtOfferTitle)
        val imgBanner = dialogView.findViewById<ImageView>(R.id.imgBanner)
        val chkActive = dialogView.findViewById<CheckBox>(R.id.chkActive)
        val btnPickImage = dialogView.findViewById<TextView>(R.id.btnPickImage)
        val recyclerProducts = dialogView.findViewById<RecyclerView>(R.id.recyclerProducts)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvStatus)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progressUpload)

        selectedImageUri = null
        currentDialogImageView = imgBanner

        // Edit mode
        if (existingOffer != null) {
            edtTitle.setText(existingOffer.title)
            chkActive.isChecked = existingOffer.isActive
            Glide.with(this).load(existingOffer.imageUrl).into(imgBanner)
        }

        btnPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        imgBanner.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
        val productAdapter =
            OfferProductSelectAdapter(requireContext(),allProducts, existingOffer?.products ?: emptyList())
        recyclerProducts.adapter = productAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (existingOffer == null) "Add Offer" else "Update Offer")
            .setView(dialogView)
            .setPositiveButton(if (existingOffer == null) "Add" else "Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {

            val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            btnSave.setOnClickListener {

                val title = edtTitle.text.toString().trim()
                val isActive = chkActive.isChecked
                val selectedProducts = productAdapter.getSelectedProducts()

                tvError.text = ""

                if (title.isEmpty() || selectedProducts.isEmpty()) {
                    tvError.text = "Title and at least one product required"
                    return@setOnClickListener
                }

                if (existingOffer == null && selectedImageUri == null) {
                    tvError.text = "Select offer image"
                    return@setOnClickListener
                }

                // UI state (same as product)
                btnSave.isEnabled = false
                progress.visibility = View.VISIBLE
                progress.progress = 0
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Preparing upload..."

                if (selectedImageUri != null) {

                    uploadOfferImageToSupabase(
                        uri = selectedImageUri!!,
                        progressBar = progress,
                        tvStatus = tvStatus
                    ) { imageUrl ->

                        tvStatus.text = "Saving offer..."

                        saveOffer(
                            existingOffer?.id,
                            title,
                            imageUrl,
                            isActive,
                            selectedProducts
                        )

                        dialog.dismiss()
                        selectedImageUri = null
                    }

                } else {
                    saveOffer(
                        existingOffer!!.id,
                        title,
                        existingOffer.imageUrl,
                        isActive,
                        selectedProducts
                    )
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }


    private fun saveOffer(
        id: String?,
        title: String,
        imageUrl: String,
        isActive: Boolean,
        products: List<OfferProduct>
    ) {
        val offer = Offer(
            id = id ?: "",
            title = title,
            imageUrl = imageUrl,
            isActive = isActive,
            products = products
        )

        if (id == null) {
            db.collection("offers").add(offer)
                .addOnSuccessListener { Toast.makeText(requireContext(), "Offer added", Toast.LENGTH_SHORT).show() }
        } else {
            db.collection("offers").document(id).set(offer)
                .addOnSuccessListener { Toast.makeText(requireContext(), "Offer updated", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun deleteOffer(offer: Offer) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Offer")
            .setMessage("Are you sure you want to delete this offer?")
            .setPositiveButton("Yes") { dialog, _ ->
                db.collection("offers").document(offer.id).delete()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun uploadOfferImageToSupabase(
        uri: Uri,
        progressBar: ProgressBar,
        tvStatus: TextView,
        callback: (String) -> Unit
    ) {
        val supabaseUrl = "https://jpdmkzbbeijbspmjskyz.supabase.co"
        val supabaseKey = "sb_publishable_DJHseibiO4vMvoTJnhB2xg_KbNAuj-M"
        val fileName = "offers/${System.currentTimeMillis()}.jpg"

        val tempFile = File(requireContext().cacheDir, "temp_offer.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {

            withContext(Dispatchers.Main) {
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Compressing image..."
                progressBar.visibility = View.VISIBLE
                progressBar.isIndeterminate = true
            }

            val compressedFile = compressImage(tempFile)

            withContext(Dispatchers.Main) {
                progressBar.isIndeterminate = false
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                tvStatus.text = "Uploading 0%"
            }

            val requestBody = ProgressRequestBody(compressedFile, "image/jpeg") { progress ->
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

            CoroutineScope(Dispatchers.Main).launch {
                if (response.isSuccessful) {
                    val publicUrl =
                        "$supabaseUrl/storage/v1/object/public/media/$fileName"
                    tvStatus.text = "Upload complete"
                    callback(publicUrl)
                } else {
                    tvStatus.text = "Upload failed"
                }
            }
        }
    }



    private fun compressImage(file: File, maxSizeKB: Int = 200): File {
        val bitmap = try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            return file
        } ?: return file

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
            bitmap
        }

        val outFile = File(file.parent, "compressed_${System.currentTimeMillis()}.jpg")

        var quality = 95
        var currentSizeKB: Long

        do {
            FileOutputStream(outFile).use {
                fixedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
            }
            currentSizeKB = outFile.length() / 1024
            quality -= 3
            if (quality < 75) break
        } while (currentSizeKB > maxSizeKB)

        bitmap.recycle()
        if (fixedBitmap != bitmap) fixedBitmap.recycle()

        return outFile
    }


    override fun onDestroyView() {
        super.onDestroyView()
        offersListener?.remove()
    }

}
