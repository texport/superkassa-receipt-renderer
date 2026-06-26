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
import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService

import kz.mybrain.superkassa.core.data.receipt.renderer.base.MetadataBuilder

class SaleReceiptRenderer(
    private val qrCodeGenerator: QrCodeGeneratorPort,
    private val taxCalculationService: TaxCalculationService = TaxCalculationService(),
    private val ofdProviders: Map<String, OfdProviderConfig> = emptyMap()
) : BaseDocumentRenderer() {

    fun render(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot, kkm: KkmInfo): String {
        val lang = kkm.branding.language
        fun t(key: String): String = translate(key, lang)
        fun translateInlineKey(key: String): String = translateInline(key, lang)

        val sign = doc.fiscalSign ?: doc.autonomousSign ?: "-"
        val totalStr = receipt.total.formatted()
        val opTitleKey = operationTitleKey(receipt.operation)

        val receiptUrl = doc.receiptUrl?.trim()?.takeIf { it.isNotEmpty() } ?: buildFallbackReceiptUrl(receipt, doc)
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

        val taxResult = taxCalculationService.calculateTicketTaxes(
            items = receipt.items,
            taxRegime = receipt.taxRegime,
            defaultVatGroup = receipt.defaultVatGroup ?: VatGroup.NO_VAT
        )

        val taxSectionHtml = TaxesSectionComponent.render(
            ticketTaxes = taxResult.ticketTaxes,
            t = { t(it) }
        )

        val providerConfig = doc.ofdProvider?.uppercase()?.let { ofdProviders[it] }
        val ofdProviderName = providerConfig?.let {
            translate(it.nameRu, it.nameKk, lang)
        } ?: (doc.ofdProvider ?: "-").escaped()

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

    private fun buildFallbackReceiptUrl(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot): String? {
        val iinBin = doc.taxpayerBin ?: return null
        val regNum = doc.registrationNumber ?: return null
        val docNo = doc.docNo ?: return null
        val dateUnix = doc.createdAt / 1000L
        val fiscalSign = doc.fiscalSign ?: doc.autonomousSign ?: return null
        val sumCents = ReceiptFormatter.moneyToCents(receipt.total)
        val sumStr = String.format(java.util.Locale.US, "%.2f", sumCents.toDouble() / 100.0)
        val providerConfig = doc.ofdProvider?.uppercase()?.let { ofdProviders[it] }
        val domain = providerConfig?.checkDomain ?: return null
        return "https://$domain/r/$docNo?r=$regNum&i=$iinBin&d=$dateUnix&s=$sumStr&f=$fiscalSign"
    }
}
