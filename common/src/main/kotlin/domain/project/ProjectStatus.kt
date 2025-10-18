package domain.project

import kotlinx.serialization.Serializable

@Serializable
enum class ProjectStatus {
    Pending, Approved, Unapproved
}

