package com.sipekat.app.ui.ocr;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002JO\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u000f2\b\u0010\u0016\u001a\u0004\u0018\u00010\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u000f\u00a2\u0006\u0002\u0010\u001aR\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u001b"}, d2 = {"Lcom/sipekat/app/ui/ocr/MeterReadingViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "_state", "Landroidx/lifecycle/MutableLiveData;", "Lcom/sipekat/app/ui/ocr/MeterReadingState;", "repo", "Lcom/sipekat/app/data/repository/OcrRepository;", "state", "Landroidx/lifecycle/LiveData;", "getState", "()Landroidx/lifecycle/LiveData;", "submitReading", "", "customerId", "", "petugasNama", "stand", "", "photoFile", "Ljava/io/File;", "catatan", "lat", "", "lng", "bulan", "(Ljava/lang/String;Ljava/lang/String;ILjava/io/File;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/String;)V", "app_debug"})
public final class MeterReadingViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.sipekat.app.data.repository.OcrRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.sipekat.app.ui.ocr.MeterReadingState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<com.sipekat.app.ui.ocr.MeterReadingState> state = null;
    
    public MeterReadingViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.sipekat.app.ui.ocr.MeterReadingState> getState() {
        return null;
    }
    
    public final void submitReading(@org.jetbrains.annotations.NotNull()
    java.lang.String customerId, @org.jetbrains.annotations.NotNull()
    java.lang.String petugasNama, int stand, @org.jetbrains.annotations.NotNull()
    java.io.File photoFile, @org.jetbrains.annotations.NotNull()
    java.lang.String catatan, @org.jetbrains.annotations.Nullable()
    java.lang.Double lat, @org.jetbrains.annotations.Nullable()
    java.lang.Double lng, @org.jetbrains.annotations.NotNull()
    java.lang.String bulan) {
    }
}