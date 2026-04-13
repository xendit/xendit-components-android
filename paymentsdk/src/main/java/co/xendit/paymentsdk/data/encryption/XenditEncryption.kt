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

object XenditEncryption {

  private const val ECDH_ALGORITHM = "EC"
  private const val ECDH_CURVE = "secp384r1" // P-384
  private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
  private const val AES_ALGORITHM = "AES/GCM/NoPadding"
  private const val HMAC_SHA256 = "HmacSHA256"
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 48 // Web uses Uint32Array(12) = 12 * 4 bytes = 48 bytes

  /** Generates an ephemeral EC key pair using P-384 curve. */
  fun generateKeyPair(): KeyPair {
    val kpg = KeyPairGenerator.getInstance(ECDH_ALGORITHM)
    val ecSpec = ECGenParameterSpec(ECDH_CURVE)
    kpg.initialize(ecSpec)
    return kpg.generateKeyPair()
  }

  /** Derives a shared secret using ECDH. */
  private fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
    val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM)
    keyAgreement.init(privateKey)
    keyAgreement.doPhase(publicKey, true)
    return keyAgreement.generateSecret()
  }

  /** Implements HKDF (RFC 5869) using HMAC-SHA256 to derive a session key. */
  private fun hkdf(
    inputKeyingMaterial: ByteArray,
    salt: ByteArray?,
    info: ByteArray,
    length: Int
  ): ByteArray {
    val mac = Mac.getInstance(HMAC_SHA256)
    val effectiveSalt =
      if (salt == null || salt.isEmpty()) {
        ByteArray(mac.macLength) // Defaults to zero-filled byte array of hash length
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

  /** Encrypts data using AES-GCM. */
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
   * Performs the full encryption flow:
   * 1. Generates ephemeral key pair.
   * 2. Derives shared secret with server's public key.
   * 3. Derives session key using HKDF.
   * 4. Encrypts data with AES-GCM.
   * 5. Formats the output as expected by Xendit backend.
   */
  fun encrypt(data: String, serverPublicKeyBase64: String, sessionId: String): String {
    try {
      // 1. Generate local ephemeral key pair
      val ownKeyPair = generateKeyPair()
      val ownPublicKey = ownKeyPair.public as ECPublicKey

      // 2. Decode server public key
      val serverPublicKeyBytes = Base64.decode(serverPublicKeyBase64, Base64.DEFAULT)
      val keyFactory = KeyFactory.getInstance(ECDH_ALGORITHM)
      val x509KeySpec = X509EncodedKeySpec(serverPublicKeyBytes)
      val serverPublicKey = keyFactory.generatePublic(x509KeySpec)

      // 3. Derive shared secret
      val sharedSecret = deriveSharedSecret(ownKeyPair.private, serverPublicKey)

      // 4. Derive session key using HKDF
      // Info should be session ID as UTF-8 bytes
      // Salt is empty byte array
      // Length 32 bytes (256 bits) for AES-256
      val info = sessionId.toByteArray(StandardCharsets.UTF_8)
      val salt = ByteArray(0)
      val sessionKey = hkdf(sharedSecret, salt, info, 32)

      // 5. Encrypt data
      val iv = ByteArray(GCM_IV_LENGTH)
      SecureRandom().nextBytes(iv)

      // AAD is SHA-256 hash of session ID
      val messageDigest = MessageDigest.getInstance("SHA-256")
      val aad = messageDigest.digest(sessionId.toByteArray(StandardCharsets.UTF_8))

      val ciphertext = encryptAesGcm(data.toByteArray(StandardCharsets.UTF_8), sessionKey, iv, aad)

      // 6. Format output
      // xendit-encrypted-1-[Base64 ClientPubKey]-[Base64 IV]-[Base64 CipherText]
      // IMPORTANT: Use Base64.DEFAULT (standard Base64 with +/) not URL_SAFE (which uses -_)
      // Base64.DEFAULT adds newlines every 76 chars, so we need to remove them
      // Base64.NO_WRAP uses URL-safe encoding which breaks the format
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
