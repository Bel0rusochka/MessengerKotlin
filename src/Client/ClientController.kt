package Client
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import TypeMessage
import java.security.MessageDigest
import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.crypto.Cipher
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

const val CLIENT_CLIENT_KEY = "3766A913327C16289E9A142778C35"
const val CLIENT_SERVER_KEY = "8992A6FA5C64DCD7FDBAA78F12A6B"
const val CLIENT_DB_KEY = "8B72C16DAAED9D12F11E163396B68"


class ClientController(private val name: String, private val password: String) {
    private var socket: Socket? = null
    private var dataIn: DataInputStream? = null
    private var dataOut: DataOutputStream? = null
    private var dbMessages:ClientMessageModel? = null
    private var connectFlag = false

    fun getName():String{
        return this.name
    }

    private fun stringToKey(strKey: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(strKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, 0, 32, "AES")  // Используем первые 32 байта для AES-256
    }

    private fun encrypt(text: String, secretKey: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, stringToKey(secretKey))
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    private fun decrypt(text: String, secretKey: String):String{
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, stringToKey(secretKey))
        val decryptedBytes = Base64.getDecoder().decode(text)
        return String(cipher.doFinal(decryptedBytes), Charsets.UTF_8)
    }

    fun registerUser():Boolean{
        try {
            initConnection()
            sendMessage(TypeMessage.REGISTER)
            val statusMsg = processMessageFromServer()
            closeConnection()
            return statusMsg == "Success"
        } catch (e: IOException) {
            return false
        }
    }

    fun loginUser():Boolean{
        try {
            initConnection()
            sendMessage(TypeMessage.LOGIN)
            val statusMsg = processMessageFromServer()
            closeConnection()
            return statusMsg == "Success"
        } catch (e: IOException) {
           return false
        }
    }

    fun connectDb(){
        dbMessages = ClientMessageModel("/data/${name}Message.db")
    }

    fun startConnection(){
        return try {
            initConnection()
            sendMessage(TypeMessage.START)

        } catch (e: IOException) {
            println("Connection Failed: ${e.message}")
        }
    }

    fun getConnectStatus():Boolean{
        return this.connectFlag
    }

    private fun initConnection() {
        this.socket = Socket()
        this.socket!!.connect(InetSocketAddress("127.0.0.1",5001), 1000) // 54.160.173.83
        this.dataOut = DataOutputStream(socket!!.getOutputStream())
        this.dataIn = DataInputStream(socket!!.getInputStream())
        connectFlag = socket!!.isConnected
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
        dbMessages!!.close()
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
                    this.dataOut?.writeUTF(encrypt("Bye|$timestamp", CLIENT_SERVER_KEY))
                }
                TypeMessage.START -> {
                    this.dataOut?.writeUTF(encrypt("Start|${name}|${password}|$timestamp",CLIENT_SERVER_KEY))
                }
                TypeMessage.SEND -> {
                    val encMsgClient = encrypt(msg!!, CLIENT_CLIENT_KEY)
                    this.dataOut?.writeUTF(encrypt("Send|$encMsgClient|$to|$timestamp",CLIENT_SERVER_KEY))
                    val encMsgDB = encrypt(msg!!, CLIENT_DB_KEY)
                    dbMessages!!.insertMessage(DataMessageClientModel(timestamp, encMsgDB, to!!, "Send"))
                }
                TypeMessage.REGISTER -> {
                    this.dataOut?.writeUTF(encrypt("Register|${name}|${password}|$timestamp", CLIENT_SERVER_KEY))
                }
                TypeMessage.LOGIN -> {
                    this.dataOut?.writeUTF(encrypt("Login|${name}|${password}|$timestamp", CLIENT_SERVER_KEY))
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
        val msgList = decrypt(this.dataIn!!.readUTF(), CLIENT_SERVER_KEY).split("|")
        when(msgList[0]){
            "Bye"->{
                return "Bye"
            }
            "Response"->{
                val srcClientName = msgList[2]
                val timestamp = transformStringToTimestamp(msgList[3])
                if(srcClientName=="Server"){
                    return "Unfindable"
                }else{
                    val decText = decrypt(msgList[1], CLIENT_CLIENT_KEY)
                    val encText = encrypt(decText, CLIENT_DB_KEY)
                    dbMessages!!.insertMessage(DataMessageClientModel(timestamp,encText,srcClientName,"Response"))
                    return "Response"
                }
            }
            else -> {
                return msgList[0]
            }
        }

    }

    fun getAllMessageWith(clientName: String): List<String>{
        return dbMessages!!.getAllClientMessages(clientName,getName(), ::decrypt, CLIENT_DB_KEY)
    }

    fun deleteAllMessagesWithConvClient(clientName: String){
        dbMessages!!.deleteAllMessagesWithClient(clientName)
    }

    fun getAllConverClientNames():List<String>{
        return dbMessages!!.getAllClientConverNames()
    }

}