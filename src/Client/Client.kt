package Client
import java.io.*
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.*

val scanner = Scanner(System.`in`)
val buffer = BufferedReader(InputStreamReader(System.`in`))
fun initConnection(host: String, port: Int): Socket? {
    return try {
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), 1000)
        socket
    } catch (e: IOException) {
        println("Connection Failed: ${e.message}")
        null
    }
}

fun closeConnection(dataInputStream: DataInputStream, dataOutputStream: DataOutputStream, socket: Socket) {
    try {
        dataOutputStream.close()
        dataInputStream.close()
        socket.close()
    } catch (e: IOException) {
        println("Failed to close connection: ${e.message}")
    }
}
fun sendMessage(scanner: Scanner, dataInputStream: DataInputStream, dataOutputStream: DataOutputStream, text:String="Send|${scanner.nextLine()}|Anton228|${Instant.now()}"){
    dataOutputStream.writeUTF(text)
    println(dataInputStream.readUTF())
}

fun run() {
    while (true) {
        if (buffer.ready()) {
            val socket = initConnection("127.0.0.1", 5001)
            if (socket != null){
                val dataIn = DataInputStream(socket.getInputStream())
                val dataOut = DataOutputStream(socket.getOutputStream())
                val timestamp = Instant.now()
                sendMessage(scanner, dataIn, dataOut, "Start|aaa|1234|${timestamp}")
                sendMessage(scanner, dataIn, dataOut)
                try {
                    var timeStart = System.currentTimeMillis()
                    while (!Thread.currentThread().isInterrupted) {
                        if(buffer.ready()){
                            sendMessage(scanner, dataIn, dataOut)
                            timeStart = System.currentTimeMillis()
                        }else if(System.currentTimeMillis()- timeStart  > 50000000) {
                            break
                        }
                    }
                }finally {
                    val timestamp = Instant.now()
                    sendMessage(scanner, dataIn, dataOut, "Bye|AndreiKulinkovich|1234|${timestamp}")
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
