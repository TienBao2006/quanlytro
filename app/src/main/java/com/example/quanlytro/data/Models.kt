package com.example.quanlytro

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

// ── Auth ──────────────────────────────────────────────────────────────────
data class LoginResponse(
    val status: String,
    val uid: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val message: String? = null
)

data class UserLookupResponse(
    val status: String,
    val uid: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val id_card: String? = null,
    val role: String? = null,
    val avatar: String? = null
)

// ── Post ──────────────────────────────────────────────────────────────────
data class PostResponse(
    val id: Int,
    val title: String,
    val location: String,
    val price: Double,
    val area: Double,
    val description: String,
    val amenities: String?,
    val map_url: String?,
    val contact_name: String?,
    val contact_phone: String?,
    val images: List<String>?,
    val created_at: String,
    val user_id: String? = null,
    val available: Int? = 1,
    val available_rooms: Int? = 1,
    val total_rooms: Int? = 1
)

// ── Chat ──────────────────────────────────────────────────────────────────
data class ChatMessage(
    val id: Int = 0,
    val sender_id: String = "",
    val receiver_id: String = "",
    val message: String = "",
    val created_at: String = ""
)

data class ChatConversation(
    val chat_id: String = "",
    val other_id: String = "",
    val other_name: String = "",
    val other_avatar: String = "",
    val last_message: String = "",
    val last_time: String = ""
)

// ── Booking ───────────────────────────────────────────────────────────────
data class BookingStatusResponse(
    val status: String  // "none" | "pending" | "confirmed" | "rejected"
)

data class BookedRoomsResponse(
    val status: String,
    val booked_rooms: List<String> = emptyList()
)

data class BookingItem(
    val id: Int,
    val post_id: Int,
    val user_id: String,
    val room_number: String? = null,
    val full_name: String,
    val phone: String,
    val id_card: String,
    val dob: String,
    val id_issue_date: String,
    val start_date: String? = null,
    val duration: String? = null,
    val email: String?,
    val address: String?,
    val status: String,   // "pending" | "confirmed" | "rejected"
    val created_at: String,
    val post_title: String,
    val has_contract: Int = 0  // 1 nếu đã tạo hợp đồng
)

data class BookingListResponse(
    val status: String,
    val bookings: List<BookingItem>? = null
)

// ── Contract ──────────────────────────────────────────────────────────────
data class ContractItem(
    val id: Int,
    val booking_id: Int,
    val post_id: Int,
    val landlord_id: String,
    val tenant_id: String,
    val tenant_name: String,
    val tenant_phone: String,
    val tenant_id_card: String,
    val tenant_address: String?,
    val landlord_name: String,
    val landlord_phone: String,
    val landlord_address: String?,
    val room_name: String?,
    val room_address: String?,
    val room_area: Double,
    val amenities: String?,
    val rent_price: Double,
    val deposit: Double,
    val electric_price: Double,
    val water_price: Double,
    val other_fee: Double,
    val other_fee_note: String?,
    val start_date: String,
    val duration_months: Int,
    val payment_day: Int,
    val payment_method: String,
    val late_payment_rule: String?,
    val rules: String?,
    val termination_notice_days: Int,
    val deposit_return_condition: String?,
    val status: String,
    val created_at: String,
    val renew_requested_months: Int = 0
)

data class ContractListResponse(
    val status: String,
    val contracts: List<ContractItem>? = null
)

// ── Landlord Stats ────────────────────────────────────────────────────────
data class LandlordStatsResponse(
    val status: String,
    val total_rooms: Int = 0,
    val active_contracts: Int = 0,
    val empty_rooms: Int = 0
)

// ── Invoice ───────────────────────────────────────────────────────────────
data class InvoiceItem(
    val id: Int,
    val contract_id: Int,
    val landlord_id: String,
    val tenant_id: String,
    val month: String,
    val electric_old: Double,
    val electric_new: Double,
    val electric_used: Double,
    val electric_price: Double,
    val electric_cost: Double,
    val water_old: Double,
    val water_new: Double,
    val water_used: Double,
    val water_price: Double,
    val water_cost: Double,
    val rent_price: Double,
    val other_fee: Double,
    val other_fee_note: String?,
    val total: Double,
    val status: String,           // "unpaid" | "paid"
    val payment_method: String?,  // "cash" | "wallet"
    val txn_id: String?,
    val paid_at: String?,
    val created_at: String,
    val room_name: String?,
    val room_address: String?
)

data class InvoiceListResponse(
    val status: String,
    val invoices: List<InvoiceItem>? = null
)

data class CreateInvoiceResponse(
    val status: String,
    val invoice_id: Int? = null,
    val total: Double? = null,
    val message: String? = null
)

// ── Notification ──────────────────────────────────────────────────────────
data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val type: String,       // "payment" | "general" | ...
    val is_read: Int,       // 0 | 1
    val created_at: String
)

data class NotificationListResponse(
    val status: String,
    val notifications: List<NotificationItem>? = null
)

data class NotificationCountResponse(
    val status: String,
    val count: Int = 0
)

// ── Landlord Notice ───────────────────────────────────────────────────────
data class LandlordNoticeItem(
    val id: Int,
    val post_id: Int,
    val title: String,
    val message: String,
    val created_at: String,
    val post_title: String?,
    val tenant_count: Int = 0
)

data class LandlordNoticeListResponse(
    val status: String,
    val notices: List<LandlordNoticeItem>? = null
)

data class SendNoticeResponse(
    val status: String,
    val notice_id: Int? = null,
    val sent_to: Int? = null,
    val message: String? = null
)

// ── Tenant Management ─────────────────────────────────────────────────────
data class TenantItem(
    val contract_id: Int,
    val tenant_id: String,
    val tenant_name: String,
    val tenant_phone: String,
    val tenant_id_card: String,
    val tenant_address: String?,
    val room_name: String?,
    val room_address: String?,
    val start_date: String,
    val duration_months: Int,
    val rent_price: Double,
    val contract_status: String,
    val avatar: String?,
    val email: String?,
    val dob: String?,
    val unpaid_count: Int,
    val paid_count: Int,
    val member_count: Int = 0
)

data class TenantListResponse(
    val status: String,
    val tenants: List<TenantItem>? = null
)

// ── Room Members ──────────────────────────────────────────────────────────
data class RoomMember(
    val id: Int,
    val full_name: String,
    val phone: String = "",
    val id_card: String = "",
    val dob: String = "",
    val note: String = "",
    val created_at: String = ""
)

data class RoomMemberListResponse(
    val status: String,
    val members: List<RoomMember> = emptyList()
)

// ── Common ────────────────────────────────────────────────────────────────
data class OtpResponse(
    val status: String,
    val message: String? = null,
    val otp_debug: String? = null
)

data class SimpleResponse(
    val status: String,
    val message: String? = null,
    val booking_id: Int? = null
)

// ── Image helper ──────────────────────────────────────────────────────────
@Composable
fun Base64Image(base64String: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64String) {
        try {
            val pure = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
            val bytes = Base64.decode(pure.replace(" ", "+"), Base64.DEFAULT)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            opts.inSampleSize = calculateInSampleSize(opts, 1024, 1024)
            opts.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (e: Exception) { null }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfH = height / 2; val halfW = width / 2
        while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) inSampleSize *= 2
    }
    return inSampleSize
}

fun formatPrice(price: Double): String = String.format("%,.0f", price)
