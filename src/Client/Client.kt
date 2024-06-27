package Client
import java.io.*
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
    private val dbMessages = ClientMessageModel("${name}Message.db")
    private var connectFlag = false

    fun getName():String{
        return this.name
    }

    fun getConnectStatus():Boolean{
        return this.connectFlag
    }

    fun initConnection() {
        return try {
            this.socket = Socket()
            this.socket!!.connect(InetSocketAddress("127.0.0.1",5001), 1000)
            this.dataOut = DataOutputStream(socket!!.getOutputStream())
            this.dataIn = DataInputStream(socket!!.getInputStream())
            sendMessage(TypeMessage.START)
            connectFlag = socket!!.isConnected
        } catch (e: IOException) {
            println("Connection Failed: ${e.message}")
        }
    }

    fun closeConnection() {
        try {
            connectFlag = false
            this.sendMessage(TypeMessage.BYE)
            this.dataIn?.close()
            this.dataOut?.close()
            this.socket!!.close()
        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    fun closeDB() {
        this.dbMessages.close()
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
        return this.dbMessages.getAllClientMessages(clientName,this.getName())
    }

    fun deleteAllMessagesWithConvClient(clientName: String){
        this.dbMessages.deleteAllMessagesWithClient(clientName)
    }

    fun getAllConverClientNames():List<String>{
        return this.dbMessages.getAllClientConverNames()
    }

}

