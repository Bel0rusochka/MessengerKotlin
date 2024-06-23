package Server
import java.sql.Connection

import java.util.Objects

interface DbModel<T> {
    val dbName: String
    var dbConnection: Connection?
    fun connect()
    fun close()
    fun createTable()
    fun insertItem(item: T)
    fun getItem(primerKey: String): T
    fun getAllItems():List<T>
    fun itemExists(primerKey: String): Boolean
}
