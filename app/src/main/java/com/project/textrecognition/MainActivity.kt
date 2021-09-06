package com.project.textrecognition

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var imageView: ImageView
    private lateinit var btnCapture: ImageView
    private lateinit var txtView: TextView
    private val cameraRequest = 1888
    private lateinit var photoFile: File
    private var photoPath: String = "text"
    private lateinit var bitmap: Bitmap
    private lateinit var apkUri: Uri
    private lateinit var btnSpeak: Button
    lateinit var tts: TextToSpeech

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                cameraRequest
            )

            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            )
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1
                )

            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            )
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
                )
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Text Recognition"

        imageView = findViewById(R.id.imageView)
        txtView = findViewById(R.id.txtView)
        btnCapture = findViewById(R.id.btnCapture)
        btnSpeak = findViewById(R.id.btnSpeak)
        tts = TextToSpeech(this, this)
        txtView.movementMethod = ScrollingMovementMethod()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault(Locale.Category.FORMAT)).format(Date())
        val fileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(null).toString() + "/TextRecognition"
        val dir = File(storageDir)
        if (!dir.exists())
            dir.mkdir()
        photoFile = File("$storageDir/$fileName.jpg")
        photoPath = photoFile.absolutePath
        apkUri = FileProvider.getUriForFile(applicationContext,
            "$packageName.provider", photoFile)


        btnCapture.setOnClickListener {
            val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            camIntent.putExtra(MediaStore.EXTRA_OUTPUT, apkUri)
            resultLauncher.launch(camIntent)
        }
        btnSpeak.setOnClickListener {
            val text = txtView.text.toString()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setPic()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setPic() {
        var targetW = imageView.width
        var targetH = imageView.height
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true

        val photoH = bmOptions.outHeight
        val photoW = bmOptions.outWidth
        if (targetW == 0)
            targetW = 1
        if (targetH == 0)
            targetH = 1
        val scaleFactor = (photoW / targetW).coerceAtMost(photoH / targetH)
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor shl 1
        bitmap = BitmapFactory.decodeFile(photoPath, bmOptions)
        val matrix = Matrix()
        matrix.postRotate(90.0F)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        imageView.setImageBitmap(bitmap)
        txtView.text = detectText(bitmap)
    }

    private fun detectText(bitmap: Bitmap): String {
        val textRecognizer = TextRecognizer.Builder(this).build()
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val items = textRecognizer.detect(frame)
        val sb = StringBuilder()
        for (i in 0 until items.size()) {
            val myItem = items.valueAt(i)
            sb.append(myItem.value)
        }
        photoFile.delete()
        return sb.toString()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }
}

