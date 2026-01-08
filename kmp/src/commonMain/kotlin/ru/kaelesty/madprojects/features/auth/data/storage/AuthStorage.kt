package ru.kaelesty.madprojects.features.auth.data.storage

import domain.auth.UserType

interface AuthStorage {

    suspend fun save(item: Item)

    suspend fun load(): Item?

    suspend fun clear()

    data class Item(
        val access: String,
        val refresh: String,
        val userType: UserType
    )
}