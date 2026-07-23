package com.app.pakeplus

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.app.pakeplus.databinding.ActivityCameraBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * App 内自绘相机（CameraX）。不调用系统相机 App，规避华为/小米/OPPO 等 OEM 相机兼容性闪退。
 * 拍照结果以压缩后的 base64（data:image/jpeg;base64,...）通过 Intent extra "data" 回传，
 * 由 MainActivity 经 JsBridge 注入到 WebView 的 window.__onNativeCameraResult。
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var capturedBitmap: Bitmap? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
        binding.btnCancel.setOnClickListener { finishWith(null) }
        binding.btnShutter.setOnClickListener { takePhoto() }
        binding.btnRetake.setOnClickListener { retake() }
        binding.btnConfirm.setOnClickListener { confirm() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraActivity", "startCamera failed", e)
                Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show()
                finishWith(null)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "cam_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "takePhoto failed", exc)
                    Toast.makeText(this@CameraActivity, "拍照失败", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    try {
                        val raw = BitmapFactory.decodeFile(file.absolutePath)
                            ?: throw RuntimeException("decode null")
                        val corrected = rotateIfNeeded(raw, file.absolutePath)
                        capturedBitmap = corrected
                        binding.imgPreview.setImageBitmap(corrected)
                        binding.imgPreview.visibility = View.VISIBLE
                        binding.previewView.visibility = View.GONE
                        showResultControls(true)
                    } catch (e: Exception) {
                        Log.e("CameraActivity", "decode failed", e)
                        Toast.makeText(this@CameraActivity, "照片处理失败", Toast.LENGTH_SHORT).show()
                        finishWith(null)
                    }
                }
            }
        )
    }

    private fun retake() {
        capturedBitmap = null
        binding.imgPreview.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        showResultControls(false)
        startCamera()
    }

    private fun confirm() {
        val bmp = capturedBitmap ?: return finishWith(null)
        val scaled = scaleToMaxSide(bmp, 1280)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        finishWith("data:image/jpeg;base64,$b64")
    }

    private fun showResultControls(isResult: Boolean) {
        binding.btnShutter.visibility = if (isResult) View.GONE else View.VISIBLE
        binding.btnConfirm.visibility = if (isResult) View.VISIBLE else View.GONE
        binding.btnRetake.visibility = if (isResult) View.VISIBLE else View.GONE
    }

    /** 按 EXIF 方向校正旋转（前置/部分机型后置相机输出会带旋转角） */
    private fun rotateIfNeeded(src: Bitmap, path: String): Bitmap {
        return try {
            val ei = ExifInterface(path)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return src
            }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } catch (e: Exception) {
            Log.w("CameraActivity", "exif rotate failed, use raw", e)
            src
        }
    }

    private fun scaleToMaxSide(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longSide = maxOf(w, h)
        if (longSide <= maxSide) return src
        val ratio = maxSide.toFloat() / longSide
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    private fun finishWith(data: String?) {
        val intent = Intent()
        if (data != null) intent.putExtra("data", data)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
