package com.ainotes.app.data.repository

import android.util.Base64
import com.ainotes.app.security.SecureStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Thin AES/GCM helpers on top of SecureStore values.
 * A random key is generated once and stored inside EncryptedSharedPreferences,
 * so no key material lives in source code or logs.
 */
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val TAG_BITS = 128
private const val IV_BYTES = 12

fun SecureStore.encrypt(plain: String): String {
    val key = keyBytes()
    val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
    val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
}

fun SecureStore.decrypt(payload: String): String {
    val key = keyBytes()
    val data = Base64.decode(payload, Base64.NO_WRAP)
    val iv = data.copyOfRange(0, IV_BYTES)
    val body = data.copyOfRange(IV_BYTES, data.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
    return String(cipher.doFinal(body), Charsets.UTF_8)
}

private fun SecureStore.keyBytes(): SecretKey {
    val stored = get("ainotes_aes_key")
    val bytes = if (stored.isNullOrBlank()) {
        ByteArray(32).also { SecureRandom().nextBytes(it) }
            .also { put("ainotes_aes_key", Base64.encodeToString(it, Base64.NO_WRAP)) }
    } else {
        Base64.decode(stored, Base64.NO_WRAP)
    }
    return SecretKeySpec(bytes, "AES")
}
