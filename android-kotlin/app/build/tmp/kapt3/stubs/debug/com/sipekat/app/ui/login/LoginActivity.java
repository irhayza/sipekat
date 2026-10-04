package com.sipekat.app.ui.login;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000f\u001a\u00020\u0010H\u0002J\b\u0010\u0011\u001a\u00020\u0010H\u0002J\u0018\u0010\u0012\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\b2\u0006\u0010\u0014\u001a\u00020\bH\u0002J\b\u0010\u0015\u001a\u00020\u0010H\u0002J\u0012\u0010\u0016\u001a\u00020\u00102\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0014J\b\u0010\u0019\u001a\u00020\u0010H\u0002J\u001a\u0010\u001a\u001a\u00020\u00102\u0006\u0010\u001b\u001a\u00020\u001c2\b\b\u0002\u0010\u001d\u001a\u00020\bH\u0002J\b\u0010\u001e\u001a\u00020\u0010H\u0002J\u0010\u0010\u001f\u001a\u00020\u00102\u0006\u0010 \u001a\u00020\bH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\t\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\f\u00a8\u0006!"}, d2 = {"Lcom/sipekat/app/ui/login/LoginActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/sipekat/app/databinding/ActivityLoginBinding;", "locationPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "viewModel", "Lcom/sipekat/app/ui/login/LoginViewModel;", "getViewModel", "()Lcom/sipekat/app/ui/login/LoginViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "checkGpsThenLogin", "", "checkLocationPermissionThenLogin", "navigateToMain", "nama", "email", "observeViewModel", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "proceedWithLogin", "setLoading", "loading", "", "stage", "setupViews", "showErr", "msg", "app_debug"})
public final class LoginActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.sipekat.app.databinding.ActivityLoginBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> locationPermissionLauncher = null;
    
    public LoginActivity() {
        super();
    }
    
    private final com.sipekat.app.ui.login.LoginViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupViews() {
    }
    
    private final void checkLocationPermissionThenLogin() {
    }
    
    private final void checkGpsThenLogin() {
    }
    
    private final void proceedWithLogin() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void showErr(java.lang.String msg) {
    }
    
    private final void setLoading(boolean loading, java.lang.String stage) {
    }
    
    private final void navigateToMain(java.lang.String nama, java.lang.String email) {
    }
}