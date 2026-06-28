package kz.mybrain.superkassa.core.data.receipt.renderer.component.report
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

import kz.mybrain.superkassa.core.data.receipt.renderer.base.toOperationKey

object ReportNonNullableComponent {
    fun render(
        nonNullableSums: List<Pair<String, Long>>,
        startShiftNonNullableSums: List<Pair<String, Long>>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val nonNullableCards = mutableListOf<String>()
        for (i in nonNullableSums.indices) {
            val endPair = nonNullableSums[i]
            val op = endPair.first
            val startSum = startShiftNonNullableSums.firstOrNull { it.first == op }?.second ?: 0L
            val endSum = endPair.second

            val label = translateInlineKey(op.toOperationKey())

            nonNullableCards += kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.render(
                title = label,
                rows = listOf(
                    kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                        label = t("start_shift"),
                        value = formatAmount(startSum),
                        valueClass = "tax-sum-cell",
                        valueStyle = "font-size: 0.95em; color: var(--m3-on-surface-variant); font-weight: normal;"
                    ),
                    kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                        label = t("end_shift"),
                        value = formatAmount(endSum),
                        valueClass = "tax-sum-cell",
                        valueStyle = "font-size: 0.95em; color: var(--m3-on-surface); font-weight: bold;"
                    )
                )
            )
        }

        return """
            <div class="section-title">${translateInlineKey("non_nullable_totals")}</div>
            <div class="taxes-list">
                ${nonNullableCards.joinToString("")}
            </div>
        """.trimIndent()
    }
}
