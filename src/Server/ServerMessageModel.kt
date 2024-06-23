package Server
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp

class ServerMessageModel(override val dbName: String):DbModel<DataMessageServerModel> {
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
            CREATE TABLE IF NOT EXISTS messages (
                timestamp TIMESTAMP NOT NULL PRIMARY KEY,
                text TEXT NOT NULL,
                dstClientName TEXT NOT NULL,
                srcClientName TEXT NOT NULL
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

    override fun insertItem(item: DataMessageServerModel) {
        val sql = "INSERT INTO messages(timestamp,text,dstClientName,srcClientName) VALUES(?,?,?,?)"
        try {
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setTimestamp(1, item.timestamp)
            stmt?.setString(2, item.text)
            stmt?.setString(3, item.dstClientName)
            stmt?.setString(4, item.srcClientName)
            stmt?.executeUpdate()
        }catch(e: SQLException) {
            println(e.message)
        }
    }

    fun getAllClientItems(dstClientName: String): List<DataMessageServerModel> {
        val sql = "SELECT timestamp,text,dstClientName,srcClientName FROM  messages WHERE dstClientName = ? ORDER BY timestamp"
        val items = mutableListOf<DataMessageServerModel>()
        try{
            val stmt = dbConnection?.prepareStatement(sql)
            stmt?.setString(1, dstClientName)
            val tableDates = stmt?.executeQuery()

            while (tableDates?.next() == true) {
                val item = DataMessageServerModel(tableDates.getTimestamp("timestamp"),tableDates.getString("text"), tableDates.getString("dstClientName"), tableDates.getString("srcClientName"))
                items.add(item)
            }
        } catch (e: SQLException) {
            println(e.message)
        }

        return items
    }

    fun itemsExists(dstClientName: String): Boolean {
        val sql = "SELECT timestamp,text,dstClientName,srcClientName FROM  messages WHERE dstClientName = ?"
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