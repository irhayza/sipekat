package com.sipekat.app.ui.kunjungan;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000bH\u0002J\b\u0010\u000f\u001a\u00020\rH\u0002J\u0012\u0010\u0010\u001a\u00020\r2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0014J\b\u0010\u0013\u001a\u00020\rH\u0002J\b\u0010\u0014\u001a\u00020\rH\u0002J\b\u0010\u0015\u001a\u00020\rH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/sipekat/app/ui/kunjungan/VisitListActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/sipekat/app/ui/kunjungan/VisitCustomerAdapter;", "allCustomers", "", "Lcom/sipekat/app/data/model/VisitCustomer;", "binding", "Lcom/sipekat/app/databinding/ActivityVisitListBinding;", "tipe", "", "filter", "", "query", "loadData", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "setupRecyclerView", "setupSearch", "setupToolbar", "app_debug"})
public final class VisitListActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.sipekat.app.databinding.ActivityVisitListBinding binding;
    private com.sipekat.app.ui.kunjungan.VisitCustomerAdapter adapter;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.sipekat.app.data.model.VisitCustomer> allCustomers;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String tipe = "";
    
    public VisitListActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupToolbar() {
    }
    
    private final void setupRecyclerView() {
    }
    
    private final void setupSearch() {
    }
    
    private final void loadData() {
    }
    
    private final void filter(java.lang.String query) {
    }
}