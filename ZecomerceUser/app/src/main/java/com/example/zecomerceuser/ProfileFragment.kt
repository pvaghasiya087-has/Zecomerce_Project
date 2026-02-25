package com.example.zecomerceuser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var phoneText: TextView
    private lateinit var balanceText: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var loader: View
    private lateinit var btnEditBalance: Button


    private var dialogImageView: ImageView? = null
    private var selectedImageUri: Uri? = null
    private var oldImageUrl = ""

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                dialogImageView?.setImageURI(it)
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
        phoneText = view.findViewById(R.id.txtPhone)
        balanceText = view.findViewById(R.id.txtBalance)
        btnEditProfile = view.findViewById(R.id.txtEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        loader = view.findViewById(R.id.profileLoader)
        btnEditBalance = view.findViewById(R.id.txtEditBalance)
        loader.visibility = View.VISIBLE

        loadUserProfile()


        btnEditBalance.setOnClickListener { showEditBalanceDialog() }

        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnLogout.setOnClickListener { logoutUser() }

        return view
    }

    // ================= LOAD PROFILE =================
    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            loader.visibility = View.GONE
            return
        }

        db.collection("userdetail")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    loader.visibility = View.GONE
                    return@addOnSuccessListener
                }

                usernameText.text = doc.getString("fullName") ?: "User"
                emailText.text = doc.getString("email") ?: ""
                phoneText.text = doc.getString("phone") ?: ""
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

                loader.visibility = View.GONE
            }
            .addOnFailureListener {
                loader.visibility = View.GONE
            }
    }

    // ================= EDIT PROFILE =================
    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        val edtUsername = dialogView.findViewById<EditText>(R.id.edtUsername)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtPhone = dialogView.findViewById<EditText>(R.id.edtPhone)
        val imgPick = dialogView.findViewById<ImageView>(R.id.imgPickProfile)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        val user = auth.currentUser ?: return

        edtUsername.setText(usernameText.text)
        edtEmail.setText(emailText.text)
        edtPhone.setText(phoneText.text)

        edtEmail.isEnabled = false
        edtEmail.alpha = 0.6f

        dialogImageView = imgPick
        selectedImageUri = null
        tvError.visibility = View.GONE

        if (oldImageUrl.isNotEmpty()) {
            Glide.with(requireContext()).load(oldImageUrl).circleCrop().into(imgPick)
        }

        imgPick.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = edtUsername.text.toString().trim()
            val phone = edtPhone.text.toString().trim()

            if (!name.matches(Regex("^[A-Za-z ]{8,}$"))) {
                tvError.text = "Name must be minimum 8 characters"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!phone.matches(Regex("^[0-9]{10}$"))) {
                tvError.text = "Enter valid 10 digit phone number"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            loader.visibility = View.VISIBLE

            if (selectedImageUri != null) {
                uploadImageToSupabase(selectedImageUri!!) { imageUrl ->
                    updateUserProfile(user.uid, name, phone, imageUrl)
                    dialog.dismiss()
                }
            } else {
                updateUserProfile(user.uid, name, phone, oldImageUrl)
                dialog.dismiss()
            }
        }
    }

    // ================= UPDATE PROFILE =================
    private fun updateUserProfile(uid: String, name: String, phone: String, imageUrl: String) {
        val data = hashMapOf<String, Any>(
            "fullName" to name,
            "phone" to phone
        )

        if (imageUrl.isNotEmpty()) data["profileImage"] = imageUrl

        db.collection("userdetail")
            .document(uid)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                loadUserProfile()
            }
            .addOnFailureListener {
                loader.visibility = View.GONE
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    // ================= IMAGE UPLOAD =================
    private fun uploadImageToSupabase(uri: Uri, callback: (String) -> Unit) {
        val supabaseUrl = "https://jpdmkzbbeijbspmjskyz.supabase.co"
        val supabaseKey = "sb_publishable_DJHseibiO4vMvoTJnhB2xg_KbNAuj-M"

        val fileName =
            "profile/${safeEmail(auth.currentUser?.email ?: "user")}_${System.currentTimeMillis()}.jpg"

        val tempFile = File(requireContext().cacheDir, "temp.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use {
            FileOutputStream(tempFile).use { out -> it.copyTo(out) }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$supabaseUrl/storage/v1/object/media/$fileName?upsert=true")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .put(tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback("$supabaseUrl/storage/v1/object/public/media/$fileName")
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { loader.visibility = View.GONE }
            }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        activity?.finish()
    }

    private fun safeEmail(email: String): String {
        return email.lowercase().replace(Regex("[^a-z0-9]"), "_")
    }

    // ================= EDIT BALANCE =================
    private fun showEditBalanceDialog() {

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_balance, null)

        val edtBalance = dialogView.findViewById<EditText>(R.id.edtBalance)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        tvError.visibility = View.GONE

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {

            val balanceInput = edtBalance.text.toString().trim()

            if (balanceInput.isEmpty()) {
                tvError.text = "Enter amount"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val amount = balanceInput.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                tvError.text = "Enter valid amount"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            loader.visibility = View.VISIBLE

            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            db.collection("userdetail")
                .document(uid)
                .update("balance", amount)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Balance updated", Toast.LENGTH_SHORT).show()
                    loadUserProfile()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    loader.visibility = View.GONE
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
