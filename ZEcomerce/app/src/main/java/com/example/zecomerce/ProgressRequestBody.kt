package com.example.zecomerce

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: (Int) -> Unit
) : RequestBody() {

    override fun contentType() = contentType.toMediaTypeOrNull()
    override fun contentLength() = file.length()

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val input = FileInputStream(file)
        var uploaded = 0L
        val total = contentLength()

        input.use { stream ->
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                val progress = (100 * uploaded / total).toInt()
                onProgress(progress)
            }
        }
    }
}

