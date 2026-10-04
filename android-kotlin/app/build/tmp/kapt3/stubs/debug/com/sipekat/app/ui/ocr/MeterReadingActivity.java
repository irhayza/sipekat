package com.sipekat.app.ui.ocr;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0019\u001a\u00020\u001aH\u0002J\u0012\u0010\u001b\u001a\u00020\u001a2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0014J\b\u0010\u001e\u001a\u00020\u001aH\u0014J\u0010\u0010\u001f\u001a\u00020\u001a2\u0006\u0010 \u001a\u00020!H\u0002J\b\u0010\"\u001a\u00020\u001aH\u0002J\b\u0010#\u001a\u00020\u001aH\u0002J\b\u0010$\u001a\u00020\u001aH\u0002J\b\u0010%\u001a\u00020\u001aH\u0002J\u0018\u0010&\u001a\u00020\u001a2\u0006\u0010\'\u001a\u00020\u00112\u0006\u0010(\u001a\u00020\tH\u0002J\b\u0010)\u001a\u00020\u001aH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0013\u001a\u00020\u00148BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\u0018\u001a\u0004\b\u0015\u0010\u0016\u00a8\u0006*"}, d2 = {"Lcom/sipekat/app/ui/ocr/MeterReadingActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/sipekat/app/databinding/ActivityMeterReadingBinding;", "cameraExecutor", "Ljava/util/concurrent/ExecutorService;", "cameraPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "customerId", "customerNama", "imageCapture", "Landroidx/camera/core/ImageCapture;", "photoFile", "Ljava/io/File;", "standLalu", "", "userNama", "viewModel", "Lcom/sipekat/app/ui/ocr/MeterReadingViewModel;", "getViewModel", "()Lcom/sipekat/app/ui/ocr/MeterReadingViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "observeViewModel", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "recognizeText", "uri", "Landroid/net/Uri;", "setupListeners", "setupToolbar", "setupUI", "startCamera", "submitData", "stand", "catatan", "takePhoto", "app_debug"})
public final class MeterReadingActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.sipekat.app.databinding.ActivityMeterReadingBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.core.ImageCapture imageCapture;
    private java.util.concurrent.ExecutorService cameraExecutor;
    @org.jetbrains.annotations.Nullable()
    private java.io.File photoFile;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String customerId = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String customerNama = "";
    private int standLalu = 0;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String userNama = "";
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> cameraPermissionLauncher = null;
    
    public MeterReadingActivity() {
        super();
    }
    
    private final com.sipekat.app.ui.ocr.MeterReadingViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupToolbar() {
    }
    
    private final void setupUI() {
    }
    
    private final void setupListeners() {
    }
    
    private final void submitData(int stand, java.lang.String catatan) {
    }
    
    private final void observeViewModel() {
    }
    
    private final void startCamera() {
    }
    
    private final void takePhoto() {
    }
    
    private final void recognizeText(android.net.Uri uri) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}