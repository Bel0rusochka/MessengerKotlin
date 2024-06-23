package Server

import java.awt.TrayIcon
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import Server.ServerClientModel
import TypeMessage

class ExitException(message: String) : Exception(message)

class Client(val socket: Socket, val dataIn: DataInputStream, val dataOut: DataOutputStream){
    private var userName: String = ""
    private var password: String = ""

    fun setName(name: String){
        this.userName = name
    }
    fun setPassword(password: String){
        this.password = password
    }
    fun getUsername(): String{
        return this.userName
    }

    fun getPassword(): String{
        return this.password
    }


    fun sendMessage(typeMessage: TypeMessage, msg: String?=null,srcClient: String?=null){
        val timestamp = Instant.now()
        when (typeMessage) {
            TypeMessage.BYE -> {
                this.dataOut.writeUTF("Bye|$timestamp")
            }
            TypeMessage.RESPONSE -> {
                this.dataOut.writeUTF("Response|$msg|$srcClient|$timestamp")
            }
            else -> {
                throw Exception("Error type")
            }
        }
    }
}

class Server(private val port: Int){
    private var clientsArray: ArrayList<Client> = arrayListOf()
    private val serverSocket = ServerSocket(this.port)
    private val dbClients = ServerClientModel("Server.db")

    private fun connect(): Client{
        val clientSocket = this.serverSocket.accept()
        val newClient = Client(clientSocket,DataInputStream(clientSocket.getInputStream()), DataOutputStream(clientSocket.getOutputStream()))
        this.processMessage(newClient)
        this.clientsArray.add(newClient)
        return newClient
    }

    private fun closeConnection(client: Client){
        try {
           if (!client.socket.isClosed){
               client.dataIn.close()
               client.dataOut.close()
               client.socket.close()

               this.clientsArray.remove(client)
           }

        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    private fun closeServer(){
        clientsArray.forEach {
            it.sendMessage(TypeMessage.BYE)
        }
//        serverSocket.close()
        this.dbClients.close()
    }
    private fun findClient(name: String): Client?{
        clientsArray.forEach {
            if(it.getUsername() == name) return it
        }
        return null
    }
    private fun processMessage(client: Client){
        val msgList = client.dataIn.readUTF().split("|")
        when(msgList[0]){
            "Send"->{
                if(!this.dbClients.itemExists(msgList[2])){
                    client.sendMessage(TypeMessage.RESPONSE,"Can't find destination user","Server")
                }else{
                    val dstClient = findClient(msgList[2])
                    dstClient?.sendMessage(TypeMessage.RESPONSE, msgList[1], client.getUsername())
                }

            }
            "Bye"->{
                println("Bye, bye ${client.getUsername()}")
                throw ExitException("Client ${client.getUsername()} has disconnected.")
            }
            "Start"->{
                client.setName(msgList[1])
                client.setPassword(msgList[2])
                if (!this.dbClients.itemExists(msgList[1])){
                    this.dbClients.insertItem(DbClient(msgList[1],msgList[2]))
                }
                println("User ${client.getUsername()} successfully registered")
            }
        }

    }
    fun run(){
        Runtime.getRuntime().addShutdownHook(Thread {
            synchronized(this) {
                this.closeServer()
            }
        })

        while (true) {
            val client = this.connect()
            Thread {
                try {
                    while (client.socket.isConnected) {
                        if(client.dataIn.available() > 0) {
                            processMessage(client)
                        }
                    }
                }catch (e: ExitException) {
                    println(e.message)
                }finally{
                    this.closeConnection(client)
                }
            }.start()
        }
    }
}

fun main() {
    Server(5001).run()
}