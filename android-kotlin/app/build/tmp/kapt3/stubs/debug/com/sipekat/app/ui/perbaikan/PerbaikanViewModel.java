package com.sipekat.app.ui.perbaikan;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\r\n\u0002\u0010\u0006\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u0005H\u0002J\u0010\u0010\u001d\u001a\u00020\u001b2\b\b\u0002\u0010\u001e\u001a\u00020\u001fJ\u0006\u0010 \u001a\u00020\u001bJ\u0006\u0010!\u001a\u00020\u001bJ\u000e\u0010\"\u001a\u00020\u001b2\u0006\u0010#\u001a\u00020\u0007J\u000e\u0010$\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u0005J[\u0010%\u001a\u00020\u001b2\u0006\u0010#\u001a\u00020\u00072\u0006\u0010&\u001a\u00020\u00052\u0006\u0010\'\u001a\u00020\u00052\u0006\u0010(\u001a\u00020\u00052\u0006\u0010)\u001a\u00020\u00052\b\u0010*\u001a\u0004\u0018\u00010\u00052\b\u0010+\u001a\u0004\u0018\u00010\u00052\b\u0010,\u001a\u0004\u0018\u00010-2\b\u0010.\u001a\u0004\u0018\u00010-\u00a2\u0006\u0002\u0010/R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0006\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00070\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00050\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0019\u0010\u0014\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0013R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\t0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0013R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0013\u00a8\u00060"}, d2 = {"Lcom/sipekat/app/ui/perbaikan/PerbaikanViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "_searchQuery", "Landroidx/lifecycle/MutableLiveData;", "", "_selectedTicket", "Lcom/sipekat/app/data/model/Ticket;", "_submitState", "Lcom/sipekat/app/ui/perbaikan/SubmitState;", "_uiState", "Lcom/sipekat/app/ui/perbaikan/PerbaikanUiState;", "allTickets", "", "repository", "Lcom/sipekat/app/data/repository/PerbaikanRepository;", "searchQuery", "Landroidx/lifecycle/LiveData;", "getSearchQuery", "()Landroidx/lifecycle/LiveData;", "selectedTicket", "getSelectedTicket", "submitState", "getSubmitState", "uiState", "getUiState", "filterTickets", "", "query", "loadData", "forceRefresh", "", "refresh", "resetSubmitState", "selectTicket", "ticket", "setSearchQuery", "submitPenyelesaian", "jenis", "penanganan", "petugasNama", "tindakan", "fotoSebBase64", "fotoSesBase64", "lat", "", "lng", "(Lcom/sipekat/app/data/model/Ticket;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;)V", "app_debug"})
public final class PerbaikanViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.sipekat.app.data.repository.PerbaikanRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.sipekat.app.ui.perbaikan.PerbaikanUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<com.sipekat.app.ui.perbaikan.PerbaikanUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.sipekat.app.ui.perbaikan.SubmitState> _submitState = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<com.sipekat.app.ui.perbaikan.SubmitState> submitState = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.sipekat.app.data.model.Ticket> _selectedTicket = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<com.sipekat.app.data.model.Ticket> selectedTicket = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<java.lang.String> _searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<java.lang.String> searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.sipekat.app.data.model.Ticket> allTickets;
    
    public PerbaikanViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.sipekat.app.ui.perbaikan.PerbaikanUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.sipekat.app.ui.perbaikan.SubmitState> getSubmitState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.sipekat.app.data.model.Ticket> getSelectedTicket() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<java.lang.String> getSearchQuery() {
        return null;
    }
    
    public final void loadData(boolean forceRefresh) {
    }
    
    public final void refresh() {
    }
    
    public final void selectTicket(@org.jetbrains.annotations.NotNull()
    com.sipekat.app.data.model.Ticket ticket) {
    }
    
    public final void setSearchQuery(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    private final void filterTickets(java.lang.String query) {
    }
    
    public final void submitPenyelesaian(@org.jetbrains.annotations.NotNull()
    com.sipekat.app.data.model.Ticket ticket, @org.jetbrains.annotations.NotNull()
    java.lang.String jenis, @org.jetbrains.annotations.NotNull()
    java.lang.String penanganan, @org.jetbrains.annotations.NotNull()
    java.lang.String petugasNama, @org.jetbrains.annotations.NotNull()
    java.lang.String tindakan, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoSebBase64, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoSesBase64, @org.jetbrains.annotations.Nullable()
    java.lang.Double lat, @org.jetbrains.annotations.Nullable()
    java.lang.Double lng) {
    }
    
    public final void resetSubmitState() {
    }
}