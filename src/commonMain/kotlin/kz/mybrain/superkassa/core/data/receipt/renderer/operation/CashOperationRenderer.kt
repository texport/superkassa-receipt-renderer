package kz.mybrain.superkassa.core.data.receipt.renderer.operation
import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.receipt.*

import kz.mybrain.superkassa.core.data.receipt.renderer.base.BaseDocumentRenderer

class CashOperationRenderer : BaseDocumentRenderer() {

    fun render(doc: FiscalDocumentSnapshot, kkm: KkmInfo): String {
        val lang = kkm.branding.language

        val key = when (doc.docType) {
            "CASH_IN" -> "cash_in"
            "CASH_OUT" -> "cash_out"
            else -> "cash_operation"
        }
        val sumStr = doc.totalAmount?.let { formatAmount(it) } ?: "0.00"
        val currencyCode = doc.currency ?: "KZT"
        val currency = translate(currencyCode, lang)

        val sumLabel = translateInline("sum", lang)
        val bodyContent = """
            <table class="meta-table">
                <tr class="bold"><td>$sumLabel</td><td>$sumStr $currency</td></tr>
            </table>
        """.trimIndent()

        return renderStandardDocument(
            titleKey = key,
            kkm = kkm,
            createdAt = doc.createdAt,
            shiftNo = doc.shiftNo,
            docNo = doc.docNo?.toString() ?: doc.id,
            ofdStatus = doc.ofdStatus,
            isFiscal = false,
            bodyContent = bodyContent
        )
    }
}
