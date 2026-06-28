package kz.mybrain.superkassa.core.data.receipt.renderer.component.report

import kz.mybrain.superkassa.core.domain.model.zxreport.*

import kz.mybrain.superkassa.core.domain.model.zxreport.TaxAggregate
import kz.mybrain.superkassa.core.data.receipt.renderer.base.toOperationKey
import kz.mybrain.superkassa.core.data.receipt.renderer.base.toTaxKey

object ReportTaxesComponent {
    fun render(
        taxes: List<TaxAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val taxCards = taxes.flatMap { taxAgg ->
            val label = translateInlineKey(taxAgg.taxTypeCode.toTaxKey())
            taxAgg.operations.map { op ->
                val opLabel = translateInlineKey(op.operation.toOperationKey())
                kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.render(
                    title = "$label ($opLabel)",
                    rows = listOf(
                        kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                            label = t("sum_tax"),
                            value = formatAmount(op.taxSumBills),
                            valueClass = "tax-sum-cell bold"
                        ),
                        kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                            label = t("turnover"),
                            value = formatAmount(op.turnoverBills)
                        )
                    )
                )
            }
        }.joinToString("")

        return """
            <div class="section-title">${translateInlineKey("taxes_by_operations")}</div>
            <div class="taxes-list">
                $taxCards
            </div>
        """.trimIndent()
    }
}
