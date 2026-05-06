package com.example.quanlytro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quanlytro.screen.BookingManageScreen
import com.example.quanlytro.screen.ContractListScreen
import com.example.quanlytro.screen.CreateContractScreen
import com.example.quanlytro.screen.InvoiceManageScreen
import com.example.quanlytro.screen.TenantInvoiceScreen
import com.example.quanlytro.screen.NotificationScreen
import com.example.quanlytro.screen.UtilityStatsScreen
import com.example.quanlytro.screen.LandlordNoticeScreen
import com.example.quanlytro.ui.theme.QuanLyTroTheme
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuanLyTroTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var homeRefreshKey by remember { mutableStateOf(0) }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate("home/$role") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "home/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Người thuê"
            HomeScreen(
                userRole = role,
                refreshKey = homeRefreshKey,
                onProfileClick = {
                    navController.navigate("profile/$role")
                },
                onManageClick = {
                    navController.navigate("landlord/$role")
                },
                onPostClick = {
                    navController.navigate("post_room")
                },
                onEditPostClick = { post ->
                    val postJson = Gson().toJson(post)
                    val encodedJson = URLEncoder.encode(postJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("post_room?postJson=$encodedJson")
                },
                onPostDetailClick = { post ->
                    val postJson = Gson().toJson(post)
                    val encodedJson = URLEncoder.encode(postJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("post_detail/$encodedJson")
                },
                onChatListClick = {
                    navController.navigate("chat_list") {
                        popUpTo("home/${UserSession.role}") { saveState = true }
                        launchSingleTop = true
                    }
                },
                onNotificationClick = {
                    navController.navigate("notifications")
                }
            )
        }
        composable("chat_list") {
            ChatListScreen(
                userRole = UserSession.role,
                onBackClick = { navController.popBackStack() },
                onChatClick = { otherId, otherName ->
                    val encodedName = URLEncoder.encode(otherName, StandardCharsets.UTF_8.toString())
                    navController.navigate("chat/$otherId/$encodedName")
                },
                onHomeClick = {
                    navController.navigate("home/${UserSession.role}") {
                        popUpTo("home/${UserSession.role}") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    navController.navigate("profile/${UserSession.role}")
                },
                onManageClick = {
                    navController.navigate("landlord/${UserSession.role}")
                }
            )
        }
        composable(
            route = "chat/{receiverId}/{receiverName}",
            arguments = listOf(
                navArgument("receiverId") { type = NavType.StringType },
                navArgument("receiverName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = URLDecoder.decode(
                backStackEntry.arguments?.getString("receiverName") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            ChatScreen(
                receiverId = receiverId,
                receiverName = receiverName,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "post_detail/{postJson}",
            arguments = listOf(navArgument("postJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val postJson = backStackEntry.arguments?.getString("postJson") ?: ""
            PostDetailScreen(
                postJson = postJson,
                onBackClick = {
                    navController.popBackStack()
                },
                onChatClick = { receiverId, receiverName ->
                    val encodedName = URLEncoder.encode(receiverName, StandardCharsets.UTF_8.toString())
                    navController.navigate("chat/$receiverId/$encodedName")
                },
                onBookingClick = { postId, postTitle, totalRooms ->
                    val encodedTitle = URLEncoder.encode(postTitle, StandardCharsets.UTF_8.toString())
                    navController.navigate("booking/$postId/$encodedTitle/$totalRooms")
                }
            )
        }
        composable(
            route = "booking/{postId}/{postTitle}/{totalRooms}",
            arguments = listOf(
                navArgument("postId") { type = NavType.IntType },
                navArgument("postTitle") { type = NavType.StringType },
                navArgument("totalRooms") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getInt("postId") ?: 0
            val postTitle = URLDecoder.decode(
                backStackEntry.arguments?.getString("postTitle") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            val totalRooms = backStackEntry.arguments?.getInt("totalRooms") ?: 1
            BookingScreen(
                postId = postId,
                postTitle = postTitle,
                totalRooms = totalRooms,
                onBackClick = { navController.popBackStack() },
                onBookingSuccess = { navController.popBackStack() }
            )
        }
        composable(
            route = "profile/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Người thuê"
            ProfileScreen(
                userRole = role,
                onBackClick = {
                    navController.popBackStack()
                },
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onExploreClick = {
                    navController.navigate("home/$role")
                },
                onChatListClick = {
                    navController.navigate("chat_list") {
                        popUpTo("home/${UserSession.role}") { saveState = true }
                        launchSingleTop = true
                    }
                },
                onMyContractClick = {
                    navController.navigate("my_contracts")
                },
                onMyInvoiceClick = {
                    navController.navigate("my_invoices")
                }
            )
        }
        composable("my_contracts") {
            com.example.quanlytro.screen.TenantContractScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = "landlord/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Chủ trọ"
            LandlordScreen(
                userRole = role,
                onBackClick = { navController.popBackStack() },
                onExploreClick = { navController.navigate("home/$role") },
                onProfileClick = { navController.navigate("profile/$role") },
                onBookingManageClick = { navController.navigate("booking_manage") },
                onContractListClick = { navController.navigate("contract_list") },
                onInvoiceClick = { navController.navigate("invoice_manage") },
                onUtilityClick = { navController.navigate("utility_stats") },
                onNotificationClick = { navController.navigate("notifications") },
                onNoticeClick = { navController.navigate("landlord_notice") },
                onTenantManageClick = { navController.navigate("tenant_manage") }
            )
        }
        composable("booking_manage") {
            BookingManageScreen(
                onBackClick = { navController.popBackStack() },
                onCreateContract = { booking ->
                    val bookingJson = Gson().toJson(booking)
                    val encoded = URLEncoder.encode(bookingJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("create_contract/$encoded")
                }
            )
        }
        composable(
            route = "create_contract/{bookingJson}",
            arguments = listOf(navArgument("bookingJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingJson = URLDecoder.decode(
                backStackEntry.arguments?.getString("bookingJson") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            val booking = Gson().fromJson(bookingJson, BookingItem::class.java)
            CreateContractScreen(
                booking        = booking,
                landlordName   = UserSession.fullName,
                landlordPhone  = UserSession.phone,
                onBackClick    = { navController.popBackStack() },
                onSuccess      = {
                    navController.popBackStack("booking_manage", inclusive = false)
                }
            )
        }
        composable("post_room") {
            PostRoomScreen(
                postJson = null,
                onBackClick = { navController.popBackStack() },
                onPostSuccess = {
                    homeRefreshKey++
                    navController.popBackStack("home/Chủ trọ", inclusive = false)
                }
            )
        }
        composable(
            route = "post_room?postJson={postJson}",
            arguments = listOf(navArgument("postJson") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val postJson = backStackEntry.arguments?.getString("postJson")
            PostRoomScreen(
                postJson = postJson,
                onBackClick = { navController.popBackStack() },
                onPostSuccess = {
                    homeRefreshKey++
                    navController.popBackStack("home/Chủ trọ", inclusive = false)
                }
            )
        }
        composable("invoice_manage") {
            InvoiceManageScreen(onBackClick = { navController.popBackStack() })
        }
        composable("my_invoices") {
            TenantInvoiceScreen(onBackClick = { navController.popBackStack() })
        }
        composable("notifications") {
            NotificationScreen(onBackClick = { navController.popBackStack() })
        }
        composable("contract_list") {
            ContractListScreen(onBackClick = { navController.popBackStack() })
        }
        composable("utility_stats") {
            UtilityStatsScreen(onBackClick = { navController.popBackStack() })
        }
        composable("landlord_notice") {
            LandlordNoticeScreen(onBackClick = { navController.popBackStack() })
        }
        composable("tenant_manage") {
            com.example.quanlytro.screen.TenantManageScreen(
                onBackClick = { navController.popBackStack() },
                onChatClick = { otherId, otherName ->
                    val encodedName = URLEncoder.encode(otherName, StandardCharsets.UTF_8.toString())
                    navController.navigate("chat/$otherId/$encodedName")
                }
            )
        }
    }
}
