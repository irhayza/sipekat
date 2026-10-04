package com.sipekat.app.data.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b*\b\u0087\b\u0018\u00002\u00020\u0001B\u009d\u0001\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\f\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0010\u001a\u00020\u0011\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0013\u0012\b\b\u0002\u0010\u0014\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\u0015J\t\u0010)\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010*\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010+\u001a\u00020\u0003H\u00c6\u0003J\t\u0010,\u001a\u00020\u0011H\u00c6\u0003J\t\u0010-\u001a\u00020\u0013H\u00c6\u0003J\t\u0010.\u001a\u00020\u0007H\u00c6\u0003J\t\u0010/\u001a\u00020\u0003H\u00c6\u0003J\t\u00100\u001a\u00020\u0003H\u00c6\u0003J\t\u00101\u001a\u00020\u0007H\u00c6\u0003J\u000b\u00102\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u00103\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u00104\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u00105\u001a\u0004\u0018\u00010\fH\u00c6\u0003\u00a2\u0006\u0002\u0010!J\u0010\u00106\u001a\u0004\u0018\u00010\fH\u00c6\u0003\u00a2\u0006\u0002\u0010!J\u00a6\u0001\u00107\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\f2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u000f\u001a\u00020\u00032\b\b\u0002\u0010\u0010\u001a\u00020\u00112\b\b\u0002\u0010\u0012\u001a\u00020\u00132\b\b\u0002\u0010\u0014\u001a\u00020\u0007H\u00c6\u0001\u00a2\u0006\u0002\u00108J\u0013\u00109\u001a\u00020\u00132\b\u0010:\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010;\u001a\u00020\u0007H\u00d6\u0001J\t\u0010<\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u000f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0017R\u0011\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0013\u0010\n\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0017R\u0013\u0010\b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0017R\u0013\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0017R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0017R\u0011\u0010\u0012\u001a\u00020\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u001fR\u0015\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\n\n\u0002\u0010\"\u001a\u0004\b \u0010!R\u0015\u0010\r\u001a\u0004\u0018\u00010\f\u00a2\u0006\n\n\u0002\u0010\"\u001a\u0004\b#\u0010!R\u0011\u0010\u0014\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u0017R\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u0017R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010%\u00a8\u0006="}, d2 = {"Lcom/sipekat/app/data/model/MeterReading;", "", "id", "", "pelangganId", "petugasId", "standAngka", "", "fotoPath", "fotoUrl", "fotoDriveId", "lat", "", "lng", "catatan", "bulan", "createdAt", "", "isSynced", "", "minus", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/String;Ljava/lang/String;JZI)V", "getBulan", "()Ljava/lang/String;", "getCatatan", "getCreatedAt", "()J", "getFotoDriveId", "getFotoPath", "getFotoUrl", "getId", "()Z", "getLat", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getLng", "getMinus", "()I", "getPelangganId", "getPetugasId", "getStandAngka", "component1", "component10", "component11", "component12", "component13", "component14", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/String;Ljava/lang/String;JZI)Lcom/sipekat/app/data/model/MeterReading;", "equals", "other", "hashCode", "toString", "app_debug"})
@androidx.room.Entity(tableName = "meter_readings")
public final class MeterReading {
    @androidx.room.PrimaryKey()
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String pelangganId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String petugasId = null;
    private final int standAngka = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String fotoPath = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String fotoUrl = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String fotoDriveId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double lat = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double lng = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String catatan = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String bulan = null;
    private final long createdAt = 0L;
    private final boolean isSynced = false;
    private final int minus = 0;
    
    public MeterReading(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String pelangganId, @org.jetbrains.annotations.NotNull()
    java.lang.String petugasId, int standAngka, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoPath, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoDriveId, @org.jetbrains.annotations.Nullable()
    java.lang.Double lat, @org.jetbrains.annotations.Nullable()
    java.lang.Double lng, @org.jetbrains.annotations.Nullable()
    java.lang.String catatan, @org.jetbrains.annotations.NotNull()
    java.lang.String bulan, long createdAt, boolean isSynced, int minus) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPelangganId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPetugasId() {
        return null;
    }
    
    public final int getStandAngka() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFotoPath() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFotoUrl() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFotoDriveId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getLat() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getLng() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCatatan() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getBulan() {
        return null;
    }
    
    public final long getCreatedAt() {
        return 0L;
    }
    
    public final boolean isSynced() {
        return false;
    }
    
    public final int getMinus() {
        return 0;
    }
    
    public MeterReading() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component11() {
        return null;
    }
    
    public final long component12() {
        return 0L;
    }
    
    public final boolean component13() {
        return false;
    }
    
    public final int component14() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    public final int component4() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.sipekat.app.data.model.MeterReading copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String pelangganId, @org.jetbrains.annotations.NotNull()
    java.lang.String petugasId, int standAngka, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoPath, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoUrl, @org.jetbrains.annotations.Nullable()
    java.lang.String fotoDriveId, @org.jetbrains.annotations.Nullable()
    java.lang.Double lat, @org.jetbrains.annotations.Nullable()
    java.lang.Double lng, @org.jetbrains.annotations.Nullable()
    java.lang.String catatan, @org.jetbrains.annotations.NotNull()
    java.lang.String bulan, long createdAt, boolean isSynced, int minus) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}