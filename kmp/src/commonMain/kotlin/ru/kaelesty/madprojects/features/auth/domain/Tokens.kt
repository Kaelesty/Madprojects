package ru.kaelesty.madprojects.features.auth.domain

data class Tokens(
    val access: String,
    val refresh: String,
)
