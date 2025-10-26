package domain.curatorship

import domain.CuratorshipRepo
import domain.profile.ProfileRepo

class CheckCuratorshipUseCase(
    private val profileRepo: ProfileRepo,
    private val curatorshipRepo: CuratorshipRepo
) {

    suspend fun requireProjectCurator(userId: String, projectId: String): Boolean {
        if (!profileRepo.checkIsCurator(userId)) {
            return false
        }
        if (!curatorshipRepo.checkUserIsProjectCurator(userId, projectId)) {
            return false
        }

        return true
    }
}