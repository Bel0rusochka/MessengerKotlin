package Server
import java.sql.SQLException

class ServerClientModel(dbName: String): AbstractDbModel(dbName){
    private val dbServerCrypto = ServerCrypto()
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
            stmt?.setString(1, dbServerCrypto.encrypt(item.name,dbServerCrypto.CLIENT_DB_KEY))
            stmt?.setString(2, dbServerCrypto.encrypt(item.password,dbServerCrypto.CLIENT_DB_KEY))
            stmt?.executeUpdate()
        }catch(e: SQLException) {
            println(e.message)
        }

    }

    fun isExistClient(clientName: String): Boolean {
        val sql = "SELECT name FROM clients WHERE name = ?"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, dbServerCrypto.encrypt(clientName,dbServerCrypto.CLIENT_DB_KEY))
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
            stmt?.setString(1, dbServerCrypto.encrypt(clientName,dbServerCrypto.CLIENT_DB_KEY))
            stmt?.setString(2, dbServerCrypto.encrypt(password,dbServerCrypto.CLIENT_DB_KEY))
            val tableData = stmt?.executeQuery()
            tableData!!.getString("name")
            return true
        }catch(e: SQLException) {
            return false
        }
    }
}

data class DataClientServerModel(val name: String, val password: String)