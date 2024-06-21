package Server

import java.awt.TrayIcon
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import TypeMessage



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
    fun getUsername(): String{
        return this.userName
    }

    fun getPassword(): String{
        return this.password
    }


    fun sendMessage(typeMessage: TypeMessage, msg: String?=null){
        val timestamp = Instant.now()
        when (typeMessage) {
            TypeMessage.BYE -> {
                this.dataOut.writeUTF("Bye|$timestamp")
            }
            TypeMessage.RESPONSE -> {
                this.dataOut.writeUTF("Response|$msg|$timestamp")
                if (!checkSuccess()){
                    throw Exception("Send Error")
                }
            }
            TypeMessage.SUCCESS -> {
                this.dataOut.writeUTF("Success|$timestamp")
            }
            else -> {
                throw Exception("Error type")
            }
        }
    }

    private fun checkSuccess(): Boolean{
        return this.dataIn.readUTF().split("|")[0] == "Success"
    }

    fun closeConnect(){

    }
}

class Server(private val port: Int){
    private var userClient: ArrayList<Client> = arrayListOf()
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
            client.sendMessage(typeMessage = TypeMessage.BYE)
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
                            client.sendMessage(typeMessage = TypeMessage.BYE)
                            break
                        }else{
                            client.sendMessage(typeMessage = TypeMessage.SUCCESS)
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
    Server(5001).run()
}