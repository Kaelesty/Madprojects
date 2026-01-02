package ru.kaelesty.madprojects.features.checkin.domain

import ru.kaelesty.madprojects.features.checkin.data.CheckInApi

class CheckInUseCase(
    private val api: CheckInApi,
) {
    suspend fun check(): Boolean = api.checkConnection()
}
