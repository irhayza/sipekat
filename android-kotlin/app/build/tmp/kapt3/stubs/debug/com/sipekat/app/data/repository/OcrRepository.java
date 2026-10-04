package com.sipekat.app.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u0086@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\b\u001a\u00020\tH\u0086@\u00a2\u0006\u0002\u0010\nJ\u0010\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\tH\u0002J2\u0010\r\u001a\u0010\u0012\u0004\u0012\u00020\t\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u000e2\u0014\u0010\u000f\u001a\u0010\u0012\u0004\u0012\u00020\t\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u000eH\u0086@\u00a2\u0006\u0002\u0010\u0010J\u0010\u0010\u0011\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\tH\u0002\u00a8\u0006\u0013"}, d2 = {"Lcom/sipekat/app/data/repository/OcrRepository;", "", "()V", "getAllDapellCustomers", "", "Lcom/sipekat/app/data/model/Customer;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAssignedCustomers", "petugasNama", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "norm", "v", "postToOcr", "", "payload", "(Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sheetUrl", "sheetName", "app_debug"})
public final class OcrRepository {
    
    public OcrRepository() {
        super();
    }
    
    private final java.lang.String sheetUrl(java.lang.String sheetName) {
        return null;
    }
    
    private final java.lang.String norm(java.lang.String v) {
        return null;
    }
    
    /**
     * Pelanggan yang di-assign ke petugas tertentu (untuk modul Pencatatan Meter)
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getAssignedCustomers(@org.jetbrains.annotations.NotNull()
    java.lang.String petugasNama, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.sipekat.app.data.model.Customer>> $completion) {
        return null;
    }
    
    /**
     * Seluruh pelanggan dapell — untuk auto-fill lintas modul
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getAllDapellCustomers(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.sipekat.app.data.model.Customer>> $completion) {
        return null;
    }
    
    /**
     * POST ke OCR bridge (upload baca meter)
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object postToOcr(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> payload, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion) {
        return null;
    }
}