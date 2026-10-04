package com.sipekat.app.data.local;

/**
 * Cache sesi aplikasi — port dari Dart AppSessionCache.
 * Menyimpan seluruh daftar modul ke SharedPreferences agar
 * layar bisa langsung membaca tanpa menunggu jaringan.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\t\n\u0002\b\'\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010Z\u001a\u00020[J\u0010\u0010\\\u001a\u0004\u0018\u00010]2\u0006\u0010^\u001a\u00020\u000bJ\u000e\u0010_\u001a\u00020[2\u0006\u0010`\u001a\u00020YJ \u0010a\u001a\u00020[2\u0006\u0010b\u001a\u00020\u000b2\u0006\u0010c\u001a\u00020\u000b2\b\b\u0002\u0010d\u001a\u00020\u000bJ\u000e\u0010e\u001a\u00020[H\u0082@\u00a2\u0006\u0002\u0010fJ\u000e\u0010g\u001a\u00020[H\u0082@\u00a2\u0006\u0002\u0010fJ\u000e\u0010h\u001a\u00020[H\u0082@\u00a2\u0006\u0002\u0010fJ\u000e\u0010i\u001a\u00020[H\u0082@\u00a2\u0006\u0002\u0010fJ\u000e\u0010j\u001a\u00020[H\u0082@\u00a2\u0006\u0002\u0010fJ@\u0010k\u001a\u00020[2\u0006\u0010b\u001a\u00020\u000b2\u0006\u0010c\u001a\u00020\u000b2\b\b\u0002\u0010d\u001a\u00020\u000b2\u0016\b\u0002\u0010l\u001a\u0010\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020[\u0018\u00010mH\u0086@\u00a2\u0006\u0002\u0010nJ\u0006\u0010o\u001a\u00020[R \u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001c\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001a\u0010\u0010\u001a\u00020\u0011X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u000e\u0010\u0016\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0019\u001a\u00020\u00118F\u00a2\u0006\u0006\u001a\u0004\b\u0019\u0010\u0013R\u0011\u0010\u001a\u001a\u00020\u00118F\u00a2\u0006\u0006\u001a\u0004\b\u001a\u0010\u0013R\u001c\u0010\u001b\u001a\u0004\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001c\u0010\r\"\u0004\b\u001d\u0010\u000fR \u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u001f0\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b \u0010\u0007\"\u0004\b!\u0010\tR\u001a\u0010\"\u001a\u00020\u0011X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b#\u0010\u0013\"\u0004\b$\u0010\u0015R\u001e\u0010%\u001a\u0004\u0018\u00010&X\u0086\u000e\u00a2\u0006\u0010\n\u0002\u0010+\u001a\u0004\b\'\u0010(\"\u0004\b)\u0010*R \u0010,\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b-\u0010\u0007\"\u0004\b.\u0010\tR\u001c\u0010/\u001a\u0004\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b0\u0010\r\"\u0004\b1\u0010\u000fR\u001a\u00102\u001a\u00020\u0011X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b3\u0010\u0013\"\u0004\b4\u0010\u0015R\u001a\u00105\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b6\u0010\r\"\u0004\b7\u0010\u000fR\u001a\u00108\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b9\u0010\r\"\u0004\b:\u0010\u000fR\u001a\u0010;\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b<\u0010\r\"\u0004\b=\u0010\u000fR\u001c\u0010>\u001a\u0004\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b?\u0010\r\"\u0004\b@\u0010\u000fR \u0010A\u001a\b\u0012\u0004\u0012\u00020\u001f0\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bB\u0010\u0007\"\u0004\bC\u0010\tR\u001a\u0010D\u001a\u00020\u0011X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bE\u0010\u0013\"\u0004\bF\u0010\u0015R\u001c\u0010G\u001a\u0004\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bH\u0010\r\"\u0004\bI\u0010\u000fR\u001a\u0010J\u001a\u00020\u0011X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bK\u0010\u0013\"\u0004\bL\u0010\u0015R\u001c\u0010M\u001a\u0004\u0018\u00010NX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bO\u0010P\"\u0004\bQ\u0010RR \u0010S\u001a\b\u0012\u0004\u0012\u00020T0\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bU\u0010\u0007\"\u0004\bV\u0010\tR\u000e\u0010W\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010X\u001a\u00020YX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006p"}, d2 = {"Lcom/sipekat/app/data/local/SessionCache;", "", "()V", "dapellCustomers", "", "Lcom/sipekat/app/data/model/Customer;", "getDapellCustomers", "()Ljava/util/List;", "setDapellCustomers", "(Ljava/util/List;)V", "dapellError", "", "getDapellError", "()Ljava/lang/String;", "setDapellError", "(Ljava/lang/String;)V", "dapellLoading", "", "getDapellLoading", "()Z", "setDapellLoading", "(Z)V", "everPreloaded", "gson", "Lcom/google/gson/Gson;", "isPreloaded", "isPreloading", "kunjunganError", "getKunjunganError", "setKunjunganError", "kunjunganList", "Lcom/sipekat/app/data/model/VisitCustomer;", "getKunjunganList", "setKunjunganList", "kunjunganLoading", "getKunjunganLoading", "setKunjunganLoading", "lastLoadedAt", "", "getLastLoadedAt", "()Ljava/lang/Long;", "setLastLoadedAt", "(Ljava/lang/Long;)V", "Ljava/lang/Long;", "ocrCustomers", "getOcrCustomers", "setOcrCustomers", "ocrError", "getOcrError", "setOcrError", "ocrLoading", "getOcrLoading", "setOcrLoading", "officerEmail", "getOfficerEmail", "setOfficerEmail", "officerNama", "getOfficerNama", "setOfficerNama", "officerRole", "getOfficerRole", "setOfficerRole", "pembukaanError", "getPembukaanError", "setPembukaanError", "pembukaanList", "getPembukaanList", "setPembukaanList", "pembukaanLoading", "getPembukaanLoading", "setPembukaanLoading", "perbaikanError", "getPerbaikanError", "setPerbaikanError", "perbaikanLoading", "getPerbaikanLoading", "setPerbaikanLoading", "perbaikanOptions", "Lcom/sipekat/app/data/model/DropdownOptions;", "getPerbaikanOptions", "()Lcom/sipekat/app/data/model/DropdownOptions;", "setPerbaikanOptions", "(Lcom/sipekat/app/data/model/DropdownOptions;)V", "perbaikanTickets", "Lcom/sipekat/app/data/model/Ticket;", "getPerbaikanTickets", "setPerbaikanTickets", "preloading", "sm", "Lcom/sipekat/app/data/local/SessionManager;", "clear", "", "findCustomerById", "Lcom/sipekat/app/data/local/CachedCustomerMatch;", "rawId", "init", "sessionManager", "initFromLocal", "nama", "email", "role", "loadDapell", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadKunjungan", "loadOcr", "loadPembukaan", "loadPerbaikan", "preloadAll", "onProgress", "Lkotlin/Function1;", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveToLocal", "app_debug"})
public final class SessionCache {
    private static com.sipekat.app.data.local.SessionManager sm;
    @org.jetbrains.annotations.NotNull()
    private static final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private static java.lang.String officerNama = "";
    @org.jetbrains.annotations.NotNull()
    private static java.lang.String officerEmail = "";
    @org.jetbrains.annotations.NotNull()
    private static java.lang.String officerRole = "petugas";
    @org.jetbrains.annotations.Nullable()
    private static com.sipekat.app.data.model.DropdownOptions perbaikanOptions;
    @org.jetbrains.annotations.NotNull()
    private static java.util.List<com.sipekat.app.data.model.Ticket> perbaikanTickets;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String perbaikanError;
    private static boolean perbaikanLoading = false;
    @org.jetbrains.annotations.NotNull()
    private static java.util.List<com.sipekat.app.data.model.VisitCustomer> kunjunganList;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String kunjunganError;
    private static boolean kunjunganLoading = false;
    @org.jetbrains.annotations.NotNull()
    private static java.util.List<com.sipekat.app.data.model.VisitCustomer> pembukaanList;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String pembukaanError;
    private static boolean pembukaanLoading = false;
    @org.jetbrains.annotations.NotNull()
    private static java.util.List<com.sipekat.app.data.model.Customer> ocrCustomers;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String ocrError;
    private static boolean ocrLoading = false;
    @org.jetbrains.annotations.NotNull()
    private static java.util.List<com.sipekat.app.data.model.Customer> dapellCustomers;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String dapellError;
    private static boolean dapellLoading = false;
    private static boolean everPreloaded = false;
    private static boolean preloading = false;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.Long lastLoadedAt;
    @org.jetbrains.annotations.NotNull()
    public static final com.sipekat.app.data.local.SessionCache INSTANCE = null;
    
    private SessionCache() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOfficerNama() {
        return null;
    }
    
    public final void setOfficerNama(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOfficerEmail() {
        return null;
    }
    
    public final void setOfficerEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOfficerRole() {
        return null;
    }
    
    public final void setOfficerRole(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.sipekat.app.data.model.DropdownOptions getPerbaikanOptions() {
        return null;
    }
    
    public final void setPerbaikanOptions(@org.jetbrains.annotations.Nullable()
    com.sipekat.app.data.model.DropdownOptions p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.sipekat.app.data.model.Ticket> getPerbaikanTickets() {
        return null;
    }
    
    public final void setPerbaikanTickets(@org.jetbrains.annotations.NotNull()
    java.util.List<com.sipekat.app.data.model.Ticket> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPerbaikanError() {
        return null;
    }
    
    public final void setPerbaikanError(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean getPerbaikanLoading() {
        return false;
    }
    
    public final void setPerbaikanLoading(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.sipekat.app.data.model.VisitCustomer> getKunjunganList() {
        return null;
    }
    
    public final void setKunjunganList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.sipekat.app.data.model.VisitCustomer> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getKunjunganError() {
        return null;
    }
    
    public final void setKunjunganError(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean getKunjunganLoading() {
        return false;
    }
    
    public final void setKunjunganLoading(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.sipekat.app.data.model.VisitCustomer> getPembukaanList() {
        return null;
    }
    
    public final void setPembukaanList(@org.jetbrains.annotations.NotNull()
    java.util.List<com.sipekat.app.data.model.VisitCustomer> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPembukaanError() {
        return null;
    }
    
    public final void setPembukaanError(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean getPembukaanLoading() {
        return false;
    }
    
    public final void setPembukaanLoading(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.sipekat.app.data.model.Customer> getOcrCustomers() {
        return null;
    }
    
    public final void setOcrCustomers(@org.jetbrains.annotations.NotNull()
    java.util.List<com.sipekat.app.data.model.Customer> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getOcrError() {
        return null;
    }
    
    public final void setOcrError(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean getOcrLoading() {
        return false;
    }
    
    public final void setOcrLoading(boolean p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.sipekat.app.data.model.Customer> getDapellCustomers() {
        return null;
    }
    
    public final void setDapellCustomers(@org.jetbrains.annotations.NotNull()
    java.util.List<com.sipekat.app.data.model.Customer> p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDapellError() {
        return null;
    }
    
    public final void setDapellError(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean getDapellLoading() {
        return false;
    }
    
    public final void setDapellLoading(boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastLoadedAt() {
        return null;
    }
    
    public final void setLastLoadedAt(@org.jetbrains.annotations.Nullable()
    java.lang.Long p0) {
    }
    
    public final boolean isPreloaded() {
        return false;
    }
    
    public final boolean isPreloading() {
        return false;
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    com.sipekat.app.data.local.SessionManager sessionManager) {
    }
    
    public final void initFromLocal(@org.jetbrains.annotations.NotNull()
    java.lang.String nama, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String role) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object preloadAll(@org.jetbrains.annotations.NotNull()
    java.lang.String nama, @org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String role, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onProgress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object loadPerbaikan(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object loadKunjungan(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object loadPembukaan(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object loadOcr(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object loadDapell(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void saveToLocal() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.sipekat.app.data.local.CachedCustomerMatch findCustomerById(@org.jetbrains.annotations.NotNull()
    java.lang.String rawId) {
        return null;
    }
    
    public final void clear() {
    }
}