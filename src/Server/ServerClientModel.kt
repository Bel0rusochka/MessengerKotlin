package Server
import java.sql.SQLException

class ServerClientModel(dbName: String): AbstractDbModel(dbName){
    override fun getSQLQuery():String{
        return """
            CREATE TABLE IF NOT EXISTS clients (
                name TEXT NOT NULL PRIMARY KEY,
                password TEXT NOT NULL
            );
        """.trimIndent()
    }

    fun insertClient(item: DataClientServerModel) {
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

    fun getClient(clientName: String): DataClientServerModel {
        val sql = "SELECT name, password FROM clients WHERE name = ?"
        var client:DataClientServerModel? = null
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, clientName)
            val tableData = stmt?.executeQuery()
            client = DataClientServerModel(tableData!!.getString("name"), tableData.getString("password"))
        }catch(e: SQLException) {
            println(e.message)
        }
        return client!!
    }

    fun getAllClients(): List<DataClientServerModel> {
        val sql = "SELECT name, password FROM clients"
        val clients = mutableListOf<DataClientServerModel>()
        try {
            val stmt = dbConnection?.createStatement()
            val tableData = stmt?.executeQuery(sql)

            while (tableData?.next() == true) {
                val client = DataClientServerModel(tableData.getString("name"), tableData.getString("password"))
                clients.add(client)
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        return clients
    }

    fun isExistClient(clientName: String): Boolean {
        val sql = "SELECT name FROM clients WHERE name = ?"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, clientName)
            val tableData = stmt?.executeQuery()
            tableData!!.getString("name")
            return true
        }catch(e: SQLException) {
            return false
        }
    }
}

data class DataClientServerModel(val name: String, val password: String)