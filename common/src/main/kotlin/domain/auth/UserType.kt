package domain.auth

import kotlinx.serialization.Serializable

@Serializable
enum class UserType {
    Common, Curator
}

