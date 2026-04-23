package domain

import ru.kaelesty.madprojects.api.profile.MinimalUserInfo

interface BanhammerRepo {

    suspend fun banUserInProject(victimId: String, projectId: String)

    suspend fun unbanUserInProject(victimId: String, projectId: String)

    suspend fun getBannedUsers(projectId: String): List<MinimalUserInfo>

    suspend fun checkUserIsBanned(projectId: String, userId: String): Boolean
}