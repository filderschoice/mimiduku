package com.mimiduku.mobile.rules

import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * システムのアクセシビリティ設定には、本アプリのサービスの説明文が表示される。その文言が照合語に当たると、本アプリ自身の
 * 偽の警告バナーとログが出てしまう（BL-060）ため、説明文が照合語を含まないことを固定する。
 */
class OwnTextScarewareTest {
    private val matcher = ScarewareMatcher(DetectionRules.DEFAULT.scarewarePhrases)

    @Test
    fun `the accessibility service description does not trigger the scareware warning`() {
        val strings = File("src/main/res/values/strings.xml").readText()
        val description =
            Regex("""<string name="accessibility_service_description">([^<]*)</string>""")
                .find(strings)
                ?.groupValues
                ?.get(1)
                ?: error("accessibility_service_description が見つからない")

        assertNull(matcher.findMatch(listOf(description)))
    }
}
