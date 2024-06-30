package Server
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

abstract class AbstractDbModel(private val dbName: String){
    protected var dbConnection: Connection? = null

    protected abstract fun getSQLQuery(): String
    
    init{
        connect()
        createTable()
    }

    private fun connect() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:src/Server/data/$dbName")
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    private fun createTable() {
        val sql = getSQLQuery()
        try {
            dbConnection?.createStatement()?.execute(sql)
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun close() {
        try {
            dbConnection?.close()
        } catch (e: SQLException) {
            println(e.message)
        }
    }
}


