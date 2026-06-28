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

object PaymentsListComponent {
    fun render(
        payments: List<ReceiptPayment>,
        taken: Money?,
        change: Money?,
        t: (String) -> String
    ): String {
        return payments.joinToString("") { p ->
            val typeStr = t(p.type.translationKey)
            val paymentRow = """
            <tr>
                <td class="bold">$typeStr</td>
                <td class="num bold">${p.sum.formatted()}</td>
            </tr>
            """.trimIndent()

            if (p.type == PaymentType.CASH) {
                val details = StringBuilder(paymentRow)
                taken?.let { takenVal ->
                    details.append("\n" + renderDetailRow(t("received"), takenVal.formatted()))
                }
                change?.let { changeVal ->
                    details.append("\n" + renderDetailRow(t("change"), changeVal.formatted()))
                }
                details.toString()
            } else {
                paymentRow
            }
        }
    }

    private fun renderDetailRow(label: String, amount: String): String {
        return """
        <tr>
            <td style="padding-left: 15px; font-size: 0.9em; opacity: 0.8;">$label</td>
            <td class="num" style="font-size: 0.9em; opacity: 0.8;">$amount</td>
        </tr>
        """.trimIndent()
    }
}
