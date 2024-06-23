package Server

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ServerClientModel(override val dbName: String): DbModel<DbClient>{
    override var dbConnection: Connection? = null
    init {
        connect()
        createTable()
    }
    override fun connect() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:src/Server/$dbName")
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS clients (
                name TEXT NOT NULL PRIMARY KEY,
                password TEXT NOT NULL
            );
        """.trimIndent()

        try {
            dbConnection?.createStatement()?.execute(sql)

        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun close() {
        try {
            dbConnection?.close()
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun insertItem(item: DbClient) {
        val sql = "INSERT INTO clients(name,password) VALUES(?,?)"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, item.name)
            stmt?.setString(2, item.password)
            stmt?.executeUpdate()
        }catch(e: SQLException) {
            println(e.message)
        }

    }

    fun getItem(primerKey: String): DbClient {
        val sql = "SELECT name, password FROM clients WHERE name = ?"
        var item:DbClient? = null
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, primerKey)
            val tableDates = stmt?.executeQuery()
            item = DbClient(tableDates!!.getString("name"), tableDates.getString("password"))
        }catch(e: SQLException) {
            println(e.message)
        }

        return item!!
    }

    fun getAllItems(): List<DbClient> {
        val sql = "SELECT name, password FROM clients"
        val items = mutableListOf<DbClient>()
        try {
            val stmt = dbConnection?.createStatement()
            val tableDates = stmt?.executeQuery(sql)

            while (tableDates?.next() == true) {
                val item = DbClient(tableDates.getString("name"), tableDates.getString("password"))
                items.add(item)
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        return items
    }

    fun itemExists(primerKey: String): Boolean {
        val sql = "SELECT name, password FROM clients WHERE name = ?"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, primerKey)
            val tableDates = stmt?.executeQuery()
            DbClient(tableDates!!.getString("name"), tableDates.getString("password"))
            return true
        }catch(e: SQLException) {
            return false
        }

    }

}

data class DbClient(val name: String, val password: String)