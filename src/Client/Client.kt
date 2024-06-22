package Client
import java.io.*
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.*
import TypeMessage

val scanner = Scanner(System.`in`)
val buffer = BufferedReader(InputStreamReader(System.`in`))

class Client(private val name: String, private val password: String, private val host: String, private val port: Int) {
    private var socket: Socket? = null
    private var dataIn: DataInputStream? = null
    private var dataOut: DataOutputStream? = null

    private fun initConnection(): Boolean {
        return try {
            this.socket = Socket()
            this.socket!!.connect(InetSocketAddress(this.host, 5001), 1000)
            this.dataOut = DataOutputStream(socket!!.getOutputStream())
            this.dataIn = DataInputStream(socket!!.getInputStream())
            sendMessage(TypeMessage.START)
            true
        } catch (e: IOException) {
            println("Connection Failed: ${e.message}")
            false
        }
    }

    private fun closeConnection() {
        try {
            this.sendMessage(TypeMessage.BYE)
            this.dataIn?.close()
            this.dataOut?.close()
            this.socket!!.close()
        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    private fun sendMessage(typeMessage: TypeMessage, msg: String? = null, to: String? = null) {
        val timestamp = Instant.now()
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

    fun getMessageFromServer():List<String>{
        val msg = this.dataIn?.readUTF()
        println("Message from server: $msg")
        return msg!!.split("|")
    }

    fun isMessageToServer(): Boolean {
        return buffer.ready()
    }


    private fun communicationWithServer(){

        try {
            var timeStart = System.currentTimeMillis()
            while (true) {
                if (this.isMessageToServer()) {
                    this.sendMessage(TypeMessage.SEND, scanner.nextLine(), "AndreiKulinkovich")
                    timeStart = System.currentTimeMillis()
                }

                if(this.isMessageFromServer()) {
                    val msgList = this.getMessageFromServer()
                    timeStart = System.currentTimeMillis()
                    if(msgList[0]=="Bye") break
                }

                if (System.currentTimeMillis() - timeStart > 10000 )break

            }
        } finally {
            closeConnection()
        }
    }

    fun run() {
        while (true) {
            if (buffer.ready()) {
                if (initConnection()) {
                    this.communicationWithServer()
                } else {
                    sleep(10000)
                }
            }
        }
    }


}

fun main() {
    Client("Anton228","229", "127.0.0.1",5001).run()
}
