package com.sipekat.app.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\r\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J8\u0010\u0003\u001a\u0010\u0012\u0004\u0012\u00020\u0005\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u00042\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\u00052\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0005H\u0086@\u00a2\u0006\u0002\u0010\tJ$\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\u0006\u0010\r\u001a\u00020\u00052\u0006\u0010\u000e\u001a\u00020\u0005H\u0086@\u00a2\u0006\u0002\u0010\u000fJ\u000e\u0010\u0010\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J$\u0010\u0013\u001a\u00020\u00142\u0014\u0010\u0015\u001a\u0010\u0012\u0004\u0012\u00020\u0005\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u0004H\u0086@\u00a2\u0006\u0002\u0010\u0016J2\u0010\u0017\u001a\u0010\u0012\u0004\u0012\u00020\u0005\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u00042\u0014\u0010\u0018\u001a\u0010\u0012\u0004\u0012\u00020\u0005\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u0004H\u0082@\u00a2\u0006\u0002\u0010\u0016JX\u0010\u0019\u001a\u00020\u00142\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\u00052\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u001b\u001a\u0004\u0018\u00010\u00052\u0014\u0010\u001c\u001a\u0010\u0012\u0004\u0012\u00020\u0005\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u0004H\u0086@\u00a2\u0006\u0002\u0010\u001dJ\u001e\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u00052\u0006\u0010 \u001a\u00020\u0005H\u0086@\u00a2\u0006\u0002\u0010\u000f\u00a8\u0006!"}, d2 = {"Lcom/sipekat/app/data/repository/PerbaikanRepository;", "", "()V", "findRow", "", "", "sheetName", "keyValue", "keyColumn", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getActiveTicketsFull", "", "Lcom/sipekat/app/data/model/Ticket;", "officerEmail", "officerNama", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getDropdownOptions", "Lcom/sipekat/app/data/model/DropdownOptions;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "notifySelesai", "", "payload", "(Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "post", "body", "updateRowCells", "filterColumn", "filterValue", "updates", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "uploadPhoto", "base64", "filename", "app_debug"})
public final class PerbaikanRepository {
    
    public PerbaikanRepository() {
        super();
    }
    
    private final java.lang.Object post(java.util.Map<java.lang.String, ? extends java.lang.Object> body, kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getDropdownOptions(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.sipekat.app.data.model.DropdownOptions> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getActiveTicketsFull(@org.jetbrains.annotations.NotNull()
    java.lang.String officerEmail, @org.jetbrains.annotations.NotNull()
    java.lang.String officerNama, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.sipekat.app.data.model.Ticket>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object findRow(@org.jetbrains.annotations.NotNull()
    java.lang.String sheetName, @org.jetbrains.annotations.NotNull()
    java.lang.String keyValue, @org.jetbrains.annotations.Nullable()
    java.lang.String keyColumn, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateRowCells(@org.jetbrains.annotations.NotNull()
    java.lang.String sheetName, @org.jetbrains.annotations.NotNull()
    java.lang.String keyValue, @org.jetbrains.annotations.Nullable()
    java.lang.String keyColumn, @org.jetbrains.annotations.Nullable()
    java.lang.String filterColumn, @org.jetbrains.annotations.Nullable()
    java.lang.String filterValue, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> updates, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object uploadPhoto(@org.jetbrains.annotations.NotNull()
    java.lang.String base64, @org.jetbrains.annotations.NotNull()
    java.lang.String filename, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object notifySelesai(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> payload, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}