package com.mimiduku.mobile.exclusion

import com.mimiduku.mobile.storage.StringStore
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 「戻る／このまま進む」の確認を省く対象。サイト（ドメイン）とアプリ（パッケージ名）を分けて持つ。
 *
 * - サイトはドメインだけを、端末ごとのランダムな値（ソルト）を混ぜた SHA-256 にして保存する。元のドメインは
 *   保存しないため、一覧に出せない（件数と全削除だけ）。
 * - アプリは遷移元のパッケージ名をそのまま保存する（検知ログにも残るもので、一覧から個別に外せる）。
 * 登録できる件数はそれぞれ [MAX_ENTRIES] まで。登録時に PIN を求める判断は呼び出し側
 * （[com.mimiduku.mobile.settings.SettingsController]）が行う。
 */
class ExclusionStore(
    private val store: StringStore,
    private val saltGenerator: () -> String = ::randomSalt,
) {
    private val packageList = EntryList(store, KEY_PACKAGES)
    private val domainList = EntryList(store, KEY_DOMAINS)

    /** 登録済みのアプリ（パッケージ名）。 */
    fun packages(): List<String> = packageList.read()

    fun hasPackage(packageName: String): Boolean = packageName in packageList.read()

    /** 登録する。空・空白入り・上限超えは false。登録済みなら true。 */
    fun addPackage(packageName: String): Boolean {
        val name = packageName.trim()
        return name.isNotEmpty() && name.none(Char::isWhitespace) && packageList.add(name)
    }

    fun removePackage(packageName: String) = packageList.remove(packageName)

    fun domainCount(): Int = domainList.read().size

    /** [domain] は [DomainNormalizer.fromUrlText] で整えた値。 */
    fun hasDomain(domain: String): Boolean = hash(domain) in domainList.read()

    /** 登録する。空・上限超えは false。登録済みなら true。 */
    fun addDomain(domain: String): Boolean = domain.isNotBlank() && domainList.add(hash(domain))

    fun clearDomains() = domainList.clear()

    private fun hash(domain: String): String {
        val salt = store.getString(KEY_SALT) ?: saltGenerator().also { store.putString(KEY_SALT, it) }
        return MessageDigest
            .getInstance("SHA-256")
            .digest("$salt:$domain".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /** 1 つのキーに改行区切りで入れた、上限つきの文字列の並び。 */
    private class EntryList(
        private val store: StringStore,
        private val key: String,
    ) {
        fun read(): List<String> = store.getString(key)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

        fun add(value: String): Boolean {
            val current = read()
            val room = current.size < MAX_ENTRIES
            if (value !in current && room) store.putString(key, (current + value).joinToString("\n"))
            return value in current || room
        }

        fun remove(value: String) {
            val rest = read() - value
            if (rest.isEmpty()) store.remove(key) else store.putString(key, rest.joinToString("\n"))
        }

        fun clear() = store.remove(key)
    }

    companion object {
        const val MAX_ENTRIES = 50

        private const val KEY_PACKAGES = "exclusion_packages"
        private const val KEY_DOMAINS = "exclusion_domain_hashes"
        private const val KEY_SALT = "exclusion_salt"

        private fun randomSalt(): String =
            ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    }
}
