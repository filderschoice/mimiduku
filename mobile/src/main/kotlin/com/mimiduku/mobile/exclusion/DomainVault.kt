package com.mimiduku.mobile.exclusion

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 確認を省くサイト名を、保護者 PIN から導いた鍵で暗号化・復号する（AES-256-GCM、鍵は PBKDF2-HMAC-SHA256）。
 *
 * PIN は 4〜8 桁なので、保存データを端末から取り出せる相手には総当たりで復号されうる。強度は PIN の桁数に依存する
 * （利用者の判断で、この強度を受け入れている）。改ざんや別の PIN での復号は null になる。
 */
class DomainVault(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    fun key(
        pin: String,
        salt: ByteArray,
    ): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** 保存用の文字列（IV と暗号文を連結した Base64）にする。 */
    fun encrypt(
        plain: String,
        key: SecretKey,
    ): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher =
            Cipher.getInstance(
                TRANSFORMATION,
            ).apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    /** 復号できない（鍵の違い・壊れた保存値）ときは null。 */
    fun decrypt(
        token: String,
        key: SecretKey,
    ): String? =
        try {
            val bytes = Base64.getDecoder().decode(token)
            val cipher =
                Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
                }
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IndexOutOfBoundsException) {
            null
        }

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}
