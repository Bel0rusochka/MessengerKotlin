package Client
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp
import java.time.format.DateTimeFormatter

class ClientMessageModel(private val dbName: String){
    private var dbConnection: Connection? = null
    init {
        connect()
        createTable()
    }
    private fun connect() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:src/Client/$dbName")
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS messages (
                timestamp TIMESTAMP NOT NULL PRIMARY KEY,
                text TEXT NOT NULL,
                clientConverName TEXT NOT NULL,
                type TEXT NOT NULL
            );
        """.trimIndent()

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

    fun insertMessage(message: DataMessageClientModel) {
        val sql = "INSERT INTO messages(timestamp,text,clientConverName,type) VALUES(?,?,?,?)"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setTimestamp(1, message.timestamp)
            stmt?.setString(2, message.text)
            stmt?.setString(3, message.clientConverName)
            stmt?.setString(4, message.type)
            stmt?.executeUpdate()
        }catch(e: SQLException) {
            println(e.message)
        }
    }

    fun getAllClientMessages(clientConverName: String, clintOwnName: String, operation: (String, String) -> String, strKey:String): List<String> {
        val sql = "SELECT timestamp,text,clientConverName,type FROM  messages WHERE clientConverName = ? ORDER BY timestamp"
        val messages = mutableListOf<String>()
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, clientConverName)
            val tableData = stmt?.executeQuery()

            while (tableData?.next() == true) {
                val timestampFromDB = tableData.getTimestamp("timestamp")
                val localDateTime = timestampFromDB.toLocalDateTime()
                val formattedDateTime = localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

                val textFromDB = operation(tableData.getString("text"), strKey)
                val clientConverNameFromDB = tableData.getString("clientConverName")
                val typeFromDB = tableData.getString("type")

                val message = if(typeFromDB == "Response"){
                    "[$formattedDateTime] $clientConverNameFromDB: $textFromDB"
                }else{
                    "[$formattedDateTime] $clintOwnName: $textFromDB"
                }
                messages.add(message)
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        return messages
    }

    fun clientConverNameExists(clientConverName: String): Boolean {
        val sql = "SELECT clientConverName FROM messages WHERE clientConverName = ?"
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, clientConverName)
            val tableData = stmt?.executeQuery()
            return tableData?.next() == true
        } catch (e: SQLException) {
            return false
        }
    }

    fun getAllClientConverNames():List<String>{
        val sql = "SELECT DISTINCT clientConverName FROM messages ORDER BY timestamp"
        val clientNames = mutableListOf<String>()
        try{

            val stmt = dbConnection?.createStatement()
            val tableData = stmt?.executeQuery(sql)
            while (tableData?.next() == true) {
                clientNames.add(tableData.getString("clientConverName"))
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        return clientNames
    }

    fun deleteAllMessagesWithClient(clientConverName: String){
        val sql = "DELETE FROM messages WHERE clientConverName = ?"
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, clientConverName)
            stmt?.executeUpdate()
        } catch (e: SQLException) {
            println(e.message)
        }
    }
}

data class DataMessageClientModel(val timestamp: Timestamp, val text: String, val clientConverName: String, val type: String)