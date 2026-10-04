package com.sipekat.app.ui.ocr

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sipekat.app.SiPekatApp
import com.sipekat.app.databinding.ActivityMeterReadingBinding
import com.sipekat.app.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MeterReadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeterReadingBinding
    private val viewModel: MeterReadingViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var photoFile: File? = null

    private var customerId = ""
    private var customerNama = ""
    private var standLalu = 0
    private var userNama = ""

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeterReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = intent.getStringExtra("customer_id") ?: ""
        customerNama = intent.getStringExtra("customer_nama") ?: ""
        standLalu = intent.getIntExtra("stand_awal", 0)
        
        val app = application as SiPekatApp
        userNama = app.sessionManager.sessionNama ?: ""

        setupToolbar()
        setupUI()
        setupListeners()
        observeViewModel()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        binding.tvNama.text = customerNama
        binding.tvIdpel.text = "ID: $customerId"
        binding.tvAlamat.text = intent.getStringExtra("customer_alamat") ?: "-"
        binding.tvStandLalu.text = standLalu.toString()
    }

    private fun setupListeners() {
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
        
        binding.btnRetake.setOnClickListener {
            photoFile?.delete()
            photoFile = null
            binding.ivPreview.visibility = View.GONE
            binding.viewFinder.visibility = View.VISIBLE
            binding.btnRetake.visibility = View.GONE
            binding.btnCapture.visibility = View.VISIBLE
            binding.etStandAngka.setText("")
        }

        binding.btnSubmit.setOnClickListener {
            val standStr = binding.etStandAngka.text.toString().trim()
            val catatan = binding.etCatatan.text.toString().trim()
            
            if (standStr.isEmpty()) {
                Toast.makeText(this, "Stand angka wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (photoFile == null) {
                Toast.makeText(this, "Foto meter wajib diambil", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val stand = standStr.toIntOrNull() ?: 0
            
            // Konfirmasi validasi logika stand meter (misal tidak boleh lebih kecil)
            if (stand < standLalu) {
                AlertDialog.Builder(this)
                    .setTitle("Peringatan")
                    .setMessage("Stand yang dimasukkan ($stand) lebih KECIL dari stand bulan lalu ($standLalu). Yakin melanjutkan?")
                    .setPositiveButton("Lanjutkan") { _, _ -> submitData(stand, catatan) }
                    .setNegativeButton("Batal", null)
                    .show()
            } else {
                submitData(stand, catatan)
            }
        }
    }

    private fun submitData(stand: Int, catatan: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val loc = LocationHelper.getCurrentLocation(this@MeterReadingActivity, 5000)
            val bulan = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            viewModel.submitReading(
                customerId, userNama, stand, photoFile!!, catatan, loc.lat, loc.lng, bulan
            )
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is MeterReadingState.Submitting -> {
                    binding.progressSubmit.visibility = View.VISIBLE
                    binding.btnSubmit.isEnabled = false
                }
                is MeterReadingState.Success -> {
                    binding.progressSubmit.visibility = View.GONE
                    Toast.makeText(this, "Data berhasil dikirim!", Toast.LENGTH_LONG).show()
                    finish()
                }
                is MeterReadingState.Error -> {
                    binding.progressSubmit.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Gagal membuka kamera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Gagal ambil foto: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    this@MeterReadingActivity.photoFile = photoFile
                    val savedUri = Uri.fromFile(photoFile)
                    
                    binding.viewFinder.visibility = View.INVISIBLE
                    binding.ivPreview.visibility = View.VISIBLE
                    binding.ivPreview.setImageURI(savedUri)
                    
                    binding.btnCapture.visibility = View.GONE
                    binding.btnRetake.visibility = View.VISIBLE
                    
                    recognizeText(savedUri)
                }
            })
    }
    
    private fun recognizeText(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    val numbersOnly = resultText.replace(Regex("[^0-9]"), "")
                    if (numbersOnly.isNotEmpty()) {
                        // Coba temukan 5 digit angka yang paling masuk akal (sesuai spesifikasi meteran)
                        val match = Regex("\\b\\d{4,5}\\b").find(numbersOnly)
                        if (match != null) {
                            binding.etStandAngka.setText(match.value)
                        } else {
                            binding.etStandAngka.setText(numbersOnly.take(5))
                        }
                    }
                }
                .addOnFailureListener {
                    // Jika gagal baca, biarkan manual
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
