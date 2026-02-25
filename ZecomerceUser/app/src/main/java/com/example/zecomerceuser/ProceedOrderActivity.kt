package com.example.zecomerceuser

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import java.util.*

class ProceedOrderActivity : BaseActivity() {



    private val ADMIN_UID = "ua2KE2nxZSfc0H1MHNs2nc1SnCl1"
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var tvTotal: TextView
    private lateinit var btnPlaceOrder: Button
    private lateinit var btnCheckout: Button
    private lateinit var rv: RecyclerView
    private lateinit var currentUserName: String
    private lateinit var currentUserEmail: String
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var adapter: CartAdapterReadOnly



    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var settingsClient: SettingsClient
    private val LOCATION_REQ = 111
    private val GPS_REQ = 222
    private lateinit var upiLauncher: ActivityResultLauncher<Intent>
    private var isProcessingOrder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proceed_order)

        // ---- INIT UI ----
        etName = findViewById(R.id.etReceiverName)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        tvTotal = findViewById(R.id.tvTotal)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)
        btnCheckout =findViewById<Button>(R.id.btnCheckout)
        rv = findViewById(R.id.recyclerCart)


        rv.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapterReadOnly(cartItems)
        rv.adapter = adapter




        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        // ---- LOAD DATA ----
        loadUser()
        loadCart()

        // ---- TRY AUTO FETCH LOCATION ----
        checkLocationPermissionAndFetch()

        btnPlaceOrder.setOnClickListener { placeOrder() }

        upiLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleUPIResult(result.resultCode, result.data)
        }

        btnCheckout.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener
            //payUsingUPI()
            processTokenOrder()

        }

    }

    // ---------------- USER ----------------
    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("userdetail")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                currentUserName = doc.getString("fullName") ?: ""
                currentUserEmail = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""

                etPhone.setText(phone)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- CART ----------------
    private fun loadCart() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("cart")
            .get()
            .addOnSuccessListener {
                cartItems.clear()
                var total = 0.0
                for (doc in it) {
                    val item = doc.toObject(CartItem::class.java)
                    cartItems.add(item)
                    total += item.price.toDouble() * item.quantity
                }
                adapter.notifyDataSetChanged()
                tvTotal.text = "Total: ₹${String.format(Locale.US, "%.2f", total)}"
            }
    }

    // ---------------- LOCATION PERMISSION & FETCH ----------------
    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQ
            )
            return
        }
        checkGPSAndFetchLocation()
    }

    private fun checkGPSAndFetchLocation() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener { getCurrentLocation() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(this, GPS_REQ)
                    } catch (e: IntentSender.SendIntentException) {
                        Toast.makeText(this, "Enable GPS manually", Toast.LENGTH_SHORT).show()
                        etAddress.hint = "Enter your address"
                        etAddress.isEnabled = true
                    }
                } else {
                    Toast.makeText(this, "GPS is required for location", Toast.LENGTH_SHORT).show()
                    etAddress.hint = "Enter your address"
                    etAddress.isEnabled = true
                }
            }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // Show hint while fetching
        etAddress.hint = "Fetching address..."
        etAddress.isEnabled = false

        // Fetch fresh location only once
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val list = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )
                        if (!list.isNullOrEmpty()) {
                            // Auto-fill address when location is available
                            etAddress.setText(list[0].getAddressLine(0))
                            etAddress.hint = "" // clear hint only on success
                        } else {
                            Toast.makeText(this, "Unable to fetch address", Toast.LENGTH_SHORT).show()
                            etAddress.hint = "Enter your address"
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Address fetch failed", Toast.LENGTH_SHORT).show()
                        etAddress.hint = "Enter your address"
                    }
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                    etAddress.hint = "Enter your address"
                }

                // Enable typing after fetch attempt
                etAddress.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                etAddress.hint = "Enter your address"
                etAddress.isEnabled = true
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQ &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            checkGPSAndFetchLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            etAddress.hint = "Enter your address"
            etAddress.isEnabled = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQ) {
            getCurrentLocation()
        }
    }

    // ---------------- PLACE ORDER ----------------
    private fun placeOrder() {
        Toast.makeText(this, "placeOrder() called", Toast.LENGTH_SHORT).show()

        // CHECK LOGIN
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        // VALIDATION
        if (!validateInputs()) return

        // SAFETY: empty cart
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // CALCULATE TOTAL SAFELY
        val totalAmount = cartItems.sumOf { item ->
            item.price
                .replace("₹", "")
                .replace("/-", "")
                .trim()
                .toDoubleOrNull() ?: 0.0 * item.quantity
        }

        // FIRESTORE REF
        val orderRef = db.collection("orders").document()

        // ITEM SUMMARY
        val itemSummary = cartItems.joinToString("\n") {
            "${it.name} x ${it.quantity}"
        }

        db.runTransaction { transaction ->
            // ---------- READ FIRST ----------
            val userRef = db.collection("userdetail").document(uid)
            val adminRef = db.collection("userdetail").document(ADMIN_UID)

            val userSnapshot = transaction.get(userRef)
            val adminSnapshot = transaction.get(adminRef)

            val userBalance = userSnapshot.getDouble("balance") ?: 0.0
            val adminBalance = adminSnapshot.getDouble("balance") ?: 0.0

            if (userBalance < totalAmount) {
                throw Exception("Insufficient Token Balance")
            }

            // ---------- READ PRODUCTS AND PREPARE UPDATES ----------
            val productUpdates = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            for (cartItem in cartItems) {
                val productRef = db.collection("products").document(cartItem.productId)
                val productSnap = transaction.get(productRef)
                val currentStock = productSnap.getLong("stock") ?: 0
                if (currentStock < cartItem.quantity) {
                    throw Exception("Insufficient stock for ${cartItem.name}")
                }

                // Prepare the product stock update
                productUpdates.add(
                    productRef to mapOf("stock" to currentStock - cartItem.quantity)
                )
            }

            // ---------- WRITE OPERATIONS ----------
            // Update all product stock
            productUpdates.forEach { (productRef, updateMap) ->
                transaction.update(productRef, updateMap)
            }

            // Update user balance
            transaction.update(userRef, "balance", userBalance - totalAmount)

            // Update admin balance
            transaction.update(adminRef, "balance", adminBalance + totalAmount)

            // ---------- CREATE ORDER ----------
            val order = Order(
                orderId = orderRef.id,
                productName = itemSummary,
                quantity = cartItems.sumOf { it.quantity },
                userId = uid,
                customerName = currentUserName,
                userEmail = currentUserEmail,
                receiverName = etName.text.toString(),
                totalPrice = "₹${String.format(Locale.US, "%.2f", totalAmount)}",
                phone = etPhone.text.toString(),
                address = etAddress.text.toString(),
                status = "PLACED",
                timestamp = System.currentTimeMillis(),
                items = cartItems.toList(),
                totalAmount = totalAmount,
                paymentMode = "TOKEN",
                paymentStatus = "SUCCESS",
                isVisible = true
            )

            // Save the order
            transaction.set(orderRef, order)
        }
            .addOnSuccessListener {
                clearCart()
                showPaymentDialog("Order Placed Successfully 🎉", true)
            }
            .addOnFailureListener { e ->
                showPaymentDialog("Transaction Failed: ${e.message}", false)
            }
    }


    private fun validateInputs(): Boolean {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Customer name is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!name.matches(Regex("^[A-Za-z ]{8,}$"))) {
            Toast.makeText(
                this,
                "Name must contain only letters and minimum 8 characters",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!phone.matches(Regex("^[0-9]{10}$"))) {
            Toast.makeText(
                this,
                "Enter a valid 10-digit phone number",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (address.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (address.length < 10) {
            Toast.makeText(
                this,
                "Please enter a complete address",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    // ---------------- CLEAR CART ----------------
    private fun clearCart() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("cart")
            .get()
            .addOnSuccessListener {
                for (doc in it) doc.reference.delete()

            }
    }
    private fun payUsingUPI() {

        val totalAmount = cartItems.sumOf { item ->
            val price = item.price
                .replace("₹", "")
                .replace("/-", "")
                .trim()
                .toDoubleOrNull() ?: 0.0
            price * item.quantity
        }

        val amount = String.format(Locale.US, "%.2f", totalAmount)
        val txnId = "ZECOM${System.currentTimeMillis()}"

        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", "pvaghasiya087@okicici")
            .appendQueryParameter("pn", "ZEcommerce")
            .appendQueryParameter("tn", "Order Payment")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", txnId)
            .build()

        upiLauncher.launch(
            Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay using UPI")
        )
    }
    private fun handleUPIResult(resultCode: Int, data: Intent?) {

        if (resultCode != Activity.RESULT_OK || data == null) {
            showPaymentDialog("Payment Cancelled", false)
            return
        }

        val response = data.getStringExtra("response") ?: ""

        val status = getUPIValue(response, "Status")
            ?: getUPIValue(response, "status")
            ?: getUPIValue(response, "txnStatus")

        val txnRef = getUPIValue(response, "ApprovalRefNo")
            ?: getUPIValue(response, "txnRef")

        if (status.equals("SUCCESS", true) && !isProcessingOrder) {
            isProcessingOrder = true
            saveOrderWithPayment(txnRef,response)
        } else {
            showPaymentDialog("Payment Failed", false)
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
    private fun saveOrderWithPayment(txnRef: String?, upiResponse: String) {

        val uid = auth.currentUser?.uid ?: return

        if (!validateInputs()) {
            isProcessingOrder = false
            return
        }

        // ----- CALCULATE TOTAL -----
        val totalAmount = cartItems.sumOf { item ->
            val price = item.price
                .replace("₹", "")
                .replace("/-", "")
                .trim()
                .toDoubleOrNull() ?: 0.0
            price * item.quantity
        }

        // ----- ITEM SUMMARY (SAME AS placeOrder) -----
        val itemSummary = cartItems.joinToString(separator = "\n") { item ->
            "${item.name} x ${item.quantity}"
        }

        // ----- ORDER REF -----
        val orderRef = db.collection("orders").document()

        // ----- FULL ORDER DATA (MERGED) -----
        val orderData = hashMapOf(
            // Order Info
            "orderId" to orderRef.id,
            "userId" to uid,
            "productName" to itemSummary,
            "quantity" to cartItems.sumOf { it.quantity },
            "items" to cartItems.toList(),

            // User Info
            "customerName" to currentUserName,
            "userEmail" to currentUserEmail,
            "receiverName" to etName.text.toString(),
            "phone" to etPhone.text.toString(),
            "address" to etAddress.text.toString(),

            // Pricing
            "totalPrice" to "₹${String.format(Locale.US, "%.2f", totalAmount)}",
            "totalAmount" to totalAmount,

            // Payment Info
            "paymentMode" to "UPI",
            "paymentStatus" to "SUCCESS",
            "transactionRef" to txnRef,
            "upiRawResponse" to upiResponse,

            // Order Status
            "status" to "PLACED",
            "timestamp" to System.currentTimeMillis()
        )

        // ----- SAVE -----
        orderRef.set(orderData)
            .addOnSuccessListener {
                clearCart()
                showPaymentDialog("Order Placed Successfully 🎉", true)
            }
            .addOnFailureListener {
                isProcessingOrder = false
                showPaymentDialog("Order saving failed", false)
            }
    }

    private fun showPaymentDialog(message: String, success: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Checkout Status")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                if (success) {
                    val intent = Intent(this, MainActivity2::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .show()
    }
    private fun processTokenOrder() {
        val uid = auth.currentUser?.uid ?: return
        if (!validateInputs()) return

        val totalAmount = cartItems.sumOf { item ->
            val price = item.price
                .replace("₹", "")
                .replace("/-", "")
                .trim()
                .toDoubleOrNull() ?: 0.0
            price * item.quantity
        }

        val userRef = db.collection("userdetail").document(uid)
        val adminRef = db.collection("userdetail")
            .document(ADMIN_UID)
        val orderRef = db.collection("orders").document()

        db.runTransaction { transaction ->
            // ---------- READ FIRST ----------
            val userSnapshot = transaction.get(userRef)
            val adminSnapshot = transaction.get(adminRef)

            val userBalance = userSnapshot.getDouble("balance") ?: 0.0
            val adminBalance = adminSnapshot.getDouble("balance") ?: 0.0

            if (userBalance < totalAmount) {
                throw Exception("Insufficient Token Balance")
            }

            // ---------- READ PRODUCTS AND PREPARE UPDATES ----------
            val productUpdates = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            for (cartItem in cartItems) {
                val productRef = db.collection("products").document(cartItem.productId)
                val productSnap = transaction.get(productRef)
                val currentStock = productSnap.getLong("stock") ?: 0
                if (currentStock < cartItem.quantity) {
                    throw Exception("Insufficient stock for ${cartItem.name}")
                }

                // Prepare the product stock update
                productUpdates.add(
                    productRef to mapOf("stock" to currentStock - cartItem.quantity)
                )
            }

            // ---------- WRITE OPERATIONS ----------
            // Update all product stock
            productUpdates.forEach { (productRef, updateMap) ->
                transaction.update(productRef, updateMap)
            }

            // Update user balance
            transaction.update(userRef, "balance", userBalance - totalAmount)

            // Update admin balance
            transaction.update(adminRef, "balance", adminBalance + totalAmount)

            // ---------- CREATE ORDER ----------
            val itemSummary = cartItems.joinToString("\n") { "${it.name} x ${it.quantity}" }

            val order = Order(
                orderId = orderRef.id,
                productName = itemSummary,
                quantity = cartItems.sumOf { it.quantity },
                userId = uid,
                customerName = currentUserName,
                userEmail = currentUserEmail,
                receiverName = etName.text.toString(),
                totalPrice = "₹${String.format(Locale.US, "%.2f", totalAmount)}",
                phone = etPhone.text.toString(),
                address = etAddress.text.toString(),
                status = "PLACED",
                timestamp = System.currentTimeMillis(),
                items = cartItems.toList(),
                totalAmount = totalAmount,
                paymentMode = "TOKEN",
                paymentStatus = "SUCCESS",
                isVisible = true
            )

            // Save the order
            transaction.set(orderRef, order)
        }
            .addOnSuccessListener {
                clearCart()
                showPaymentDialog("Order Placed Successfully 🎉", true)
            }
            .addOnFailureListener { e ->
                showPaymentDialog("Transaction Failed: ${e.message}", false)
            }
    }






}
