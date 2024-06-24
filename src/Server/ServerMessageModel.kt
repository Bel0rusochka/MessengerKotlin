package Server
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp

class ServerMessageModel(dbName: String):AbstractDbModel(dbName){
    override fun getSQLQuery(): String {
        return """
            CREATE TABLE IF NOT EXISTS messages (
                timestamp TIMESTAMP NOT NULL PRIMARY KEY,
                text TEXT NOT NULL,
                dstClientName TEXT NOT NULL,
                srcClientName TEXT NOT NULL
            );
        """.trimIndent()
    }

    fun insertMessage(message: DataMessageServerModel) {
        val sql = "INSERT INTO messages(timestamp,text,dstClientName,srcClientName) VALUES(?,?,?,?)"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setTimestamp(1, message.timestamp)
            stmt?.setString(2, message.text)
            stmt?.setString(3, message.dstClientName)
            stmt?.setString(4, message.srcClientName)
            stmt?.executeUpdate()
        }catch(e: SQLException) {
            println(e.message)
        }
    }

    fun getAllClientMessages(dstClientName: String): List<DataMessageServerModel> {
        val sql = "SELECT timestamp,text,dstClientName,srcClientName FROM  messages WHERE dstClientName = ? ORDER BY timestamp"
        val messages = mutableListOf<DataMessageServerModel>()
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, dstClientName)
            val tableDates = stmt?.executeQuery()

            while (tableDates?.next() == true) {
                val message = DataMessageServerModel(tableDates.getTimestamp("timestamp"),tableDates.getString("text"), tableDates.getString("dstClientName"), tableDates.getString("srcClientName"))
                messages.add(message)
            }
        } catch (e: SQLException) {
            println(e.message)
        }
        return messages
    }

    fun isExistMessages(dstClientName: String): Boolean {
        val sql = "SELECT timestamp FROM messages WHERE dstClientName = ?"
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, dstClientName)
            val tableDates = stmt?.executeQuery()
            return tableDates?.next() == true
        } catch (e: SQLException) {
            return false
        }
    }

    fun deleteItems(dstClientName: String){
        val sql = "DELETE  FROM messages WHERE dstClientName = ?"
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, dstClientName)
            stmt?.executeUpdate()
        } catch (e: SQLException) {
            println(e.message)
        }
    }

}

data class DataMessageServerModel(val timestamp: Timestamp, val text: String, val dstClientName: String, val srcClientName: String)