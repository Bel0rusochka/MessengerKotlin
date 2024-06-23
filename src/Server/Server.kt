package Server

import java.sql.Timestamp
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import TypeMessage
import java.util.Date
import java.text.SimpleDateFormat


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
    fun getClientName(): String{
        return this.userName
    }

    fun getPassword(): String{
        return this.password
    }


    fun sendMessage(typeMessage: TypeMessage, msg: String?=null,srcClient: String?=null, timestamp: Timestamp=Timestamp(Date().time)){
        when (typeMessage) {
            TypeMessage.BYE -> {
                this.dataOut.writeUTF("Bye|$timestamp")
            }
            TypeMessage.RESPONSE -> {
                this.dataOut.writeUTF("Response|$msg|$srcClient|${timestamp}")
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
    private val dbClients = ServerClientModel("ServerClients.db")
    private val dbMessages = ServerMessageModel("ServerMessage.db")

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
        this.dbMessages.close()
    }
    private fun findActiveClient(name: String): Client?{
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

    private fun processMessage(client: Client){
        val msgList = client.dataIn.readUTF().split("|")
        when(msgList[0]){
            "Send"->{
                val text = msgList[1]
                val dstClientName = msgList[2]
                val timestamp = transformStringToTimestamp(msgList[3])
                val srcClientName = client.getClientName()

                if(!this.dbClients.itemExists(dstClientName)){
                    client.sendMessage(TypeMessage.RESPONSE,"Can't find destination user","Server",timestamp)
                }else{
                    val dstClient = findActiveClient(dstClientName)
                    if(dstClient==null){
                        this.dbMessages.insertItem(DataMessageServerModel(timestamp,text,dstClientName, srcClientName))
                    }else{
                        dstClient.sendMessage(TypeMessage.RESPONSE, text, client.getClientName(),timestamp)
                    }

                }
            }
            "Bye"->{
                println("Bye, bye ${client.getClientName()}")
                throw ExitException("Client ${client.getClientName()} has disconnected.")
            }
            "Start"->{
                val name = msgList[1]
                val password = msgList[2]
                client.setName(name)
                client.setPassword(password)
                if (!this.dbClients.itemExists(name)){
                    this.dbClients.insertItem(DataClient(name,password))
                }else{
                    if (this.dbMessages.itemsExists(name)){
                        val messageList = this.dbMessages.getAllClientItems(name)
                        messageList.forEach{message -> client.sendMessage(TypeMessage.RESPONSE, message.text,message.srcClientName,message.timestamp)}
                        this.dbMessages.deleteItems(name)

                    }
                }
                println("User ${client.getClientName()} successfully registered")
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