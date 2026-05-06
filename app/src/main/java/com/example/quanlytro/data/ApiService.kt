package com.example.quanlytro

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("register.php")
    fun registerUser(
        @Field("fullName") fullName: String,
        @Field("email")    email: String,
        @Field("phone")    phone: String,
        @Field("password") password: String,
        @Field("role")     role: String
    ): Call<String>

    @FormUrlEncoded
    @POST("login.php")
    fun loginUser(
        @Field("email")    email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @GET("get_user.php")
    fun getUserByPhone(@Query("phone") phone: String): Call<UserLookupResponse>

    @GET("get_user.php")
    fun getUserByUid(@Query("uid") uid: String): Call<UserLookupResponse>

    @FormUrlEncoded
    @POST("update_profile.php")
    fun updateProfile(
        @Field("uid")      uid: String,
        @Field("fullName") fullName: String,
        @Field("email")    email: String,
        @Field("phone")    phone: String,
        @Field("address")  address: String,
        @Field("dob")      dob: String,
        @Field("id_card")  idCard: String,
        @Field("avatar")   avatar: String = ""
    ): Call<SimpleResponse>

    // ── Posts ─────────────────────────────────────────────────────────────
    @GET("get_posts.php")
    fun getPosts(): Call<List<PostResponse>>

    @GET("get_posts.php")
    fun getPostById(@Query("id") id: Int): Call<List<PostResponse>>

    @FormUrlEncoded
    @POST("upload_post.php")
    fun uploadPost(
        @Field("user_id")         userId: String,
        @Field("title")           title: String,
        @Field("location")        location: String,
        @Field("price")           price: Double,
        @Field("area")            area: Double,
        @Field("description")     description: String,
        @Field("amenities")       amenities: String,
        @Field("map_url")         mapUrl: String,
        @Field("contact_name")    contactName: String,
        @Field("contact_phone")   contactPhone: String,
        @Field("images")          imagesJson: String,
        @Field("total_rooms")     totalRooms: Int
    ): Call<String>

    @FormUrlEncoded
    @POST("update_post.php")
    fun updatePost(
        @Field("id")              id: Int,
        @Field("user_id")         userId: String,
        @Field("title")           title: String,
        @Field("location")        location: String,
        @Field("price")           price: Double,
        @Field("area")            area: Double,
        @Field("description")     description: String,
        @Field("amenities")       amenities: String,
        @Field("map_url")         mapUrl: String,
        @Field("contact_name")    contactName: String,
        @Field("contact_phone")   contactPhone: String,
        @Field("images")          imagesJson: String,
        @Field("total_rooms")     totalRooms: Int
    ): Call<String>

    @FormUrlEncoded
    @POST("delete_post.php")
    fun deletePost(@Field("id") id: Int): Call<String>

    // ── Chat ──────────────────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("chat_send.php")
    fun sendMessage(
        @Field("sender_id")   senderId: String,
        @Field("receiver_id") receiverId: String,
        @Field("message")     message: String
    ): Call<String>

    @GET("chat_get.php")
    fun getMessages(
        @Query("sender_id")   senderId: String,
        @Query("receiver_id") receiverId: String
    ): Call<List<ChatMessage>>

    @GET("chat_list.php")
    fun getChatList(@Query("user_id") userId: String): Call<List<ChatConversation>>

    @FormUrlEncoded
    @POST("chat_welcome.php")
    fun sendWelcomeMessage(
        @Field("landlord_id") landlordId: String,
        @Field("tenant_id")   tenantId: String
    ): Call<String>

    // ── Booking ───────────────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("send_otp.php")
    fun sendOtp(@Field("phone") phone: String): Call<OtpResponse>

    @FormUrlEncoded
    @POST("verify_otp.php")
    fun verifyOtp(
        @Field("phone") phone: String,
        @Field("otp")   otp: String
    ): Call<SimpleResponse>

    @GET("get_booked_rooms.php")
    fun getBookedRooms(@Query("post_id") postId: Int): Call<BookedRoomsResponse>

    @GET("check_booking.php")
    fun checkBooking(
        @Query("post_id") postId: Int,
        @Query("user_id") userId: String
    ): Call<BookingStatusResponse>

    @GET("get_bookings.php")
    fun getLandlordBookings(@Query("landlord_id") landlordId: String): Call<BookingListResponse>

    @FormUrlEncoded
    @POST("update_booking.php")
    fun updateBookingStatus(
        @Field("booking_id")  bookingId: Int,
        @Field("status")      status: String,
        @Field("landlord_id") landlordId: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("delete_booking.php")
    fun deleteBooking(@Field("booking_id") bookingId: Int): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("cancel_booking.php")
    fun cancelBooking(
        @Field("post_id") postId: Int,
        @Field("user_id") userId: String
    ): Call<SimpleResponse>

    // ── Contract ──────────────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("create_contract.php")
    fun createContract(
        @Field("booking_id")               bookingId: Int,
        @Field("post_id")                  postId: Int,
        @Field("landlord_id")              landlordId: String,
        @Field("tenant_id")                tenantId: String,
        @Field("tenant_name")              tenantName: String,
        @Field("tenant_phone")             tenantPhone: String,
        @Field("tenant_id_card")           tenantIdCard: String,
        @Field("tenant_address")           tenantAddress: String,
        @Field("landlord_name")            landlordName: String,
        @Field("landlord_phone")           landlordPhone: String,
        @Field("landlord_address")         landlordAddress: String,
        @Field("room_name")                roomName: String,
        @Field("room_address")             roomAddress: String,
        @Field("room_area")                roomArea: Double,
        @Field("amenities")                amenities: String,
        @Field("rent_price")               rentPrice: Double,
        @Field("deposit")                  deposit: Double,
        @Field("electric_price")           electricPrice: Double,
        @Field("water_price")              waterPrice: Double,
        @Field("other_fee")                otherFee: Double,
        @Field("other_fee_note")           otherFeeNote: String,
        @Field("start_date")               startDate: String,
        @Field("duration_months")          durationMonths: Int,
        @Field("payment_day")              paymentDay: Int,
        @Field("payment_method")           paymentMethod: String,
        @Field("late_payment_rule")        latePaymentRule: String,
        @Field("rules")                    rules: String,
        @Field("termination_notice_days")  terminationNoticeDays: Int,
        @Field("deposit_return_condition") depositReturnCondition: String
    ): Call<SimpleResponse>

    @GET("get_landlord_stats.php")
    fun getLandlordStats(@Query("landlord_id") landlordId: String): Call<LandlordStatsResponse>

    @GET("get_contracts.php")
    fun getContractsByLandlord(@Query("landlord_id") landlordId: String): Call<ContractListResponse>

    @GET("get_contracts.php")
    fun getContractsByTenant(@Query("tenant_id") tenantId: String): Call<ContractListResponse>

    @FormUrlEncoded
    @POST("confirm_contract.php")
    fun confirmContract(
        @Field("contract_id") contractId: Int,
        @Field("tenant_id")   tenantId: String,
        @Field("action")      action: String  // "agreed" | "rejected"
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("cancel_contract.php")
    fun cancelContract(
        @Field("contract_id") contractId: Int,
        @Field("user_id")     userId: String,
        @Field("role")        role: String  // "landlord" | "tenant"
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("respond_cancel_contract.php")
    fun respondCancelContract(
        @Field("contract_id") contractId: Int,
        @Field("user_id")     userId: String,
        @Field("role")        role: String,   // "landlord" | "tenant"
        @Field("action")      action: String  // "accept" | "reject"
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("update_contract.php")
    fun updateContract(
        @Field("contract_id")              contractId: Int,
        @Field("landlord_id")              landlordId: String,
        @Field("rent_price")               rentPrice: Double,
        @Field("deposit")                  deposit: Double,
        @Field("electric_price")           electricPrice: Double,
        @Field("water_price")              waterPrice: Double,
        @Field("other_fee")                otherFee: Double,
        @Field("other_fee_note")           otherFeeNote: String,
        @Field("start_date")               startDate: String,
        @Field("duration_months")          durationMonths: Int,
        @Field("payment_day")              paymentDay: Int,
        @Field("payment_method")           paymentMethod: String,
        @Field("late_payment_rule")        latePaymentRule: String,
        @Field("rules")                    rules: String,
        @Field("termination_notice_days")  terminationNoticeDays: Int,
        @Field("deposit_return_condition") depositReturnCondition: String,
        @Field("room_name")                roomName: String,
        @Field("room_address")             roomAddress: String,
        @Field("room_area")                roomArea: Double,
        @Field("amenities")                amenities: String
    ): Call<SimpleResponse>

    // ── Invoice ───────────────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("create_invoice.php")
    fun createInvoice(
        @Field("contract_id")    contractId: Int,
        @Field("landlord_id")    landlordId: String,
        @Field("month")          month: String,
        @Field("electric_old")   electricOld: Double,
        @Field("electric_new")   electricNew: Double,
        @Field("electric_price") electricPrice: Double,
        @Field("water_old")      waterOld: Double,
        @Field("water_new")      waterNew: Double,
        @Field("water_price")    waterPrice: Double,
        @Field("other_fee")      otherFee: Double,
        @Field("other_fee_note") otherFeeNote: String
    ): Call<CreateInvoiceResponse>

    @GET("get_invoices.php")
    fun getInvoicesByLandlord(@Query("landlord_id") landlordId: String): Call<InvoiceListResponse>

    @GET("get_invoices.php")
    fun getInvoicesByTenant(@Query("tenant_id") tenantId: String): Call<InvoiceListResponse>

    @GET("get_invoices.php")
    fun getInvoicesByContract(
        @Query("landlord_id")  landlordId: String,
        @Query("contract_id")  contractId: Int
    ): Call<InvoiceListResponse>

    @FormUrlEncoded
    @POST("update_invoice_status.php")
    fun updateInvoiceStatus(
        @Field("invoice_id")      invoiceId: Int,
        @Field("status")          status: String,
        @Field("user_id")         userId: String,
        @Field("payment_method")  paymentMethod: String = "",
        @Field("txn_id")          txnId: String = ""
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("update_invoice.php")
    fun updateInvoice(
        @Field("invoice_id")     invoiceId: Int,
        @Field("landlord_id")    landlordId: String,
        @Field("month")          month: String,
        @Field("electric_old")   electricOld: Double,
        @Field("electric_new")   electricNew: Double,
        @Field("electric_price") electricPrice: Double,
        @Field("water_old")      waterOld: Double,
        @Field("water_new")      waterNew: Double,
        @Field("water_price")    waterPrice: Double,
        @Field("other_fee")      otherFee: Double,
        @Field("other_fee_note") otherFeeNote: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("delete_invoice.php")
    fun deleteInvoice(
        @Field("invoice_id")  invoiceId: Int,
        @Field("landlord_id") landlordId: String
    ): Call<SimpleResponse>

    @GET("get_notifications.php")
    fun getNotifications(@Query("user_id") userId: String): Call<NotificationListResponse>

    @GET("count_notifications.php")
    fun countNotifications(@Query("user_id") userId: String): Call<NotificationCountResponse>

    @FormUrlEncoded
    @POST("delete_notification.php")
    fun deleteNotification(
        @Field("notification_id") notificationId: Int,
        @Field("user_id")         userId: String
    ): Call<SimpleResponse>

    // ── Landlord Notice ───────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("send_landlord_notice.php")
    fun sendLandlordNotice(
        @Field("landlord_id") landlordId: String,
        @Field("post_id")     postId: Int,
        @Field("title")       title: String,
        @Field("message")     message: String
    ): Call<SendNoticeResponse>

    @GET("get_landlord_notices.php")
    fun getLandlordNotices(
        @Query("landlord_id") landlordId: String,
        @Query("post_id")     postId: Int = 0
    ): Call<LandlordNoticeListResponse>

    // ── Renew Contract ────────────────────────────────────────────────────
    @FormUrlEncoded
    @POST("request_renew_contract.php")
    fun requestRenewContract(
        @Field("contract_id")    contractId: Int,
        @Field("tenant_id")      tenantId: String,
        @Field("extend_months")  extendMonths: Int
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("respond_renew_contract.php")
    fun respondRenewContract(
        @Field("contract_id") contractId: Int,
        @Field("landlord_id") landlordId: String,
        @Field("action")      action: String  // "accept" | "reject"
    ): Call<SimpleResponse>

    // ── Tenant Management ─────────────────────────────────────────────────
    @GET("get_tenants.php")
    fun getTenants(@Query("landlord_id") landlordId: String): Call<TenantListResponse>

    @FormUrlEncoded
    @POST("remind_payment.php")
    fun remindPayment(
        @Field("contract_id") contractId: Int,
        @Field("landlord_id") landlordId: String,
        @Field("tenant_id")   tenantId: String,
        @Field("message")     message: String = ""
    ): Call<SimpleResponse>

    // ── Room Members ──────────────────────────────────────────────────────
    @GET("get_room_members.php")
    fun getRoomMembers(@Query("contract_id") contractId: Int): Call<RoomMemberListResponse>

    @FormUrlEncoded
    @POST("add_room_member.php")
    fun addRoomMember(
        @Field("contract_id") contractId: Int,
        @Field("landlord_id") landlordId: String,
        @Field("full_name")   fullName: String,
        @Field("phone")       phone: String,
        @Field("id_card")     idCard: String,
        @Field("dob")         dob: String,
        @Field("note")        note: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("delete_room_member.php")
    fun deleteRoomMember(
        @Field("member_id")   memberId: Int,
        @Field("landlord_id") landlordId: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("booking.php")
    fun createBooking(
        @Field("post_id")       postId: Int,
        @Field("user_id")       userId: String,
        @Field("room_number")   roomNumber: String,
        @Field("full_name")     fullName: String,
        @Field("phone")         phone: String,
        @Field("id_card")       idCard: String,
        @Field("dob")           dob: String,
        @Field("id_issue_date") idIssueDate: String,
        @Field("start_date")    startDate: String,
        @Field("duration")      duration: String,
        @Field("email")         email: String,
        @Field("address")       address: String
    ): Call<SimpleResponse>
}
