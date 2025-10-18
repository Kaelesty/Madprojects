package domain.profile

import domain.auth.UserType
import kotlinx.serialization.Serializable

@Serializable
data class SharedProfile(
    val firstName: String,
    val secondName: String,
    val lastName: String,
)

@Serializable
data class RoledSharedProfile(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val role: UserType,
    val email: String,
)
