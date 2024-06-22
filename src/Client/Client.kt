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

class Client(val name: String, val password: String, val host: String, val port: Int) {
    var socket: Socket? = null
    var dataIn:DataInputStream? = null
    var dataOut:DataOutputStream? = null

    fun initConnection():Boolean{
        return try {
            this.socket = Socket()
            this.socket!!.connect(InetSocketAddress(this.host, this.port), 1000)
            this.dataOut = DataOutputStream(socket!!.getOutputStream())
            this.dataIn = DataInputStream(socket!!.getInputStream())
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

    fun sendMessage(typeMessage: TypeMessage, msg: String?=null,to: String?=null){
        val timestamp = Instant.now()
        when (typeMessage) {
            TypeMessage.BYE -> {
                this.dataOut?.writeUTF("Bye|$timestamp")
                if (!checkResponse(TypeMessage.BYE)){
                    throw Exception("Response Error")
                }
            }
            TypeMessage.SUCCESS -> {
                this.dataOut?.writeUTF("Success|$timestamp")
            }
            TypeMessage.START -> {
                this.dataOut?.writeUTF("Start|${this.name}|${password}|$timestamp")
                if (!checkResponse(TypeMessage.SUCCESS)){
                    throw Exception("Response Error")
                }
            }
            TypeMessage.SEND -> {
                this.dataOut?.writeUTF("Send|$msg|$to|$timestamp")
                if (!checkResponse(TypeMessage.SUCCESS)){
                    throw Exception("Response Error")
                }
            }
            else -> {
                throw Exception("Error type")
            }
        }
    }

    private fun checkResponse(type:TypeMessage):Boolean{
        return when(type){
            TypeMessage.BYE -> {
                this.dataIn?.readUTF()!!.split("|")[0] == "Bye"
            }

            TypeMessage.SUCCESS -> {
                this.dataIn?.readUTF()!!.split("|")[0] == "Success"
            }else -> {
                false
            }
        }
    }

    fun run(){
        while (true) {
            if (buffer.ready()){
                if(this.initConnection()){
                    this.sendMessage(TypeMessage.START)
                    try {
                        var timeStart = System.currentTimeMillis()
                        while (true){
                            if(buffer.ready()){
                                sendMessage(TypeMessage.SEND, scanner.nextLine(), "Anton228")
                                timeStart = System.currentTimeMillis()
                            }else if(System.currentTimeMillis()- timeStart  > 10000) {
                                break
                            }
                        }
                    }finally {
                        this.closeConnection()
                    }
                }else{
                    sleep(10000)
                }
            }
        }
    }
}

fun main() {
    Client("AndreiKulinkovich","229", "127.0.0.1",5001).run()
}
