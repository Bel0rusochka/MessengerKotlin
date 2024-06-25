package Client
import Server.*
import java.io.*
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import TypeMessage
import java.sql.Timestamp
import java.text.SimpleDateFormat


class Client(private val name: String, private val password: String) {
    private var socket: Socket? = null
    private var dataIn: DataInputStream? = null
    private var dataOut: DataOutputStream? = null
    private val dbMessages = ClientMessageModel("ClientMessage.db")
    init{
        initConnection()
    }
    fun getName():String{
        return this.name
    }

    private fun initConnection(): Boolean {
        return try {
            this.socket = Socket()
            this.socket!!.connect(InetSocketAddress("127.0.0.1",5001), 1000)
            this.dataOut = DataOutputStream(socket!!.getOutputStream())
            this.dataIn = DataInputStream(socket!!.getInputStream())
            sendMessage(TypeMessage.START)
            true
        } catch (e: IOException) {
            println("Connection Failed: ${e.message}")
            false
        }
    }

    fun closeConnection() {
        try {
            this.sendMessage(TypeMessage.BYE)
            this.dataIn?.close()
            this.dataOut?.close()
            this.socket!!.close()
            this.dbMessages.close()
        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    private fun transformStringToTimestamp(date: String): Timestamp{
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val parsedDate = dateFormat.parse(date)
        return Timestamp(parsedDate.time)
    }

    fun sendMessage(typeMessage: TypeMessage, msg: String? = null, to: String? = null) {
        val timestamp = Timestamp(Date().time)
        try {
            when (typeMessage) {
                TypeMessage.BYE -> {
                    this.dataOut?.writeUTF("Bye|$timestamp")
                }

                TypeMessage.START -> {
                    this.dataOut?.writeUTF("Start|${this.name}|${password}|$timestamp")
                }

                TypeMessage.SEND -> {
                    this.dataOut?.writeUTF("Send|$msg|$to|$timestamp")
                    dbMessages.insertMessage(DataMessageClientModel(timestamp, msg!!, to!!, "Send"))
                }

                else -> {
                    throw Exception("Error type")
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    fun isMessageFromServer(): Boolean {
        return this.dataIn!!.available() > 0
    }

    fun processMessageFromServer():String{
        val msgList = this.dataIn?.readUTF()!!.split("|")
        when(msgList[0]){
            "Bye"->{
                return "Bye"
            }
            "Response"->{
                val text = msgList[1]
                val srcClientName = msgList[2]
                val timestamp = transformStringToTimestamp(msgList[3])
                if(srcClientName=="Server"){
                    return "Unfindable"
                }else{
                    dbMessages.insertMessage(DataMessageClientModel(timestamp,text,srcClientName,"Response"))
                    return "Response"
                }
            }else -> {
                throw Error("Server Error")
            }
        }

    }

    fun getAllMessageWith(clientName: String): List<String>{
        return this.dbMessages.getAllClientMessages(clientName)
    }

    fun isExistMessageWith(clientName: String): Boolean{
        return this.dbMessages.clientConverNameExists(clientName)
    }

    fun deleteAllMessagesWithConvClient(clientName: String){
        this.dbMessages.deleteAllMessagesWithClient(clientName)
    }

    fun getAllConverClientNames():List<String>{
        return this.dbMessages.getAllClientConverNames()
    }

}

