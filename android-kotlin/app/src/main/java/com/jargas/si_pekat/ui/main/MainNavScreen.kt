package com.jargas.si_pekat.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.jargas.si_pekat.ui.Route
import com.jargas.si_pekat.ui.theme.AppColors

private data class NavItem(val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("Beranda", Icons.Rounded.Home),
    NavItem("Riwayat", Icons.Rounded.History),
    NavItem("Pesan", Icons.Rounded.Mail),
    NavItem("Profil", Icons.Rounded.Person),
)

@Composable
fun MainNavScreen(
    userNama: String,
    userEmail: String,
    onOpenModule: (Route) -> Unit,
    onLogout: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = AppColors.Canvas,
        // Setiap tab menggambar header sendiri sampai ke belakang status bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NAV_ITEMS.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Brand,
                            selectedTextColor = AppColors.Brand,
                            unselectedIconColor = AppColors.Muted,
                            unselectedTextColor = AppColors.Muted,
                            indicatorColor = AppColors.BrandLight,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> DashboardScreen(userNama, userEmail, onOpenModule)
                1 -> RiwayatTab()
                2 -> PesanTab()
                else -> ProfilTab(userNama, userEmail, onLogout)
            }
        }
    }
}
