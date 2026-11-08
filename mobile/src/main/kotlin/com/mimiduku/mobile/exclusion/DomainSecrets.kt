package com.mimiduku.mobile.exclusion

import com.mimiduku.mobile.storage.StringStore
import java.util.Base64

/** 復号した、確認を省くサイト。[id] は照合用ハッシュと同じ値（個別に外すときの目印）。 */
data class ExcludedDomain(
    val id: String,
    val domain: String,
)

/**
 * 確認を省くサイト名の暗号化した控え。照合（[ExclusionStore.hasDomain]）にはハッシュを使い、これは PIN を入力したときに
 * 一覧を表示するためだけに持つ。PIN がなければ復号できないため、サービス側の判定では使わない。
 */
class DomainSecrets(
    private val store: StringStore,
    private val vault: DomainVault = DomainVault(),
) {
    /** 一覧に出せる（暗号化した控えがある）サイトの目印。 */
    fun ids(): Set<String> = read().keys

    fun put(
        id: String,
        domain: String,
        pin: String,
    ) {
        val key = vault.key(pin, salt())
        write(read() + (id to vault.encrypt(domain, key)))
    }

    /** [pin] で復号できたものだけを返す。 */
    fun all(pin: String): List<ExcludedDomain> {
        val key = vault.key(pin, salt())
        return read().mapNotNull { (id, token) -> vault.decrypt(token, key)?.let { ExcludedDomain(id, it) } }
    }

    fun remove(id: String) = write(read() - id)

    fun clear() {
        store.remove(KEY_SECRETS)
        store.remove(KEY_SALT)
    }

    /** PIN の変更に合わせて、新しい PIN で暗号化し直す。旧 PIN で復号できなかったものは捨てる。 */
    fun reencrypt(
        oldPin: String,
        newPin: String,
    ) {
        val entries = all(oldPin)
        store.putString(KEY_SALT, Base64.getEncoder().encodeToString(vault.newSalt()))
        val key = vault.key(newPin, salt())
        write(entries.associate { it.id to vault.encrypt(it.domain, key) })
    }

    private fun salt(): ByteArray =
        store.getString(KEY_SALT)?.let { Base64.getDecoder().decode(it) }
            ?: vault.newSalt().also { store.putString(KEY_SALT, Base64.getEncoder().encodeToString(it)) }

    private fun read(): Map<String, String> =
        store
            .getString(KEY_SECRETS)
            ?.lineSequence()
            ?.mapNotNull { line -> line.split(SEPARATOR).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
            ?.toMap()
            .orEmpty()

    private fun write(entries: Map<String, String>) {
        if (entries.isEmpty()) {
            store.remove(KEY_SECRETS)
        } else {
            store.putString(KEY_SECRETS, entries.entries.joinToString("\n") { "${it.key}$SEPARATOR${it.value}" })
        }
    }

    private companion object {
        const val KEY_SECRETS = "exclusion_domain_secrets"
        const val KEY_SALT = "exclusion_domain_kdf_salt"
        const val SEPARATOR = "\t"
    }
}
