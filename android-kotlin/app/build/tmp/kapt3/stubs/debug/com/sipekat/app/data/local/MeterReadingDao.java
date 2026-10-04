package com.sipekat.app.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000b\n\u0002\u0010\b\n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\bH\'J\u0018\u0010\n\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u000b\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u001c\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u000f\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00050\tH\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u0016\u0010\u0012\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J*\u0010\u0013\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\u0014\u001a\u0004\u0018\u00010\f2\b\u0010\u0015\u001a\u0004\u0018\u00010\fH\u00a7@\u00a2\u0006\u0002\u0010\u0016J\u000e\u0010\u0017\u001a\u00020\u0018H\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u0016\u0010\u0019\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u001a"}, d2 = {"Lcom/sipekat/app/data/local/MeterReadingDao;", "", "delete", "", "reading", "Lcom/sipekat/app/data/model/MeterReading;", "(Lcom/sipekat/app/data/model/MeterReading;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllFlow", "Lkotlinx/coroutines/flow/Flow;", "", "getById", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByPelanggan", "pelangganId", "getPendingSync", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insert", "markSynced", "fotoUrl", "driveId", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pendingCount", "", "update", "app_debug"})
@androidx.room.Dao()
public abstract interface MeterReadingDao {
    
    @androidx.room.Query(value = "SELECT * FROM meter_readings ORDER BY createdAt DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.sipekat.app.data.model.MeterReading>> getAllFlow();
    
    @androidx.room.Query(value = "SELECT * FROM meter_readings WHERE pelangganId = :pelangganId ORDER BY createdAt DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByPelanggan(@org.jetbrains.annotations.NotNull()
    java.lang.String pelangganId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.sipekat.app.data.model.MeterReading>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM meter_readings WHERE isSynced = 0 ORDER BY createdAt ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPendingSync(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.sipekat.app.data.model.MeterReading>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM meter_readings WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.sipekat.app.data.model.MeterReading> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.sipekat.app.data.model.MeterReading reading, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull()
    com.sipekat.app.data.model.MeterReading reading, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.sipekat.app.data.model.MeterReading reading, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE meter_readings SET isSynced = 1, fotoUrl = :fotoUrl, fotoDriveId = :driveId WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markSynced(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String driveId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM meter_readings WHERE isSynced = 0")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object pendingCount(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
}