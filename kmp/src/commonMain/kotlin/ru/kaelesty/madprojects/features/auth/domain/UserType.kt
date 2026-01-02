package ru.kaelesty.madprojects.features.auth.domain

import kotlinx.serialization.Serializable

@Serializable
enum class UserType {
    Common,
    Curator,
}
