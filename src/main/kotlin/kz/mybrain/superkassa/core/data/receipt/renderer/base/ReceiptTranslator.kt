package kz.mybrain.superkassa.core.data.receipt.renderer.base

import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.core.domain.model.ReceiptLanguage

object ReceiptTranslator {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private val translations: Map<String, Map<String, String>> by lazy {
        try {
            val stream = ReceiptTranslator::class.java.getResourceAsStream("/translations.json")
            if (stream != null) {
                val jsonText = stream.bufferedReader().use { it.readText() }
                Json.decodeFromString<Map<String, Map<String, String>>>(jsonText)
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getTranslationMap(key: String): Map<String, String>? {
        return translations[key]
    }

    fun translate(key: String, lang: ReceiptLanguage): String {
        val trans = translations[key]
        val ru = trans?.get("ru") ?: key
        val kk = trans?.get("kk") ?: key
        return translate(ru, kk, lang)
    }

    fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage): String {
        if (labelRu == labelKk) return labelRu
        return when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> {
                """<span class="lang-fraction"><span class="lang-fraction-top">$labelKk</span><span class="lang-fraction-bottom">$labelRu</span></span>"""
            }
        }
    }

    fun translateInline(key: String, lang: ReceiptLanguage, separator: String = " / "): String {
        val trans = translations[key]
        val ru = trans?.get("ru") ?: key
        val kk = trans?.get("kk") ?: key
        return translateInline(ru, kk, lang, separator)
    }

    fun translateInline(labelRu: String, labelKk: String, lang: ReceiptLanguage, separator: String = " / "): String {
        if (labelRu == labelKk) return labelRu
        return when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> "$labelKk$separator$labelRu"
        }
    }
}
