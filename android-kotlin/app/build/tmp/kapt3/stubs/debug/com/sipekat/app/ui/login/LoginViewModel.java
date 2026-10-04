package com.sipekat.app.ui.login;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000fR\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0011"}, d2 = {"Lcom/sipekat/app/ui/login/LoginViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "_state", "Landroidx/lifecycle/MutableLiveData;", "Lcom/sipekat/app/ui/login/LoginUiState;", "repo", "Lcom/sipekat/app/data/repository/AuthRepository;", "state", "Landroidx/lifecycle/LiveData;", "getState", "()Landroidx/lifecycle/LiveData;", "login", "", "userId", "", "password", "app_debug"})
public final class LoginViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.sipekat.app.data.repository.AuthRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.MutableLiveData<com.sipekat.app.ui.login.LoginUiState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.lifecycle.LiveData<com.sipekat.app.ui.login.LoginUiState> state = null;
    
    public LoginViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.lifecycle.LiveData<com.sipekat.app.ui.login.LoginUiState> getState() {
        return null;
    }
    
    public final void login(@org.jetbrains.annotations.NotNull()
    java.lang.String userId, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
}