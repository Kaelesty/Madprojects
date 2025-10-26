package data.repos

import data.schemas.BanhammerService
import data.schemas.ProjectMembershipService
import domain.BanhammerRepo
import domain.profile.ProfileRepo
import ru.kaelesty.madprojects.api.profile.MinimalUserInfo

class BanhammerRepoImpl(
    private val banhammerService: BanhammerService,
    private val profileRepo: ProfileRepo,
    private val projectMembershipService: ProjectMembershipService,
): BanhammerRepo {

    override suspend fun banUserInProject(victimId: String, projectId: String) {
        banhammerService.banUserInProject(victimId, projectId)

        projectMembershipService.removeProjectMember(victimId.toInt(), projectId.toInt())
    }

    override suspend fun unbanUserInProject(victimId: String, projectId: String) {
        banhammerService.unbanUserInProject(victimId, projectId)
    }

    override suspend fun getBannedUsers(projectId: String): List<MinimalUserInfo> {
        return banhammerService.getBannedUsers(projectId)
            .mapNotNull { userId ->
                profileRepo.getSharedById(userId)?.let { shared ->
                    MinimalUserInfo(
                        id = userId,
                        firstName = shared.firstName,
                        secondName = shared.secondName,
                        lastName = shared.lastName,
                    )
                }
            }

    }

    override suspend fun checkUserIsBanned(projectId: String, userId: String): Boolean {
        return banhammerService.checkUserIsBanned(projectId, userId)
    }
}