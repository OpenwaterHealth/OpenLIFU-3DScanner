package com.example.facedetectionar.api.dto

data class UserCredit(
    val uid: String,
    val credit: Int
)

data class UserData(
    val user: UserCredit
)

data class UserCreditsResponse(
    val status: String,
    val data: UserData?
)