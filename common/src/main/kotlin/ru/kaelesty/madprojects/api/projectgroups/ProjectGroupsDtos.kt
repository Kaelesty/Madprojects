package ru.kaelesty.madprojects.api.projectgroups

import domain.projectgroups.ProjectInGroupView
import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectGroupRequest(
    val title: String
)

@Serializable
data class GroupProjectsResponse(
    val title: String,
    val projects: List<ProjectInGroupView>
)

