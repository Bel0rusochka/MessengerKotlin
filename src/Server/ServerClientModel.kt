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

    fun isClientPasswordCorrect(clientName: String,password: String): Boolean {
        val sql = "SELECT name FROM clients WHERE name = ? AND password = ?"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, clientName)
            stmt?.setString(2, password)
            val tableData = stmt?.executeQuery()
            tableData!!.getString("name")
            return true
        }catch(e: SQLException) {
            return false
        }
    }
}

data class DataClientServerModel(val name: String, val password: String)