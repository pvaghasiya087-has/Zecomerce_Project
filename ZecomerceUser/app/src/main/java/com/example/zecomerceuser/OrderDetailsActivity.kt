package com.example.zecomerceuser

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailsActivity : BaseActivity() {

    private lateinit var tvReceiverName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvOrderId: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var btnDownload: Button
    private lateinit var tvEmail: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var adapter: CartAdapterReadOnly
    private var currentOrder: Order? = null

    private val STORAGE_REQ = 201
    private var orderListener: ListenerRegistration? = null // Firestore listener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        tvReceiverName = findViewById(R.id.tvReceiverName)
        tvAddress = findViewById(R.id.tvAddress)
        tvStatus = findViewById(R.id.tvStatus)
        tvTotal = findViewById(R.id.tvTotal)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvPhone = findViewById(R.id.tvPhone)
        tvOrderId = findViewById(R.id.tvOrderId)
        tvEmail = findViewById(R.id.tvEmail)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        rvItems = findViewById(R.id.recyclerItems)
        btnDownload = findViewById(R.id.btnDownloadReceipt)

        rvItems.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapterReadOnly(cartItems)
        rvItems.adapter = adapter

        val orderId = intent.getStringExtra("orderId") ?: return
        loadOrderDetails(orderId)

        btnDownload.setOnClickListener { checkPermissionAndDownload() }
    }

    private fun loadOrderDetails(orderId: String) {
        // Remove previous listener if any
        orderListener?.remove()

        // Real-time Firestore listener
        orderListener = db.collection("orders").document(orderId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load order details", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (doc == null || !doc.exists()) return@addSnapshotListener

                val order = doc.toObject(Order::class.java) ?: return@addSnapshotListener
                currentOrder = order

                // Update UI immediately
                tvReceiverName.text = "Receiver: ${order.receiverName}"
                tvAddress.text = "Address: ${order.address}"
                tvPhone.text = "Phone: ${order.phone}"
                tvCustomerName.text = "Customer: ${order.customerName}"
                tvStatus.text = "Status: ${order.status}"
                tvTotal.text = "Total: ₹${order.totalPrice}"
                tvOrderId.text = "Order ID: ${order.orderId}"
                tvEmail.text = "Email ID: ${order.userEmail}"
                val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                tvTimestamp.text = "Ordered On: ${sdf.format(Date(order.timestamp))}"

                cartItems.clear()
                cartItems.addAll(order.items)
                adapter.notifyDataSetChanged()
            }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_REQ)
            return
        }
        generateAndSavePdf()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_REQ && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            generateAndSavePdf()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAndSavePdf() {
        val order = currentOrder ?: return

        Thread {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Draw app logo as subtle background
            val logo = BitmapFactory.decodeResource(resources, R.drawable.app_logo)
            val scaledLogo = Bitmap.createScaledBitmap(logo, 595, 842, true)
            paint.alpha = 30
            canvas.drawBitmap(scaledLogo, 0f, 0f, paint)
            paint.alpha = 255

            var y = 40
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("Order Receipt", 200f, y.toFloat(), paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            y += 40
            canvas.drawText("Order ID: ${order.orderId}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("Customer: ${order.customerName}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("Receiver: ${order.receiverName}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("Phone: ${order.phone}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("Address: ${order.address}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("Status: ${order.status}", 40f, y.toFloat(), paint); y += 30

            paint.isFakeBoldText = true
            canvas.drawText("Items:", 40f, y.toFloat(), paint)
            paint.isFakeBoldText = false
            y += 20

            for (item in order.items) {
                val bitmap = loadBitmapFromUrl(item.imageUrl)
                bitmap?.let {
                    val scaled = Bitmap.createScaledBitmap(it, 60, 60, true)
                    canvas.drawBitmap(scaled, 40f, y.toFloat(), paint)
                }
                canvas.drawText(item.name, 110f, y + 20f, paint)
                canvas.drawText("Qty: ${item.quantity}", 110f, y + 40f, paint)
                canvas.drawText("₹${item.price}", 110f, y + 60f, paint)
                y += 80
            }

            y += 20
            paint.isFakeBoldText = true
            canvas.drawText("Total: ₹${order.totalPrice}", 40f, y.toFloat(), paint)

            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            y += 20
            paint.isFakeBoldText = false
            canvas.drawText("Ordered On: ${sdf.format(Date(order.timestamp))}", 40f, y.toFloat(), paint)

            pdf.finishPage(page)

            runOnUiThread { savePdf(pdf) }
        }.start()
    }

    private fun savePdf(pdf: PdfDocument) {
        val fileName = "Order_${System.currentTimeMillis()}.pdf"
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                val output = contentResolver.openOutputStream(uri!!)!!
                pdf.writeTo(output)
                output.close()
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                val output = FileOutputStream(file)
                pdf.writeTo(output)
                output.close()
                uri = Uri.fromFile(file)
            }

            Toast.makeText(this, "Receipt saved", Toast.LENGTH_SHORT).show()
            uri?.let { openPdf(it) }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "PDF failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdf.close()
        }
    }

    // Updated: Use chooser so user can select any app
    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            val chooser = Intent.createChooser(intent, "Open PDF with")
            startActivity(chooser)
        } catch (e: Exception) {
           // Toast.makeText(this, "No app found to open PDF", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val input = java.net.URL(url).openStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove Firestore listener to avoid memory leaks
        orderListener?.remove()
    }
}
