package data.repos

import data.schemas.CommonUsersDataService
import data.schemas.ProjectCuratorshipService
import data.schemas.ProjectGroupService
import data.schemas.ProjectMembershipService
import data.schemas.ProjectService
import data.schemas.UserService
import domain.project.ProjectStatus
import domain.projectgroups.ProjectGroup
import domain.projectgroups.ProjectInGroupAnalyticsView
import domain.projectgroups.ProjectInGroupMember
import domain.projectgroups.ProjectInGroupView
import domain.projectgroups.ProjectsGroupRepo
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

private data class CuratorProjectBase(
    val id: Int,
    val title: String,
    val createDate: String,
    val status: ProjectStatus,
    val maxMembersCount: Int,
    val groupTitle: String,
    val groupId: Int,
    val mark: Int?,
)

class ProjectGroupsRepoImpl(
    private val projectGroupsService: ProjectGroupService,
    private val projectCuratorshipService: ProjectCuratorshipService,
    private val projectService: ProjectService,
    private val projectMembershipService: ProjectMembershipService,
    private val userService: UserService,
    private val commonUsersDataService: CommonUsersDataService,
) : ProjectsGroupRepo {

    override suspend fun getGroupId(projectId: String): String {
        return projectCuratorshipService.getGroupId(projectId).toString()
    }

    override suspend fun getGroupTitle(groupId: String): String {
        return projectGroupsService.getGetById(groupId.toInt()).title
    }

    override suspend fun createProjectsGroup(title: String, curatorId: String): ProjectGroup {
        return projectGroupsService.create(curatorId.toInt(), title)
    }

    override suspend fun getCuratorProjectGroups(curatorId: String): List<ProjectGroup> {
        return projectGroupsService
            .getCuratorProjectGroups(curatorId.toInt())
            .map { group ->
                group.copy(
                    pendingProjectsCount = projectCuratorshipService.countPendingProjectsByGroupId(
                        group.id.toInt()
                    )
                )
            }
    }

    override suspend fun getCuratorProjectGroupsLight(curatorId: String): List<ProjectGroup> {
        return projectGroupsService.getCuratorProjectGroups(curatorId.toInt())
    }

    override suspend fun getCuratorProjects(
        curatorId: String,
        projectGroupId: String?,
        status: ProjectStatus?,
        marked: Boolean?,
        mark: Int?,
    ): List<ProjectInGroupView> {
        val groupIds = if (projectGroupId == null) {
            getCuratorProjectGroupsLight(curatorId).map { it.id.toInt() }
        } else {
            listOf(projectGroupId.toInt())
        }

        if (groupIds.isEmpty()) return emptyList()

        return projectGroupsService.dbQuery {
            val pc = ProjectCuratorshipService.ProjectsCuratorship
            val projects = ProjectService.Projects
            val groups = ProjectGroupService.ProjectsGroup
            val membership = ProjectMembershipService.ProjectsMembership
            val users = UserService.Users
            val commonUsers = CommonUsersDataService.CommonUsersData

            val baseProjects = (pc innerJoin projects innerJoin groups)
                .selectAll()
                .where {
                    (pc.projectGroupId inList groupIds) and
                        (
                            (projects.isDeleted eq false) or
                                projects.isDeleted.isNull()
                            ) and
                        (status?.let { pc.status eq it } ?: Op.TRUE) and
                        (
                            marked?.let {
                                if (it) {
                                    pc.mark.isNotNull()
                                } else {
                                    pc.mark.isNull()
                                }
                            } ?: Op.TRUE
                            ) and
                        (mark?.let { pc.mark eq it } ?: Op.TRUE)
                }
                .orderBy(groups.id to SortOrder.ASC, pc.projectId to SortOrder.ASC)
                .map { row ->
                    CuratorProjectBase(
                        id = row[pc.projectId],
                        title = row[projects.title],
                        createDate = row[projects.createDate] ?: "00.00.0000",
                        status = row[pc.status],
                        maxMembersCount = row[projects.maxMembersCount],
                        groupTitle = row[groups.title],
                        groupId = row[pc.projectGroupId],
                        mark = row[pc.mark],
                    )
                }

            if (baseProjects.isEmpty()) return@dbQuery emptyList()

            val projectIds = baseProjects.map { it.id }
            val membersByProject = mutableMapOf<Int, MutableList<ProjectInGroupMember>>()
            val membersSeenByProject = mutableMapOf<Int, MutableSet<Int>>()

            (membership innerJoin users)
                .join(commonUsers, JoinType.LEFT, membership.userId, commonUsers.userId)
                .selectAll()
                .where { membership.projectId inList projectIds }
                .orderBy(
                    membership.projectId to SortOrder.ASC,
                    users.lastName to SortOrder.ASC,
                    users.firstName to SortOrder.ASC,
                )
                .forEach { row ->
                    val projectId = row[membership.projectId]
                    val userId = row[users.id]
                    val projectSeenUsers = membersSeenByProject.getOrPut(projectId) { mutableSetOf() }
                    if (!projectSeenUsers.add(userId)) return@forEach

                    membersByProject.getOrPut(projectId) { mutableListOf() }.add(
                        ProjectInGroupMember(
                            firstName = row[users.firstName],
                            secondName = row[users.secondName],
                            lastName = row[users.lastName],
                            group = row.getOrNull(commonUsers.group) ?: "null",
                        )
                    )
                }

            baseProjects.map { project ->
                ProjectInGroupView(
                    id = project.id.toString(),
                    title = project.title,
                    members = membersByProject[project.id] ?: emptyList(),
                    createDate = project.createDate,
                    status = project.status,
                    maxMembersCount = project.maxMembersCount,
                    groupTitle = project.groupTitle,
                    groupId = project.groupId.toString(),
                    mark = project.mark,
                )
            }
        }
    }

    override suspend fun checkIsCuratorGroupOwner(curatorId: String, groupId: String): Boolean {
        return projectGroupsService.checkIsCuratorGroupOwner(curatorId.toInt(), groupId.toInt())
    }

    override suspend fun getGroupProjects(groupId: String): List<ProjectInGroupView> {
        val group = projectGroupsService.getGetById(groupId.toInt())
        val ids = projectCuratorshipService.getProjectGroupIds(groupId.toInt()).filter { !projectService.isProjectDeleted(it) }
        return ids
            .map { projectId ->

                val project = projectService.getById(projectId)

                ProjectInGroupView(
                    id = projectId.toString(),
                    title = project.title,
                    members = projectMembershipService.getProjectUserIds(projectId.toString()).map {
                        val user = userService.getById(it)
                        if (user == null) {
                            null
                        } else {
                            ProjectInGroupMember(
                                firstName = user.firstName,
                                secondName = user.secondName,
                                lastName = user.lastName,
                                group = commonUsersDataService.getByUser(it) ?: "null"
                            )
                        }
                    }.filterNotNull(),
                    createDate = project.createDate,
                    status = projectCuratorshipService.getStatus(projectId),
                    maxMembersCount = project.maxMembersCount,
                    groupTitle = group.title,
                    mark = projectCuratorshipService.getMark(projectId),
                    groupId = group.id,
                )
            }
    }

    override suspend fun getGroupProjectsAnalytics(groupId: String): List<ProjectInGroupAnalyticsView> {
        return projectGroupsService.dbQuery {
            val pc = ProjectCuratorshipService.ProjectsCuratorship
            val projects = ProjectService.Projects

            (pc innerJoin projects)
                .selectAll()
                .where {
                    (pc.projectGroupId eq groupId.toInt()) and
                        (
                            (projects.isDeleted eq false) or
                                projects.isDeleted.isNull()
                            )
                }
                .orderBy(pc.projectId to SortOrder.ASC)
                .map { row ->
                    ProjectInGroupAnalyticsView(
                        id = row[pc.projectId].toString(),
                        title = row[projects.title],
                        status = row[pc.status],
                        mark = row[pc.mark],
                    )
                }
        }
    }

    override suspend fun deleteProjectGroup(id: String): Boolean {
        return projectGroupsService.deleteProjectGroup(groupId = id) > 0
    }
}
