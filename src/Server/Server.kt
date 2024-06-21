package Server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import kotlin.concurrent.thread


class Client(val socket: Socket, val dataIn: DataInputStream, val dataOut: DataOutputStream){
    private var userName: String = ""
    private var password: String = ""

    init {
        val clientRegister = this.dataIn.readUTF().split("|")
        println("ClientRegister: $clientRegister")
        this.userName = clientRegister[1]
        this.password = clientRegister[2]
        val timestamp = Instant.now()
        dataOut.writeUTF("Success|$timestamp")

    }
    fun getName(): String{
        return this.userName
    }

    fun sendMessage(msg: String){

    }
    fun closeConnect(){

    }
}

class Server(private val port: Int){
    var userClient: ArrayList<Client> = arrayListOf()
    private val serverSocket = ServerSocket(this.port)
    private fun connect(): Client{
        val clientSocket = this.serverSocket.accept()
        val clientSocketIP = clientSocket.inetAddress.toString()
        val clientSocketPort = clientSocket.port
        println("[IP: $clientSocketIP ,Port: $clientSocketPort]  Client Connection Successful!")
        val newClient = Client(clientSocket,DataInputStream(clientSocket.getInputStream()), DataOutputStream(clientSocket.getOutputStream()))
        this.userClient.add(newClient)
        return newClient
    }

    private fun closeConnection(client: Client){
        try {
            val timestamp = Instant.now()
            client.dataOut.writeUTF("Bye|$timestamp")
            client.dataIn.close()
            client.dataOut.close()
            client.socket.close()
            this.userClient.remove(client)
        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    fun run(){
        while (true) {
            val client = this.connect()
            Thread {
                try {
                    while (client.socket.isConnected) {
                        val clientMessage = client.dataIn.readUTF()
                        println(clientMessage)
                        if(clientMessage.split("|")[0] == "Bye"){
                            break
                        }else{
                            val timestamp = Instant.now()
                            client.dataOut.writeUTF("Success|$timestamp")
                            println(this.userClient.size)
                            if (this.userClient.size == 2){
                                this.userClient[1].dataOut.writeUTF("Loch")
                            }

                        }
                    }
                }finally {
                    this.closeConnection(client)
                }
            }.start()
        }
    }
}

fun main() {
    val server = Server(5001)
    server.run()
}