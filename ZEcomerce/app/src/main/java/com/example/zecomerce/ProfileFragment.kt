package com.example.zecomerce

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import android.os.Bundle
import android.view.*
import android.widget.*
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : BaseFragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var btnEditProfile: TextView
    private lateinit var btnLogout: Button
    private lateinit var phoneText: TextView
    private lateinit var balanceText: TextView
    private lateinit var btnEditBalance: Button



    private var dialogImageView: ImageView? = null
    private var selectedImageUri: Uri? = null
    private var oldImageUrl: String = ""

    // Image picker
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                dialogImageView?.setImageURI(it) // show ONLY in dialog
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.profileImage)
        usernameText = view.findViewById(R.id.txtUsername)
        emailText = view.findViewById(R.id.txtEmail)
        btnEditProfile = view.findViewById(R.id.txtEditProfile)
        balanceText = view.findViewById(R.id.txtBalance)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditBalance = view.findViewById(R.id.txtEditBalance)

        loadUserProfile()

        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnLogout.setOnClickListener { logoutUser() }
        btnEditBalance.setOnClickListener { //showEditBalanceDialog()
             }

        return view
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return

        emailText.text = user.email

        db.collection("userdetail").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {

                    val username = doc.getString("username") ?: ""
                    usernameText.text = if (username.isEmpty()) "User" else username
                    emailText.text = doc.getString("email") ?: ""
                    balanceText.text =
                        "Balance: ₹%.2f".format(doc.getDouble("balance") ?: 0.0)

                    oldImageUrl = doc.getString("profileImage") ?: ""

                    if (oldImageUrl.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(oldImageUrl)
                            .circleCrop()
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profilee)
                    }
                }
            }
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        val edtUsername = dialogView.findViewById<EditText>(R.id.edtUsername)
        val imgPick = dialogView.findViewById<ImageView>(R.id.imgPickProfile)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)

        val user = auth.currentUser ?: return

        // 🔴 RESET STATE EVERY TIME
        selectedImageUri = null
        dialogImageView = imgPick
        tvError.visibility = View.GONE

        edtUsername.setText(usernameText.text.toString())

        // Load OLD image into dialog
        if (oldImageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(oldImageUrl)
                .circleCrop()
                .into(imgPick)
        } else {
            imgPick.setImageResource(R.drawable.ic_profilee)
        }

        imgPick.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { _, _ ->
                selectedImageUri = null
                dialogImageView = null
            }
            .create()

        dialog.setOnShowListener {
            val btnSave = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

            btnSave.setOnClickListener {

                val newUsername = edtUsername.text.toString().trim()

                if (newUsername.isEmpty()) {
                    tvError.text = "Username cannot be empty"
                    tvError.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                btnSave.isEnabled = false
                if (selectedImageUri != null) {
                    uploadImageToSupabase(selectedImageUri!!) { imageUrl ->
                        updateUserProfile(user.uid, newUsername, imageUrl)
                        dialog.dismiss()
                    }
                } else {
                    updateUserProfile(user.uid, newUsername, oldImageUrl)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateUserProfile(uid: String, username: String, imageUrl: String) {
        val data = mutableMapOf<String, Any>("username" to username)
        if (imageUrl.isNotEmpty()) data["profileImage"] = imageUrl

        db.collection("userdetail").document(uid)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                loadUserProfile()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
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


    private fun uploadImageToSupabase(
        uri: Uri,
        callback: (String) -> Unit
    ) {
        val supabaseUrl = "https://jpdmkzbbeijbspmjskyz.supabase.co"
        val supabaseKey = "YOUR_SUPABASE_KEY"
        val fileName = "profile/${System.currentTimeMillis()}.jpg"

        val tempFile = File(requireContext().cacheDir, "temp_image.jpg")

        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ SAME compression as product
                val compressedFile = compressImage(tempFile)

                val requestBody =
                    compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$supabaseUrl/storage/v1/object/media/$fileName?upsert=true")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .put(requestBody)
                    .build()

                val response = OkHttpClient().newCall(request).execute()

                if (response.isSuccessful) {
                    val publicUrl =
                        "$supabaseUrl/storage/v1/object/public/media/$fileName"

                    withContext(Dispatchers.Main) {
                        callback(publicUrl)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        activity?.finish()
    }

}
