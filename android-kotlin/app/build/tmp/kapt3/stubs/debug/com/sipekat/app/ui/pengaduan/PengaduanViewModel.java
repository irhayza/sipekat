package com.sipekat.app.ui.pengaduan;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u001c\u0010\u0011\u001a\u00020\r2\u0014\u0010\u0012\u001a\u0010\u0012\u0004\u0012\u00020\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u00140\u0013R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0015"}, d2 = {"Lcom/sipekat/app/ui/pengaduan/PengaduanViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "_state", "Landroidx/lifecycle/MutableLiveData;", "Lcom/sipekat/app/ui/pengaduan/PengaduanUiState;", "repo", "Lcom/sipekat/app/data/repository/PengaduanRepository;", "state", "Landroidx/lifecycle/LiveData;", "getState", "()Landroidx/lifecycle/LiveData;", "resetState", "", "searchCustomer", "idpel", "", "submitComplaint", "payload", "", "", "app_debug"})
public final class PengaduanViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.sipekat.app.data.repository.PengaduanRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.sipekat.app.ui.pengaduan.PengaduanUiState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<com.sipekat.app.ui.pengaduan.PengaduanUiState> state = null;
    
    public PengaduanViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.sipekat.app.ui.pengaduan.PengaduanUiState> getState() {
        return null;
    }
    
    public final void searchCustomer(@org.jetbrains.annotations.NotNull()
    java.lang.String idpel) {
    }
    
    public final void submitComplaint(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> payload) {
    }
    
    public final void resetState() {
    }
}