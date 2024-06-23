package Server
import java.sql.Connection


interface DbModel<T> {
    val dbName: String
    var dbConnection: Connection?
    fun connect()
    fun close()
    fun createTable()
    fun insertItem(item: T)
}
