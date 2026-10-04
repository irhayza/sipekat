package com.sipekat.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u0000 \u00102\u00020\u0001:\u0001\u0010B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000e\u001a\u00020\u000fH\u0016R\u001b\u0010\u0003\u001a\u00020\u00048FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0007\u0010\b\u001a\u0004\b\u0005\u0010\u0006R\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\b\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0011"}, d2 = {"Lcom/sipekat/app/SiPekatApp;", "Landroid/app/Application;", "()V", "database", "Lcom/sipekat/app/data/local/AppDatabase;", "getDatabase", "()Lcom/sipekat/app/data/local/AppDatabase;", "database$delegate", "Lkotlin/Lazy;", "sessionManager", "Lcom/sipekat/app/data/local/SessionManager;", "getSessionManager", "()Lcom/sipekat/app/data/local/SessionManager;", "sessionManager$delegate", "onCreate", "", "Companion", "app_debug"})
public final class SiPekatApp extends android.app.Application {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy database$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy sessionManager$delegate = null;
    private static com.sipekat.app.SiPekatApp instance;
    @org.jetbrains.annotations.NotNull()
    public static final com.sipekat.app.SiPekatApp.Companion Companion = null;
    
    public SiPekatApp() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.sipekat.app.data.local.AppDatabase getDatabase() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.sipekat.app.data.local.SessionManager getSessionManager() {
        return null;
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u001e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0003\u001a\u00020\u0004@BX\u0086.\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\b"}, d2 = {"Lcom/sipekat/app/SiPekatApp$Companion;", "", "()V", "<set-?>", "Lcom/sipekat/app/SiPekatApp;", "instance", "getInstance", "()Lcom/sipekat/app/SiPekatApp;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.sipekat.app.SiPekatApp getInstance() {
            return null;
        }
    }
}