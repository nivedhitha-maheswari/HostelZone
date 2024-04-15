package com.example.hostelzone

data class UserData(
    val id: String?=null,
    val username: String?=null,
    val password: String?=null,
    val userType: String?=null,
    var additionalData: MutableMap<String, Any>? = null
)
