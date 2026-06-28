package kz.mybrain.superkassa.core.data.receipt.renderer.base
import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.ofd.*
import kz.mybrain.superkassa.core.domain.model.shift.*
import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.domain.model.auth.*
import kz.mybrain.superkassa.core.domain.model.delivery.*
import kz.mybrain.superkassa.core.domain.model.queue.*
import kz.mybrain.superkassa.core.domain.model.report.*
import kz.mybrain.superkassa.core.domain.model.receipt.*
import kz.mybrain.superkassa.core.domain.model.zxreport.*

class MetadataBuilder(private val translateInlineKey: (String) -> String) {
    private val items = mutableListOf<Pair<String, String>>()

    fun add(key: String, value: String?) {
        val cleanValue = value?.trim() ?: ""
        if (cleanValue.isNotEmpty()) {
            items.add(translateInlineKey(key) to cleanValue.escaped())
        }
    }

    fun addRaw(key: String, value: String) {
        items.add(translateInlineKey(key) to value)
    }

    fun build(): List<Pair<String, String>> = items
}
