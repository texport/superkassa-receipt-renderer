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

object TemplateRenderer {
    fun render(templateName: String, placeholders: Map<String, String>): String {
        val template = ResourceLoader.readText("/templates/$templateName")
            ?: throw IllegalArgumentException("Template /templates/$templateName not found")
        var result = template
        placeholders.forEach { (k, v) ->
            result = result.replace("{{$k}}", v)
        }
        return result
    }
}
