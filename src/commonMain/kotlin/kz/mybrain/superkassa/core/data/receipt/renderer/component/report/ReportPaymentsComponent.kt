package kz.mybrain.superkassa.core.data.receipt.renderer.component.report

import kz.mybrain.superkassa.core.domain.model.zxreport.TicketOperationAggregate
import kz.mybrain.superkassa.core.data.receipt.renderer.base.toOperationKey
import kz.mybrain.superkassa.core.data.receipt.renderer.base.toPaymentKey

object ReportPaymentsComponent {
    fun render(
        ticketOperations: List<TicketOperationAggregate>,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val paymentSectionsHtml = ticketOperations.joinToString("") { op ->
            val opLabel = translateInlineKey(op.operation.toOperationKey())
            val payRows = op.payments.joinToString("") { pay ->
                val payLabel = t(pay.payment.toPaymentKey())
                val formattedSum = formatAmount(pay.sumBills)
                """
                <tr>
                    <td class="tax-details-cell" style="padding-left: 8px;">&bull; $payLabel (${pay.count})</td>
                    <td class="tax-sum-cell" style="font-size: 0.95em; color: var(--m3-on-surface-variant); font-weight: normal;">$formattedSum</td>
                </tr>
                """.trimIndent()
            }
            val detailRows = payRows.ifEmpty {
                """
                <tr>
                    <td colspan="2" class="tax-details-cell" style="padding-left: 8px;">${t("no_payments")}</td>
                </tr>
                """.trimIndent()
            }

            val totalSumStr = formatAmount(op.ticketsSumBills)
            """
            <fieldset class="tax-row-card dashed">
                <legend class="card-label">$opLabel</legend>
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-details-cell">${t("sum")}</td>
                        <td class="tax-sum-cell bold">$totalSumStr</td>
                    </tr>
                    <tr>
                        <td class="tax-details-cell">${t("checks")}</td>
                        <td class="tax-sum-cell" style="font-weight: 600; color: var(--m3-on-surface);">${op.ticketsCount}</td>
                    </tr>
                    $detailRows
                </table>
            </fieldset>
            """.trimIndent()
        }

        return """
            <div class="section-title">${translateInlineKey("payment_types")}</div>
            <div class="taxes-list">
                $paymentSectionsHtml
            </div>
        """.trimIndent()
    }
}
