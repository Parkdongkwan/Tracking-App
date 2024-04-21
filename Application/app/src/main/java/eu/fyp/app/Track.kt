package eu.fyp.app


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import eu.fyp.app.databinding.ActivityTrackBinding
import eu.fyp.app.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class Track : AppCompatActivity() {

    private lateinit var binding: ActivityTrackBinding

    private lateinit var gestureDetector: GestureDetectorCompat

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { binding.viewImage.setImageBitmap(it) }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { binding.viewImage.setImageURI(it) }
            ?: Toast.makeText(this, "Failed to pick image", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 102
        private const val GALLERY_PERMISSION_CODE = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupButtonListeners()
        gestureDetector = GestureDetectorCompat(this, SwipeGestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            e1 ?: return false
            e2 ?: return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        return true
                    } else {
                        // Swipe left
                        startActivity(Intent(this@Track, Track2::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

                        return true
                    }
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private fun setupButtonListeners() {
        binding.takePhotoButton.setOnClickListener { checkPermissionAndCaptureImage() }
        binding.choosePhotoButton.setOnClickListener { checkPermissionAndPickImage() }
        binding.trackRecordButton.setOnClickListener { processImageForTracking() }
    }

    private fun checkPermissionAndCaptureImage() {
        if (hasPermission(Manifest.permission.CAMERA)) {
            dispatchTakePictureIntent()
        } else {
            requestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
        }
    }


    private fun checkPermissionAndPickImage() {
        if (hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
            dispatchPickImageIntent()
        } else {
            requestPermission(Manifest.permission.READ_MEDIA_IMAGES, GALLERY_PERMISSION_CODE)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun dispatchPickImageIntent() {
        pickImageLauncher.launch("image/*")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                CAMERA_PERMISSION_CODE -> dispatchTakePictureIntent()
                GALLERY_PERMISSION_CODE -> dispatchPickImageIntent()
            }
        } else {
            Toast.makeText(this, "Permission is required for this action", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImageForTracking() {
        val drawable = binding.viewImage.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap

            // Assuming TensorFlow Lite Support Library is used for image processing
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(299, 299, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)

            // Apply preprocessing defined by imageProcessor to tensorImage
            tensorImage = imageProcessor.process(tensorImage)

            // Continue with your model loading and inference as before
            val model = Model.newInstance(this)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 299, 299, 3), DataType.FLOAT32).apply {
                loadBuffer(tensorImage.buffer)
            }

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

            var maxIdx = outputFeature0.indices.maxByOrNull { outputFeature0[it] } ?: -1

            model.close()

            // Handle the display of results
            PopupHelper.showPopupWindow(this, binding.trackRecordButton, maxIdx)
        } else {
            Toast.makeText(this, "Selected image cannot be processed", Toast.LENGTH_SHORT).show()
        }
    }
}

