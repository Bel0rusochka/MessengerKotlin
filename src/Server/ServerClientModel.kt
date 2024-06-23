package Server

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ServerClientModel(override val dbName: String): DbModel<DataClient>{
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

    override fun insertItem(item: DataClient) {
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

    fun getItem(primerKey: String): DataClient {
        val sql = "SELECT name, password FROM clients WHERE name = ?"
        var item:DataClient? = null
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, primerKey)
            val tableData = stmt?.executeQuery()
            item = DataClient(tableData!!.getString("name"), tableData.getString("password"))
        }catch(e: SQLException) {
            println(e.message)
        }

        return item!!
    }

    fun getAllItems(): List<DataClient> {
        val sql = "SELECT name, password FROM clients"
        val items = mutableListOf<DataClient>()
        try {
            val stmt = dbConnection?.createStatement()
            val tableData = stmt?.executeQuery(sql)

            while (tableData?.next() == true) {
                val item = DataClient(tableData.getString("name"), tableData.getString("password"))
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
            val tableData = stmt?.executeQuery()
            DataClient(tableData!!.getString("name"), tableData.getString("password"))
            return true
        }catch(e: SQLException) {
            return false
        }

    }

}

data class DataClient(val name: String, val password: String)