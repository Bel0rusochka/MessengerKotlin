package Server
import java.sql.Connection

import java.util.Objects

interface DbModel {
    val dbName: String
    var dbConnection: Connection?
    fun connect()
    fun close()
    fun createTable()
    fun insertItem(obj: Objects)
    fun getItem(primerKey: String): Objects
    fun getAllItems():List<Objects>
    fun isItem(primerKey: String): Boolean
}
