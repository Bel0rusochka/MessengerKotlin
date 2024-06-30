package Server

import java.sql.Timestamp
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import TypeMessage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList



class ExitException(message: String) : Exception(message)

class ServerClient(val socket: Socket, val dataIn: DataInputStream, val dataOut: DataOutputStream){
    private var userName: String = ""
    private var password: String = ""
    private var serverClientCrypto = ServerCrypto()

    fun setName(name: String){
        this.userName = name
    }
    fun setPassword(password: String){
        this.password = password
    }
    fun getClientName(): String{
        return this.userName
    }

    fun sendMessage(typeMessage: TypeMessage, msg: String?=null,srcClient: String?=null, timestamp: Timestamp=Timestamp(Date().time)){
        when (typeMessage) {
            TypeMessage.BYE -> {
                this.dataOut.writeUTF(serverClientCrypto.encrypt("Bye|$timestamp", serverClientCrypto.CLIENT_SERVER_KEY))
            }
            TypeMessage.RESPONSE -> {
                this.dataOut.writeUTF(serverClientCrypto.encrypt("Response|$msg|$srcClient|${timestamp}",serverClientCrypto.CLIENT_SERVER_KEY))
            }
            TypeMessage.FAILURE ->{
                this.dataOut.writeUTF(serverClientCrypto.encrypt("Failure|$timestamp", serverClientCrypto.CLIENT_SERVER_KEY))
            }
            TypeMessage.SUCCESS ->{
                this.dataOut.writeUTF(serverClientCrypto.encrypt("Success|$timestamp", serverClientCrypto.CLIENT_SERVER_KEY))
            }
            else -> {
                throw Exception("Error type")
            }
        }
    }
}

class Server(private val port: Int){
    private var clientsArray: ArrayList<ServerClient> = arrayListOf()
    private val serverSocket = ServerSocket(this.port)
    private val dbClients = ServerClientModel("ServerClients.db")
    private val dbMessages = ServerMessageModel("ServerMessage.db")
    private val serverCrypto = ServerCrypto()


    private fun connect(): ServerClient{
        val clientSocket = this.serverSocket.accept()
        val newServerClient = ServerClient(clientSocket,DataInputStream(clientSocket.getInputStream()), DataOutputStream(clientSocket.getOutputStream()))
        this.processMessage(newServerClient)
        this.clientsArray.add(newServerClient)
        return newServerClient
    }

    private fun closeConnection(serverClient: ServerClient){
        try {
           if (!serverClient.socket.isClosed){
               serverClient.dataIn.close()
               serverClient.dataOut.close()
               serverClient.socket.close()
               this.clientsArray.remove(serverClient)
           }

        } catch (e: IOException) {
            println("Failed to close connection: ${e.message}")
        }
    }

    private fun closeServer(){
        clientsArray.forEach {
            it.sendMessage(TypeMessage.BYE)
        }
        this.dbClients.close()
        this.dbMessages.close()
    }

    private fun findActiveClient(name: String): ServerClient?{
        clientsArray.forEach {
            if(it.getClientName() == name) return it
        }
        return null
    }

    private fun transformStringToTimestamp(date: String): Timestamp{
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val parsedDate = dateFormat.parse(date)
        return Timestamp(parsedDate.time)
    }

    private fun processMessage(serverClient: ServerClient){
        val msg = serverCrypto.decrypt(serverClient.dataIn.readUTF(), serverCrypto.CLIENT_SERVER_KEY)
        val msgList = msg.split("|")
        when(msgList[0]){
            "Send"->{
                val text = msgList[1]
                val dstClientName = msgList[2]
                val timestamp = transformStringToTimestamp(msgList[3])
                val srcClientName = serverClient.getClientName()

                if(!this.dbClients.isExistClient(dstClientName)){
                    serverClient.sendMessage(TypeMessage.RESPONSE,"Can't find destination user","Server",timestamp)
                }else{
                    val dstClient = findActiveClient(dstClientName)
                    if(dstClient==null){
                        this.dbMessages.insertMessage(DataMessageServerModel(timestamp,text,dstClientName,srcClientName))
                    }else{
                        dstClient.sendMessage(TypeMessage.RESPONSE, text, serverClient.getClientName(),timestamp)
                    }

                }
            }
            "Bye"->{
                println("Bye, bye ${serverClient.getClientName()}")
                throw ExitException("Client ${serverClient.getClientName()} has disconnected.")
            }
            "Start"->{
                val name = msgList[1]
                val password = msgList[2]
                serverClient.setName(name)
                serverClient.setPassword(password)
                if(dbClients.isClientPasswordCorrect(name,password)){
                    if (this.dbMessages.isExistMessages(name) ){
                        val messageList = this.dbMessages.getAllClientMessages(name)
                        messageList.forEach{message -> serverClient.sendMessage(TypeMessage.RESPONSE, message.text,message.srcClientName,message.timestamp)}
                        this.dbMessages.deleteItems(name)
                    }
                    println("User ${serverClient.getClientName()} successfully connected")
                }else{
                    serverClient.sendMessage(TypeMessage.BYE)
                }

            }
            "Register"->{
                val name = msgList[1]
                val password = msgList[2]
                if (dbClients.isExistClient(name)){
                    serverClient.sendMessage(TypeMessage.FAILURE)
                }else{
                    serverClient.sendMessage(TypeMessage.SUCCESS)
                    dbClients.insertClient(DataClientServerModel(name,password))
                }
            }
            "Login"->{
                val name = msgList[1]
                val password = msgList[2]
                if (dbClients.isClientPasswordCorrect(name,password)){
                    serverClient.sendMessage(TypeMessage.SUCCESS)
                }else{
                    serverClient.sendMessage(TypeMessage.FAILURE)
                }
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