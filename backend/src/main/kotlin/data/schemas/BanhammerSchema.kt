package data.schemas

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class BanhammerService(
    private val database: Database
) {

    object Bans : Table() {
        val id = integer("id").autoIncrement()
        val victimId = integer("victim_id")
            .references(UserService.Users.id)
        val projectId = integer("project_id")
            .references(ProjectService.Projects.id)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Bans)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T) =
        newSuspendedTransaction(db = database, context = Dispatchers.IO) { block() }

    suspend fun banUserInProject(victimId: String, projectId: String) = dbQuery {
        val exists = Bans.selectAll()
            .where {
                (Bans.victimId eq victimId.toInt()) and
                        (Bans.projectId eq projectId.toInt())
            }
            .empty().not()

        if (!exists) {
            Bans.insert {
                it[Bans.victimId] = victimId.toInt()
                it[Bans.projectId] = projectId.toInt()
            }
        }
    }

    suspend fun unbanUserInProject(victimId: String, projectId: String) = dbQuery {
        Bans.deleteWhere {
            (Bans.victimId eq victimId.toInt()) and
                    (Bans.projectId eq projectId.toInt())
        }
    }

    suspend fun getBannedUsers(projectId: String): List<String> = dbQuery {
        Bans.selectAll()
            .where { Bans.projectId eq projectId.toInt() }
            .map { it[Bans.victimId].toString() }
    }

    suspend fun checkUserIsBanned(projectId: String, userId: String): Boolean = dbQuery {
        Bans.selectAll()
            .where {
                (Bans.projectId eq projectId.toInt()) and
                        (Bans.victimId eq userId.toInt())
            }
            .any()
    }
}
