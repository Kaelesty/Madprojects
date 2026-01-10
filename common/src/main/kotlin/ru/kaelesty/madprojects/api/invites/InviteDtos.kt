package ru.kaelesty.madprojects.api.invites

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInviteResponse(
    val invite: String,
)

@Serializable
data class UseInviteResponse(
    val projectId: String,
)
