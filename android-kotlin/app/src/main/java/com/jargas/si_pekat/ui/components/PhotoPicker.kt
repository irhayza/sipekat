package com.jargas.si_pekat.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.PhotoMetadata
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Foto yang sudah dipilih + waktu pengambilan (epoch millis). */
class PickedPhoto(val file: File, val name: String, val capturedAt: Long, val fromCamera: Boolean = false) {
    /** Cache hasil kompres base64 (diisi saat kirim agar percobaan ulang tidak mengompres lagi). */
    var base64: String? = null
}

private fun photosDir(context: Context): File = File(context.cacheDir, "photos").apply { mkdirs() }

private fun displayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
} catch (e: Exception) {
    null
}

/** Dua pemicu terpisah (kamera, galeri) — untuk layar yang punya tombol sendiri-sendiri. */
class PhotoSources(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberPhotoSources(onPicked: (PickedPhoto) -> Unit, onError: (String) -> Unit): PhotoSources {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun finish(file: File, name: String, fromCamera: Boolean) {
        if (file.length() > AppConfig.MAX_PHOTO_BYTES) {
            onError("Ukuran foto terlalu besar (maks 20 MB).")
            return
        }
        val readable = withContext(Dispatchers.IO) {
            val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, b)
            b.outWidth > 0 && b.outHeight > 0
        }
        if (!readable) {
            withContext(Dispatchers.IO) { file.delete() }
            onError("Format foto tidak didukung.")
            return
        }
        val at = PhotoMetadata.resolveCapturedTime(file, name)
        onPicked(PickedPhoto(file, name, at, fromCamera))
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = cameraPath
        if (ok && path != null) scope.launch { finish(File(path), File(path).name, true) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            try {
                val name = displayName(context, uri) ?: "galeri.jpg"
                val target = File(photosDir(context), "pick_${System.currentTimeMillis()}.jpg")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                        ?: throw IllegalStateException("Berkas tidak bisa dibuka")
                }
                finish(target, name, false)
            } catch (e: Exception) {
                onError("Tidak bisa mengambil foto: ${e.message ?: e}")
            }
        }
    }
    return remember(cameraLauncher, galleryLauncher) {
        PhotoSources(
            camera = {
                try {
                    val file = File(photosDir(context), "cam_${System.currentTimeMillis()}.jpg")
                    cameraPath = file.absolutePath
                    cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                } catch (e: Exception) {
                    onError("Tidak bisa membuka kamera: ${e.message ?: e}")
                }
            },
            gallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}

/** Pemilih foto dengan lembar pilihan Kamera/Galeri. Mengembalikan fungsi pemicu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberPhotoPicker(onPicked: (PickedPhoto) -> Unit, onError: (String) -> Unit): () -> Unit {
    val sources = rememberPhotoSources(onPicked, onError)
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = Color.White) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                SourceRow(Icons.Rounded.PhotoCamera, "Ambil dari Kamera") { showSheet = false; sources.camera() }
                SourceRow(Icons.Rounded.PhotoLibrary, "Pilih dari Galeri") { showSheet = false; sources.gallery() }
            }
        }
    }
    return { showSheet = true }
}

@Composable
private fun SourceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AppColors.Brand)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = AppColors.Ink)
    }
}

/** Decode berkas foto ke ImageBitmap ukuran layar (hemat memori), di luar thread utama. */
@Composable
fun rememberFileBitmap(file: File?, maxDim: Int = 1200): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, file?.absolutePath) {
        value = if (file == null) null else withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    return state.value
}

/** Petak foto 16:10 dengan cap waktu & tombol ganti. */
@Composable
fun PhotoCaptureTile(
    label: String,
    photo: PickedPhoto?,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ketuk untuk ambil foto",
) {
    Column(modifier) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(12.dp)).background(AppColors.Canvas).clickable(onClick = onCapture),
        ) {
            if (photo == null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PhotoCamera, null, tint = AppColors.Muted, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(placeholder, color = AppColors.Muted, fontSize = 12.sp)
                }
            } else {
                val bmp = rememberFileBitmap(photo.file)
                if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AccessTime, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(PhotoMetadata.formatShort(photo.capturedAt), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onCapture).padding(6.dp),
                ) { Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}
