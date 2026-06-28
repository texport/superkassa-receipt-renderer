package kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket
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

import kz.mybrain.superkassa.core.data.receipt.renderer.base.formatted
import kz.mybrain.superkassa.core.data.receipt.renderer.base.translationKey
import kz.mybrain.superkassa.core.domain.model.receipt.TaxLine

object TaxesSectionComponent {
    fun render(
        ticketTaxes: List<TaxLine>,
        t: (String) -> String
    ): String {
        if (ticketTaxes.isEmpty()) return ""
        val taxesHtml = ticketTaxes.joinToString("") { line ->
            val label = t(line.vatGroup.translationKey)
            kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.render(
                title = label,
                rows = listOf(
                    kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                        label = t("sum_tax"),
                        value = line.taxSum.formatted(),
                        valueClass = "tax-sum-cell num bold"
                    ),
                    kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                        label = t("taxable_turnover"),
                        value = line.taxBase.formatted()
                    )
                )
            )
        }
        return """
        <div class="rule"></div>
        <div class="tax-section">
            <div class="section-title center">${t("taxes")}</div>
            <div class="taxes-list">
                $taxesHtml
            </div>
        </div>
        """.trimIndent()
    }
}
