package co.xendit.paymentsdk.data.encryption

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object XenditEncryption {

  private const val ECDH_ALGORITHM = "EC"
  private const val ECDH_CURVE = "secp384r1"
  private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
  private const val AES_ALGORITHM = "AES/GCM/NoPadding"
  private const val HMAC_SHA256 = "HmacSHA256"
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 48

  fun generateKeyPair(): KeyPair {
    val kpg = KeyPairGenerator.getInstance(ECDH_ALGORITHM)
    val ecSpec = ECGenParameterSpec(ECDH_CURVE)
    kpg.initialize(ecSpec)
    return kpg.generateKeyPair()
  }

  private fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
    val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM)
    keyAgreement.init(privateKey)
    keyAgreement.doPhase(publicKey, true)
    return keyAgreement.generateSecret()
  }
  private fun hkdf(
    inputKeyingMaterial: ByteArray,
    salt: ByteArray?,
    info: ByteArray,
    length: Int
  ): ByteArray {
    val mac = Mac.getInstance(HMAC_SHA256)
    val effectiveSalt =
      if (salt == null || salt.isEmpty()) {
        ByteArray(mac.macLength)
      } else {
        salt
      }
    mac.init(SecretKeySpec(effectiveSalt, HMAC_SHA256))
    val pseudoRandomKey = mac.doFinal(inputKeyingMaterial)

    val result = ByteArray(length)
    mac.init(SecretKeySpec(pseudoRandomKey, HMAC_SHA256))
    var t = ByteArray(0)
    var offset = 0
    var i = 1

    while (offset < length) {
      mac.update(t)
      mac.update(info)
      mac.update(i.toByte())
      t = mac.doFinal()

      val copyLength = Math.min(t.size, length - offset)
      System.arraycopy(t, 0, result, offset, copyLength)
      offset += copyLength
      i++
    }
    return result
  }

  private fun encryptAesGcm(
    plaintext: ByteArray,
    key: ByteArray,
    iv: ByteArray,
    aad: ByteArray
  ): ByteArray {
    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
    val cipher = Cipher.getInstance(AES_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
    cipher.updateAAD(aad)
    return cipher.doFinal(plaintext)
  }

  /**
   * Performs the full encryption flow
   */
  fun encrypt(data: String, serverPublicKeyBase64: String, sessionId: String): String {
    try {
      val ownKeyPair = generateKeyPair()
      val ownPublicKey = ownKeyPair.public as ECPublicKey

      val serverPublicKeyBytes = Base64.decode(serverPublicKeyBase64, Base64.DEFAULT)
      val keyFactory = KeyFactory.getInstance(ECDH_ALGORITHM)
      val x509KeySpec = X509EncodedKeySpec(serverPublicKeyBytes)
      val serverPublicKey = keyFactory.generatePublic(x509KeySpec)

      val sharedSecret = deriveSharedSecret(ownKeyPair.private, serverPublicKey)

      val info = sessionId.toByteArray(StandardCharsets.UTF_8)
      val salt = ByteArray(0)
      val sessionKey = hkdf(sharedSecret, salt, info, 32)

      val iv = ByteArray(GCM_IV_LENGTH)
      SecureRandom().nextBytes(iv)

      val messageDigest = MessageDigest.getInstance("SHA-256")
      val aad = messageDigest.digest(sessionId.toByteArray(StandardCharsets.UTF_8))

      val ciphertext = encryptAesGcm(data.toByteArray(StandardCharsets.UTF_8), sessionKey, iv, aad)

      val ownPublicKeyBase64 = Base64.encodeToString(ownPublicKey.encoded, Base64.NO_WRAP)
      val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
      val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

      return "xendit-encrypted-1-$ownPublicKeyBase64-$ivBase64-$ciphertextBase64"
    } catch (e: Exception) {
      // Fallback or better error handling
      e.printStackTrace()
      throw RuntimeException("Encryption failed: ${e.message}", e)
    }
  }
}
