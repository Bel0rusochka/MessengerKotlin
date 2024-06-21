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

class Client(val name: String, val password: String) {
    val socket: Socket = Socket()
    var dataIn:DataInputStream? = null
    var dataOut:DataOutputStream? = null

    fun initConnection(host: String, port: Int):Boolean{
        return try {
            this.socket.connect(InetSocketAddress(host, port), 1000)
            this.dataOut = DataOutputStream(socket.getOutputStream())
            this.dataIn = DataInputStream(socket.getInputStream())
            true
        } catch (e: IOException) {
            println("Connection Failed: ${e.message}")
            false
        }
    }

    fun closeConnection() {
        try {
            this.dataIn?.close()
            this.dataOut?.close()
            this.socket.close()
        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    fun sendMessage(typeMessage: TypeMessage, msg: String?=null){
        val timestamp = Instant.now()
        when (typeMessage) {
            TypeMessage.BYE -> {
                this.dataOut?.writeUTF("Bye|$timestamp")
                if (!checkResponse(TypeMessage.BYE)){
                    throw Exception("Send Error")
                }
            }
            TypeMessage.SUCCESS -> {
                this.dataOut?.writeUTF("Success|$timestamp")
            }
            TypeMessage.START -> {
                this.dataOut?.writeUTF("Start|${this.name}|${password}|$timestamp")
                if (!checkResponse(TypeMessage.SUCCESS)){
                    throw Exception("Send Error")
                }
            }
            TypeMessage.SEND -> {
                this.dataOut?.writeUTF("Send|$msg|$timestamp")
                if (!checkResponse(TypeMessage.SUCCESS)){
                    throw Exception("Send Error")
                }
            }
            else -> {
                throw Exception("Error type")
            }
        }
    }

    fun checkResponse(type:TypeMessage):Boolean{
        when(type){
            TypeMessage.BYE -> {
                return this.dataIn?.readUTF()!!.split("|")[0] == "Bye"
            }
            TypeMessage.SUCCESS -> {
                return this.dataIn?.readUTF()!!.split("|")[0] == "Success"
            }else -> {
                return false
            }
        }
    }
}


fun sendMessage(scanner: Scanner, dataInputStream: DataInputStream, dataOutputStream: DataOutputStream, text:String="Send|${scanner.nextLine()}|Anton228|${Instant.now()}"){
    dataOutputStream.writeUTF(text)
    println(dataInputStream.readUTF())
}


fun run() {
    while (true) {
        if (buffer.ready()) {

            if (socket != null){

                val timestamp = Instant.now()
                sendMessage(scanner, dataIn, dataOut, "Start|aaa|1234|${timestamp}")
                sendMessage(scanner, dataIn, dataOut)
                try {
                    var timeStart = System.currentTimeMillis()
                    while (!Thread.currentThread().isInterrupted) {
                        if(buffer.ready()){
                            sendMessage(scanner, dataIn, dataOut)
                            timeStart = System.currentTimeMillis()
                        }else if(System.currentTimeMillis()- timeStart  > 10000) {
                            break
                        }
                    }
                }finally {
                    val timestamp = Instant.now()
                    sendMessage(scanner, dataIn, dataOut, "Bye|${timestamp}")
                    closeConnection(dataIn, dataOut, socket)
                }
            }else{
                sleep(10000)
            }
        }
    }
}

fun main() {
    run()
}
