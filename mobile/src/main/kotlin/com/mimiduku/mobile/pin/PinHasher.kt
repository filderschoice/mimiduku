package com.mimiduku.mobile.pin

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN を塩つきの PBKDF2（HMAC-SHA256）でハッシュ化する。平文の PIN は保存しない。
 *
 * 保存形式は `v1:反復回数:塩(Base64):ハッシュ(Base64)`。反復回数を保存値に含めるため、将来回数を上げても
 * 既存の PIN をそのまま照合できる。
 */
class PinHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return listOf(VERSION, iterations.toString(), encode(salt), encode(derive(pin, salt, iterations)))
            .joinToString(SEPARATOR)
    }

    /** 保存値と照合する。保存値の形式が壊れている場合は null（照合できない）。 */
    fun matches(
        pin: String,
        stored: String,
    ): Boolean? {
        val parsed = parse(stored) ?: return null
        val actual = derive(pin, parsed.salt, parsed.iterations)
        return MessageDigest.isEqual(actual, parsed.hash)
    }

    private class Parsed(
        val iterations: Int,
        val salt: ByteArray,
        val hash: ByteArray,
    )

    private fun parse(stored: String): Parsed? {
        val parts = stored.split(SEPARATOR)
        val iterations = parts.getOrNull(1)?.toIntOrNull()
        val valid = parts.size == PARTS && parts[0] == VERSION && iterations != null && iterations > 0
        if (!valid) return null
        return runCatching { Parsed(iterations!!, decode(parts[2]), decode(parts[3])) }
            .onFailure { if (it !is IllegalArgumentException) throw it }
            .getOrNull()
            ?.takeIf { it.salt.isNotEmpty() && it.hash.size == KEY_BITS / Byte.SIZE_BITS }
    }

    private fun derive(
        pin: String,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String) = Base64.getDecoder().decode(text)

    companion object {
        /** 端末での照合が数百ミリ秒に収まる程度。保存値はアプリ内ストレージにあり、他アプリからは読めない。 */
        const val DEFAULT_ITERATIONS = 100_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val VERSION = "v1"
        private const val SEPARATOR = ":"
        private const val PARTS = 4
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}
