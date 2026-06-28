package kz.mybrain.superkassa.core.data.receipt.renderer.component.common
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

object CardComponent {
    data class Row(
        val label: String,
        val value: String,
        val valueClass: String = "tax-sum-cell num",
        val valueStyle: String? = null
    )

    fun render(
        title: String,
        rows: List<Row>,
        isHighlighted: Boolean = false,
        isDashed: Boolean = false
    ): String {
        val cardClass = buildString {
            append("tax-row-card")
            if (isHighlighted) append(" highlighted")
            if (isDashed) append(" dashed")
        }
        val rowsHtml = rows.joinToString("") { row ->
            val styleAttr = if (row.valueStyle != null) " style=\"${row.valueStyle}\"" else ""
            "<tr><td class=\"tax-details-cell\">${row.label}</td><td class=\"${row.valueClass}\"$styleAttr>${row.value}</td></tr>"
        }
        return """
        <fieldset class="$cardClass" style="margin-top: 14px; margin-bottom: 8px;">
            <legend class="card-label">$title</legend>
            <table class="tax-row-table">
                $rowsHtml
            </table>
        </fieldset>
        """.trimIndent()
    }
}
