package domain

import kotlinx.serialization.Serializable

@Serializable
data class CommiterModel(
    val fullName: String,
    val commitsCount: Int,
)

