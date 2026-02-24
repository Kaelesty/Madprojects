package ru.kaelesty.madprojects.features.profile.data

import domain.profile.CommonProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import ru.kaelesty.madprojects.api.projectgroups.GroupProjectsResponse
import ru.kaelesty.madprojects.api.profile.CuratorProfileResponse
import ru.kaelesty.madprojects.api.profile.UpdateProfileRequest
import ru.kaelesty.madprojects.utils.KLogger

class CommonProfileApi(
    private val client: HttpClient,
) {
    suspend fun getCommonProfile(accessToken: String): CommonProfileApiResponse? {
        return runCatching {
            val response = client.get(CommonProfilePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Common profile response status=${response.status}" }
            val profile = if (response.status == HttpStatusCode.OK) {
                response.body<CommonProfileResponse>()
            } else {
                null
            }
            CommonProfileApiResponse(status = response.status, profile = profile)
        }.getOrElse {
            KLogger.e(TAG, it) { "Common profile request failed" }
            null
        }
    }

    suspend fun getCuratorProfile(accessToken: String): CuratorProfileApiResponse? {
        return runCatching {
            val response = client.get(CuratorProfilePath) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Curator profile response status=${response.status}" }
            val profile = if (response.status == HttpStatusCode.OK) {
                response.body<CuratorProfileResponse>()
            } else {
                null
            }
            CuratorProfileApiResponse(
                status = response.status,
                profile = profile,
                errorMessage = if (response.status != HttpStatusCode.OK) {
                    response.bodyAsText().trim().takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Curator profile request failed" }
            null
        }
    }

    suspend fun updateCommonProfile(accessToken: String, request: UpdateProfileRequest): SimpleApiResponse? {
        return updateProfile(accessToken, request, UpdateCommonProfilePath, "Update common profile")
    }

    suspend fun updateCuratorProfile(accessToken: String, request: UpdateProfileRequest): SimpleApiResponse? {
        return updateProfile(accessToken, request, UpdateCuratorProfilePath, "Update curator profile")
    }

    suspend fun deleteProjectGroup(accessToken: String, projectGroupId: String): SimpleApiResponse? {
        return runCatching {
            val response = client.post("$DeleteProjectGroupPath?projectGroupId=$projectGroupId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Delete project group response status=${response.status}" }
            SimpleApiResponse(
                status = response.status,
                errorMessage = if (response.status != HttpStatusCode.OK) {
                    response.bodyAsText().trim().takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Delete project group request failed" }
            null
        }
    }

    suspend fun getGroupProjects(accessToken: String, groupId: String): GroupProjectsApiResponse? {
        return runCatching {
            val response = client.get("$GetGroupProjectsPath?groupId=$groupId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                expectSuccess = false
            }
            KLogger.d(TAG) { "Group projects response status=${response.status}" }
            val body = if (response.status == HttpStatusCode.OK) {
                response.body<GroupProjectsResponse>()
            } else {
                null
            }
            GroupProjectsApiResponse(
                status = response.status,
                payload = body,
                errorMessage = if (response.status != HttpStatusCode.OK) {
                    response.bodyAsText().trim().takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "Group projects request failed" }
            null
        }
    }

    private suspend fun updateProfile(
        accessToken: String,
        request: UpdateProfileRequest,
        path: String,
        logPrefix: String,
    ): SimpleApiResponse? {
        return runCatching {
            val response = client.post(path) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
                expectSuccess = false
            }
            KLogger.d(TAG) { "$logPrefix response status=${response.status}" }
            SimpleApiResponse(
                status = response.status,
                errorMessage = if (response.status != HttpStatusCode.OK) {
                    response.bodyAsText().trim().takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            )
        }.getOrElse {
            KLogger.e(TAG, it) { "$logPrefix request failed" }
            null
        }
    }

    data class CommonProfileApiResponse(
        val status: HttpStatusCode,
        val profile: CommonProfileResponse? = null,
    )

    data class CuratorProfileApiResponse(
        val status: HttpStatusCode,
        val profile: CuratorProfileResponse? = null,
        val errorMessage: String? = null,
    )

    data class SimpleApiResponse(
        val status: HttpStatusCode,
        val errorMessage: String? = null,
    )

    data class GroupProjectsApiResponse(
        val status: HttpStatusCode,
        val payload: GroupProjectsResponse? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        private const val TAG = "ktor-CommonProfileApi"
        const val CommonProfilePath = "/commonProfile"
        const val CuratorProfilePath = "/curatorProfile"
        const val UpdateCommonProfilePath = "/commonProfile/update"
        const val UpdateCuratorProfilePath = "/curatorProfile/update"
        const val DeleteProjectGroupPath = "/projectGroup/delete"
        const val GetGroupProjectsPath = "/projectgroup/getGroupProjects"
    }
}
