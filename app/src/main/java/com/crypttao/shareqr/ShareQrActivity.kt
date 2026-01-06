package com.crypttao.shareqr

import android.graphics.Bitmap
import android.graphics.Color
// import android.graphics.RenderEffect
// import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
// import androidx.core.view.ViewCompat
import com.crypttao.shareqr.databinding.ActivityShareQrBinding
import kotlinx.coroutines.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

class ShareQrActivity : ComponentActivity() {
    private lateinit var binding: ActivityShareQrBinding
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启用过渡动画更丝滑（可选）
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
        binding = ActivityShareQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make background transparent and dim
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.decorView.setBackgroundColor(Color.TRANSPARENT)
        // 禁用系统可能的模糊/模态效果，仅使用我们自己的遮罩
        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        // Apply blur behind on Android 12L+ (API 32) or 12 (API 31)
        // 背景高斯模糊（仅对遮罩层生效），不影响对话框与二维码
        val blurLayer = findViewById<View>(R.id.blurLayer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blur = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
            val effect = android.graphics.RenderEffect.createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP)
            blurLayer.setRenderEffect(effect)
            blurLayer.setBackgroundColor(Color.parseColor("#66000000"))
        } else {
            // 低版本使用较深的半透明遮罩
            blurLayer.setBackgroundColor(Color.parseColor("#80000000"))
        }

        // Get shared content
        val sharedText = extractSharedText().also { lastSharedText = it }
        binding.progress.visibility = View.VISIBLE
        if (sharedText.isNullOrBlank()) {
            android.widget.Toast.makeText(this, "无法识别分享内容", android.widget.Toast.LENGTH_SHORT).show()
            binding.tvCopied.text = ""
        } else {

            // 自动复制到剪贴板并提示
            try {
                val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("ShareQR", sharedText)
                cm.setPrimaryClip(clip)
                android.widget.Toast.makeText(this, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
            // 展示复制的内容
            binding.tvCopied.text = sharedText
            // 后台生成二维码，加速并避免主线程阻塞
            generateQrAsync(sharedText)
        }

        // 点击背景模糊层关闭弹窗
        findViewById<View>(R.id.blurLayer).setOnClickListener { finish() }

        // 已移除“复制到剪贴板”按钮；此处仅保留保存二维码按钮
        binding.btnSave.setOnClickListener {
            qrBitmap?.let { bmp ->
                val uri = saveBitmapToPictures(bmp)
                if (uri != null) {
                    binding.btnSave.text = "已保存"
                    android.widget.Toast.makeText(this, "已保存到相册", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this, "保存失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 取消关闭按钮（按需可通过返回键关闭）
    }

    private var lastSharedText: String? = null

    private fun extractSharedText(): String? {
        val intent = intent
        if (intent?.action == android.content.Intent.ACTION_SEND) {
            val type = intent.type ?: ""
            // Prefer EXTRA_TEXT
            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) return text

            // Try ClipData
            val cd = intent.clipData
            if (cd != null && cd.itemCount > 0) {
                val item = cd.getItemAt(0)
                val txt = item.text?.toString()
                if (!txt.isNullOrBlank()) return txt
                val uri = item.uri
                if (uri != null) return uri.toString()
            }

            // Fallback for data
            val data = intent.data
            if (data != null) return data.toString()
        }
        return null
    }

    private fun generateQr(content: String): Bitmap {
        val size = (resources.displayMetrics.widthPixels * 0.68).toInt().coerceAtLeast(240)
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        val black = Color.BLACK
        val white = Color.WHITE
        var index = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[index++] = if (matrix[x, y]) black else white
            }
        }
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    private fun generateQrAsync(content: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val bmp = generateQr(content)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                qrBitmap = bmp
                binding.ivQr.setImageBitmap(bmp)
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun saveBitmapToPictures(bitmap: Bitmap): Uri? {
        return try {
            val name = "ShareQR_${System.currentTimeMillis()}.png"
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = contentResolver
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(uri).use { out ->
                if (out == null) return null
                val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                if (!ok) return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            null
        }
    }
}
