package com.example.onetechbs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.onetechbs.ui.custom.CropView
import java.io.File
import java.io.FileOutputStream

class CropActivity : AppCompatActivity() {

    private lateinit var cropView: CropView

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_CROPPED_URI = "extra_cropped_uri"

        fun newIntent(context: Context, uri: Uri): Intent {
            val intent = Intent(context, CropActivity::class.java)
            intent.putExtra(EXTRA_URI, uri)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        cropView = findViewById(R.id.crop_view)
        val buttonCancel = findViewById<Button>(R.id.button_cancel)
        val buttonDone = findViewById<Button>(R.id.button_done)

        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_URI)
        if (imageUri == null) {
            finish()
            return
        }

        cropView.setImageUri(imageUri)

        buttonCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        buttonDone.setOnClickListener {
            val croppedBitmap = cropView.getCroppedImage()
            val croppedUri = saveBitmapAndGetUri(croppedBitmap)
            val resultIntent = Intent().apply {
                putExtra(EXTRA_CROPPED_URI, croppedUri)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun saveBitmapAndGetUri(bitmap: Bitmap): Uri {
        val tempFile = File.createTempFile("cropped_", ".jpeg", cacheDir)
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(tempFile)
    }
}
