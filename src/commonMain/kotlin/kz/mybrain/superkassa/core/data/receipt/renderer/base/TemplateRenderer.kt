package kz.mybrain.superkassa.core.data.receipt.renderer.base

object TemplateRenderer {
    fun render(templateName: String, placeholders: Map<String, String>): String {
        val template = ResourceLoader.readText("/templates/$templateName")
            ?: throw IllegalArgumentException("Template /templates/$templateName not found")
        var result = template
        placeholders.forEach { (k, v) ->
            result = result.replace("/*{{$k}}*/", v)
            result = result.replace("{{$k}}", v)
        }
        return result
    }
}
