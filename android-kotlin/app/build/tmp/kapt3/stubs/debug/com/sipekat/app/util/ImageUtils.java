package com.sipekat.app.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0007\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J \u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u0004H\u0002J\"\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u00042\b\b\u0002\u0010\u000e\u001a\u00020\u0004J \u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\n2\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u0004H\u0002J\u0018\u0010\u0012\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J\u0018\u0010\u0016\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u00102\u0006\u0010\u0017\u001a\u00020\nH\u0002\u00a8\u0006\u0018"}, d2 = {"Lcom/sipekat/app/util/ImageUtils;", "", "()V", "calculateInSampleSize", "", "options", "Landroid/graphics/BitmapFactory$Options;", "reqWidth", "reqHeight", "compressAndEncodeBase64", "", "imageFile", "Ljava/io/File;", "maxWidth", "quality", "decodeSampledBitmapFromFile", "Landroid/graphics/Bitmap;", "path", "rotateImage", "img", "degree", "", "rotateImageIfRequired", "selectedImage", "app_debug"})
public final class ImageUtils {
    @org.jetbrains.annotations.NotNull()
    public static final com.sipekat.app.util.ImageUtils INSTANCE = null;
    
    private ImageUtils() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String compressAndEncodeBase64(@org.jetbrains.annotations.NotNull()
    java.io.File imageFile, int maxWidth, int quality) {
        return null;
    }
    
    private final android.graphics.Bitmap decodeSampledBitmapFromFile(java.lang.String path, int reqWidth, int reqHeight) {
        return null;
    }
    
    private final int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return 0;
    }
    
    private final android.graphics.Bitmap rotateImageIfRequired(android.graphics.Bitmap img, java.lang.String selectedImage) {
        return null;
    }
    
    private final android.graphics.Bitmap rotateImage(android.graphics.Bitmap img, float degree) {
        return null;
    }
}