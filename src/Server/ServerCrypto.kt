package Server

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class ServerCrypto {
    val CLIENT_SERVER_KEY = "8992A6FA5C64DCD7FDBAA78F12A6B"
    val CLIENT_DB_KEY = "8B72C16DAAED9D12F11E163396B68"

    private fun stringToKey(strKey: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(strKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, 0, 32, "AES")  // Используем первые 32 байта для AES-256
    }

    fun encrypt(text: String, secretKey: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, stringToKey(secretKey))
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decrypt(text: String, secretKey: String):String{
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, stringToKey(secretKey))
        val decryptedBytes = Base64.getDecoder().decode(text)
        return String(cipher.doFinal(decryptedBytes), Charsets.UTF_8)
    }
}