package ru.kaelesty.madprojects.features.auth.data.storage

import domain.auth.UserType
import ru.kaelesty.madprojects.api.auth.Tokens

interface AuthStorage {

    suspend fun save(item: Item)

    suspend fun load(): Item?

    suspend fun clear()

    data class Item(
        val tokens: Tokens,
        val userType: UserType,
    )
}
