package kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket
import kz.mybrain.superkassa.core.domain.model.receipt.*

import kz.mybrain.superkassa.core.data.receipt.renderer.base.escaped
import kz.mybrain.superkassa.core.data.receipt.renderer.base.formatted

object ParentTicketComponent {
    fun render(
        parent: ParentTicket,
        parentTitle: String,
        formattedDateTime: String,
        t: (String) -> String
    ): String {
        val parentTotalStr = parent.parentTicketTotal.formatted()
        val escapedKgdKkmId = parent.kgdKkmId.escaped()
        val receiptNoLabel = t("fiscal_sign")
        val dateTimeLabel = t("date_time")
        val kkmIdLabel = t("rnm")
        val receiptSumLabel = t("receipt_sum")
        val cardHtml = kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.render(
            title = parentTitle,
            rows = listOf(
                kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                    label = receiptNoLabel,
                    value = parent.parentTicketNumber.toString(),
                    valueClass = "tax-sum-cell num bold",
                    valueStyle = "color: var(--m3-on-surface);"
                ),
                kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                    label = dateTimeLabel,
                    value = formattedDateTime,
                    valueClass = "tax-sum-cell num",
                    valueStyle = "color: var(--m3-on-surface);"
                ),
                kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                    label = kkmIdLabel,
                    value = escapedKgdKkmId,
                    valueClass = "tax-sum-cell num",
                    valueStyle = "color: var(--m3-on-surface);"
                ),
                kz.mybrain.superkassa.core.data.receipt.renderer.component.common.CardComponent.Row(
                    label = receiptSumLabel,
                    value = parentTotalStr,
                    valueClass = "tax-sum-cell num bold"
                )
            )
        )
        return """
        $cardHtml
        <div class="rule"></div>
        """.trimIndent()
    }
}
