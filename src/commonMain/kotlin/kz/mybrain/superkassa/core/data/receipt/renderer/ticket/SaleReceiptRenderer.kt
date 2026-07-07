package kz.mybrain.superkassa.core.data.receipt.renderer.ticket

import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter
import kz.mybrain.superkassa.core.data.receipt.renderer.base.formatted
import kz.mybrain.superkassa.core.data.receipt.renderer.base.escaped
import kz.mybrain.superkassa.core.data.receipt.renderer.style.TicketStyles
import kz.mybrain.superkassa.core.data.receipt.renderer.base.BaseDocumentRenderer
import kz.mybrain.superkassa.core.data.receipt.renderer.base.DocumentConstants
import kz.mybrain.superkassa.core.data.receipt.renderer.base.translationKey
import kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket.ParentTicketComponent
import kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket.PaymentsListComponent
import kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket.SaleItemsComponent
import kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket.TaxesSectionComponent
import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.ofd.*
import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.domain.model.receipt.*
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.data.receipt.renderer.base.MetadataBuilder
import kz.mybrain.superkassa.core.data.receipt.renderer.base.StandardDocumentInput

/**
 * Рендерер фискальных чеков продаж/возвратов.
 *
 * @property qrCodeGenerator генератор QR-кодов для чеков
 * @property ofdProviders список доступных провайдеров ОФД
 */
class SaleReceiptRenderer(
    private val qrCodeGenerator: QrCodeGeneratorPort
) : BaseDocumentRenderer() {

    /**
     * Формирует HTML-представление фискального чека.
     *
     * @param receipt запрос на чек
     * @param doc фискальный документ
     * @param kkm информация о ККМ
     * @return HTML-строка отрендеренного чека
     */
    fun render(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot, kkm: KkmInfo): String {
        val lang = kkm.branding.language
        fun t(key: String): String = translate(key, lang)
        fun translateInlineKey(key: String): String = translateInline(key, lang)

        val sign = doc.fiscalSign ?: doc.autonomousSign ?: "-"
        val totalStr = receipt.total.formatted()
        val opTitleKey = operationTitleKey(receipt.operation)

        val receiptUrl = doc.receiptUrl?.trim()?.takeIf { it.isNotEmpty() }
        val qrDataUri = receiptUrl?.let { qrCodeGenerator.generatePngDataUri(it, DocumentConstants.QR_CODE_SIZE_PX) }

        val itemsSumCents = receipt.items.sumOf { ReceiptFormatter.moneyToCents(it.sum) }
        val itemsSumStr = ReceiptFormatter.formatCents(itemsSumCents)

        val itemsHtml = SaleItemsComponent.render(
            items = receipt.items,
            defaultVatGroup = receipt.defaultVatGroup ?: VatGroup.NO_VAT,
            taxRegime = receipt.taxRegime,
            receiptDiscount = receipt.discount,
            t = { t(it) },
            translateInlineKey = { translateInlineKey(it) }
        )

        val paymentsHtml = PaymentsListComponent.render(
            payments = receipt.payments,
            taken = receipt.taken,
            change = receipt.change,
            t = { t(it) }
        )

        val summaryRowsSb = StringBuilder()
        summaryRowsSb.append(summaryRow(t("subtotal"), itemsSumStr))
        receipt.discount?.let {
            summaryRowsSb.append(summaryRow(t("discount_total"), "-${it.formatted()}"))
        }
        receipt.markup?.let {
            summaryRowsSb.append(summaryRow(t("markup_total"), it.formatted()))
        }
        summaryRowsSb.append(summaryRow(t("grand_total"), totalStr, "grand"))
        val summaryHtml = summaryRowsSb.toString()

        val taxSectionHtml = TaxesSectionComponent.render(
            ticketTaxes = receipt.ticketTaxes ?: emptyList(),
            t = { t(it) }
        )

        val ofdProviderName = (doc.ofdProvider ?: "-").escaped()

        val additionalMeta = MetadataBuilder { translateInlineKey(it) }.apply {
            add("buyer_bin_iin", receipt.customerBin)
        }.build()

        val adapter = kz.mybrain.superkassa.core.data.receipt.renderer.base.HtmlBrandingAdapter(kkm.branding)
        val beforeItemsHtml = adapter.beforeItemsHtml
        val afterItemsHtml = adapter.afterItemsHtml
        val beforeTotalsHtml = adapter.beforeTotalsHtml
        val afterTotalsHtml = adapter.afterTotalsHtml

        val parentTicketHtml = receipt.parentTicket?.let { parent ->
            val parentDateStr = formatDate(parent.parentTicketDateTimeMillis)
            val parentTitle = if (receipt.operation == ReceiptOperationType.SELL_RETURN ||
                receipt.operation == ReceiptOperationType.BUY_RETURN
            ) {
                t("parent_ticket_return")
            } else {
                t("parent_ticket_storno")
            }
            ParentTicketComponent.render(
                parent = parent,
                parentTitle = parentTitle,
                formattedDateTime = parentDateStr,
                t = { t(it) }
            )
        } ?: ""

        val bodyContent = """
            $parentTicketHtml
            $beforeItemsHtml
            <div class="items-list">
                $itemsHtml
            </div>
            $afterItemsHtml
            <div class="rule"></div>
            $beforeTotalsHtml
            <table class="summary-table">
                <tbody>
                    $summaryHtml
                </tbody>
            </table>
            <div class="rule"></div>
            <table class="payments-table">
                <tbody>
                    $paymentsHtml
                </tbody>
            </table>
            $taxSectionHtml
            $afterTotalsHtml
        """.trimIndent()

        return renderStandardDocument(
            StandardDocumentInput(
                titleKey = opTitleKey,
                kkm = kkm,
                createdAt = doc.createdAt,
                shiftNo = doc.shiftNo,
                docNo = doc.docNo?.toString() ?: doc.id,
                ofdStatus = doc.ofdStatus,
                isFiscal = true,
                isAutonomous = doc.isAutonomous,
                fiscalSign = sign,
                ofdProvider = ofdProviderName,
                receiptUrl = receiptUrl,
                qrDataUri = qrDataUri,
                additionalMeta = additionalMeta,
                docCss = TicketStyles.TICKET_CSS,
                bodyContent = bodyContent
            )
        )
    }

    private fun summaryRow(label: String, value: String, cssClass: String = ""): String {
        val classAttr = if (cssClass.isNotEmpty()) " class=\"$cssClass\"" else ""
        return """
            <tr$classAttr>
                <td>$label</td>
                <td class="num">$value</td>
            </tr>
        """.trimIndent()
    }

    private fun operationTitleKey(type: ReceiptOperationType): String = type.translationKey
}
