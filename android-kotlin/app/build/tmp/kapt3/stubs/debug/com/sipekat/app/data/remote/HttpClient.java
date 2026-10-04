package com.sipekat.app.data.remote;

/**
 * Shared OkHttp helper — semua repository pakai ini
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J \u0010\u000f\u001a\u00020\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0012\u001a\u00020\u0010H\u0086@\u00a2\u0006\u0002\u0010\u0013JN\u0010\u0014\u001a\u0010\u0012\u0004\u0012\u00020\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u00152\b\b\u0002\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0012\u001a\u00020\u00102\u0014\u0010\u0016\u001a\u0010\u0012\u0004\u0012\u00020\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u00152\b\b\u0002\u0010\u0017\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u001a"}, d2 = {"Lcom/sipekat/app/data/remote/HttpClient;", "", "()V", "default", "Lokhttp3/OkHttpClient;", "getDefault", "()Lokhttp3/OkHttpClient;", "gson", "Lcom/google/gson/Gson;", "getGson", "()Lcom/google/gson/Gson;", "jsonType", "Lokhttp3/MediaType;", "getJsonType", "()Lokhttp3/MediaType;", "getAsText", "", "client", "url", "(Lokhttp3/OkHttpClient;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "postJson", "", "body", "timeoutSec", "", "(Lokhttp3/OkHttpClient;Ljava/lang/String;Ljava/util/Map;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class HttpClient {
    @org.jetbrains.annotations.NotNull()
    private static final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.MediaType jsonType = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.sipekat.app.data.remote.HttpClient INSTANCE = null;
    
    private HttpClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.google.gson.Gson getGson() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final okhttp3.MediaType getJsonType() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final okhttp3.OkHttpClient getDefault() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object postJson(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient client, @org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> body, long timeoutSec, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getAsText(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient client, @org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
}