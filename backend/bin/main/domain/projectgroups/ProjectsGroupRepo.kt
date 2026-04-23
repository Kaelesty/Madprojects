package domain.projectgroups

import domain.project.ProjectStatus

interface ProjectsGroupRepo {

    suspend fun createProjectsGroup(title: String, curatorId: String): ProjectGroup

    suspend fun getCuratorProjectGroups(curatorId: String): List<ProjectGroup>

    suspend fun getCuratorProjectGroupsLight(curatorId: String): List<ProjectGroup>

    suspend fun getCuratorProjects(
        curatorId: String,
        projectGroupId: String?,
        status: ProjectStatus?,
        marked: Boolean?,
        mark: Int?,
    ): List<ProjectInGroupView>

    suspend fun deleteProjectGroup(id: String): Boolean

    suspend fun getGroupProjects(groupId: String): List<ProjectInGroupView>

    suspend fun getGroupProjectsAnalytics(groupId: String): List<ProjectInGroupAnalyticsView>

    suspend fun checkIsCuratorGroupOwner(curatorId: String, groupId: String): Boolean

    suspend fun getGroupTitle(groupId: String): String

    suspend fun getGroupId(projectId: String): String
}

data class ProjectInGroupAnalyticsView(
    val id: String,
    val title: String,
    val status: ProjectStatus,
    val mark: Int?,
)
