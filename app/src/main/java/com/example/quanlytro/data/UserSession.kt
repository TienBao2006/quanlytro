package com.example.quanlytro

object UserSession {
    var uid: String = ""
    var fullName: String = ""
    var phone: String = ""
    var role: String = ""
    var email: String = ""
    var address: String = ""
    var dob: String = ""
    var idCard: String = ""
    var avatar: String = ""

    fun clear() {
        uid = ""; fullName = ""; phone = ""; role = ""
        email = ""; address = ""; dob = ""; idCard = ""; avatar = ""
    }
}
