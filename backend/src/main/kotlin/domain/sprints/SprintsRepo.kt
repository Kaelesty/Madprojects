package domain.sprints

import ru.kaelesty.madprojects.api.sprints.CreateSprintRequest
import ru.kaelesty.madprojects.api.sprints.UpdateSprintRequest

interface SprintsRepo {

    suspend fun createSprint(
        request: CreateSprintRequest,
    ): String

    suspend fun getProjectSprints(projectId: String): List<ProfileSprint>

    suspend fun finishSprint(sprintId: String)

    suspend fun getSprintProjectId(sprintId: String): String

    suspend fun getSprint(sprintId: String): Sprint

    suspend fun updateSprint(
        request: UpdateSprintRequest
    )
}
