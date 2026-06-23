package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.UnitOfMeasurement
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kz.mybrain.superkassa.core.domain.model.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.ReceiptLanguage
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Рендерит чек в HTML для печати и доставки с учетом настроек локализации и брендирования.
 */
class ReceiptHtmlRenderer(
    private val qrCodeGenerator: QrCodeGeneratorPort,
    private val taxCalculationService: TaxCalculationService = TaxCalculationService()
) : ReceiptRenderPort {

    companion object {
        private const val QR_CODE_SIZE_PX = 180

        private fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage, separator: String = " / "): String {
            return when (lang) {
                ReceiptLanguage.RU -> labelRu
                ReceiptLanguage.KK -> labelKk
                ReceiptLanguage.MIXED -> "$labelRu$separator$labelKk"
            }
        }
    }

    override fun renderHtml(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot, config: ReceiptBranding): String {
        val lang = config.language
        val dateStr = ReceiptFormatter.formatDate(doc.createdAt)
        val sign = doc.fiscalSign ?: doc.autonomousSign ?: "-"
        val totalStr = ReceiptFormatter.formatMoney(receipt.total)
        val opTitle = operationTitle(receipt.operation, lang)
        val docNoStr = doc.docNo?.toString() ?: doc.id
        val shiftNoStr = doc.shiftNo?.toString() ?: "-"

        val receiptUrl = doc.receiptUrl?.trim()?.takeIf { it.isNotEmpty() } ?: buildFallbackReceiptUrl(receipt, doc)

        val qrDataUri = receiptUrl?.let { qrCodeGenerator.generatePngDataUri(it, QR_CODE_SIZE_PX) }

        val ofdStatusHtml = when (doc.ofdStatus) {
            "DELIVERED", "SENT" -> "<span class=\"badge badge-success\">${translate("Отправлен", "Жіберілді", lang)}</span>"
            "PENDING", "OFFLINE" -> "<span class=\"badge badge-warning\">${translate("Офлайн", "Автономды", lang)}</span>"
            else -> ReceiptFormatter.escape(doc.ofdStatus ?: "-")
        }

        val currency = doc.currency ?: "KZT"

        val itemsSumCents = receipt.items.sumOf { ReceiptFormatter.moneyToCents(it.sum) }
        val itemsSumStr = ReceiptFormatter.formatCents(itemsSumCents)

        val itemsHtml = receipt.items.joinToString("") { item ->
            val priceStr = ReceiptFormatter.formatMoney(item.price)
            val sumStr = ReceiptFormatter.formatMoney(item.sum)
            val unit = try {
                item.measureUnitCode?.let { UnitOfMeasurement.fromCode(it) } ?: UnitOfMeasurement.DEFAULT
            } catch (e: Exception) {
                UnitOfMeasurement.DEFAULT
            }
            val unitStr = if (unit == UnitOfMeasurement.PIECE) {
                translate("шт", "дана", lang)
            } else {
                translate(unit.shortRus ?: "", unit.shortKaz ?: "", lang)
            }
            val exciseStamps = item.listExciseStamp
            val exciseHtml = if (!exciseStamps.isNullOrEmpty()) {
                val stamps = exciseStamps.joinToString(", ") { ReceiptFormatter.escape(it) }
                "<div class=\"excise-stamps\">${translate("Маркировка", "Таңбалау", lang)}: $stamps</div>"
            } else {
                ""
            }
            val discountVal = item.discount
            val discountHtml = if (discountVal != null) {
                "<div class=\"item-discount\">${translate("Скидка", "Жеңілдік", lang)}: -${ReceiptFormatter.formatMoney(discountVal)}</div>"
            } else {
                ""
            }
            val markupVal = item.markup
            val markupHtml = if (markupVal != null) {
                "<div class=\"item-markup\">${translate("Наценка", "Үстеме", lang)}: +${ReceiptFormatter.formatMoney(markupVal)}</div>"
            } else {
                ""
            }
            """
            <tr>
                <td class="name">
                    ${ReceiptFormatter.escape(item.name)}
                    $exciseHtml
                    $discountHtml
                    $markupHtml
                </td>
                <td class="num">${item.quantity} $unitStr</td>
                <td class="num">$priceStr</td>
                <td class="num">$sumStr</td>
            </tr>
            """.trimIndent()
        }

        val paymentsHtml = receipt.payments.joinToString("") { p ->
            val typeStr = when (p.type) {
                PaymentType.CASH -> translate("Наличные", "Қолма-қол", lang)
                PaymentType.CARD -> translate("Карта", "Карта", lang)
                PaymentType.ELECTRONIC -> translate("Электронно", "Электронды", lang)
            }
            """
            <tr>
                <td>$typeStr</td>
                <td class="num">${ReceiptFormatter.formatMoney(p.sum)}</td>
            </tr>
            """.trimIndent()
        }

        val summaryRowsSb = StringBuilder()
        summaryRowsSb.append(summaryRow(translate("Промежуточный итог", "Аралық жиынтық", lang), itemsSumStr))
        receipt.discount?.let {
            summaryRowsSb.append(summaryRow(translate("Скидка", "Жеңілдік", lang), "-${ReceiptFormatter.formatMoney(it)}"))
        }
        receipt.markup?.let {
            summaryRowsSb.append(summaryRow(translate("Наценка", "Үстеме", lang), ReceiptFormatter.formatMoney(it)))
        }
        receipt.taken?.let {
            summaryRowsSb.append(summaryRow(translate("Получено", "Алынды", lang), ReceiptFormatter.formatMoney(it)))
        }
        receipt.change?.let {
            summaryRowsSb.append(summaryRow(translate("Сдача", "Қайтарым", lang), ReceiptFormatter.formatMoney(it)))
        }
        summaryRowsSb.append(summaryRow(translate("ИТОГО", "ЖИЫНЫ", lang), totalStr, "grand"))
        val summaryHtml = summaryRowsSb.toString()

        val taxResult = taxCalculationService.calculateTicketTaxes(
            items = receipt.items,
            taxRegime = receipt.taxRegime,
            defaultVatGroup = receipt.defaultVatGroup ?: VatGroup.NO_VAT
        )

        val taxSectionHtml = if (taxResult.ticketTaxes.isNotEmpty()) {
            val taxesRows = taxResult.ticketTaxes.joinToString("") { line ->
                val label = when (line.vatGroup) {
                    VatGroup.NO_VAT -> translate("Без НДС", "ҚҚС-сыз", lang)
                    VatGroup.VAT_0 -> translate("НДС 0%", "ҚҚС 0%", lang)
                    VatGroup.VAT_5 -> translate("НДС 5%", "ҚҚС 5%", lang)
                    VatGroup.VAT_10 -> translate("НДС 10%", "ҚҚС 10%", lang)
                    VatGroup.VAT_16 -> translate("НДС 16%", "ҚҚС 16%", lang)
                }
                """
                <tr>
                    <td>$label</td>
                    <td class="num">${ReceiptFormatter.formatMoney(line.taxBase)}</td>
                    <td class="num">${ReceiptFormatter.formatMoney(line.taxSum)}</td>
                </tr>
                """.trimIndent()
            }
            """
            <div class="rule"></div>
            <div class="tax-section">
                <div class="section-title center">${translate("Налоги", "Салықтар", lang)}</div>
                <table class="tax-table">
                    <thead>
                        <tr>
                            <th>${translate("Ставка", "Ставка", lang)}</th>
                            <th class="num">${translate("Облагаемый оборот", "Облыс", lang)}</th>
                            <th class="num">${translate("НДС", "ҚҚС", lang)}</th>
                        </tr>
                    </thead>
                    <tbody>
                        $taxesRows
                    </tbody>
                </table>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        val escapedSign = ReceiptFormatter.escape(sign)
        val taxpayerName = doc.taxpayerName
        val orgTitleHtml = if (!taxpayerName.isNullOrBlank()) {
            "<div class=\"brand-logo\">${ReceiptFormatter.escape(taxpayerName)}</div>"
        } else {
            "<div class=\"brand-logo\">SUPERKASSA</div>"
        }
        val taxpayerBin = doc.taxpayerBin
        val orgBinHtml = if (!taxpayerBin.isNullOrBlank()) {
            "<div class=\"org-bin\">${translate("БИН/ИИН", "БСН/ЖСН", lang)}: ${ReceiptFormatter.escape(taxpayerBin)}</div>"
        } else {
            ""
        }
        val taxpayerAddress = doc.taxpayerAddress
        val orgAddressHtml = if (!taxpayerAddress.isNullOrBlank()) {
            "<div class=\"org-address\">${ReceiptFormatter.escape(taxpayerAddress)}</div>"
        } else {
            ""
        }
        val customerBin = receipt.customerBin
        val customerBinHtml = if (!customerBin.isNullOrBlank()) {
            "<div class=\"org-bin\">${translate("Покупатель БИН/ИИН", "Сатып алушы БСН/ЖСН", lang)}: ${ReceiptFormatter.escape(customerBin)}</div>"
        } else {
            ""
        }
        val factoryNumber = doc.factoryNumber
        val factoryNumberHtml = if (!factoryNumber.isNullOrBlank()) {
            "<tr><td>${translate("ЗНМ", "ЗНМ", lang)}</td><td>${ReceiptFormatter.escape(factoryNumber)}</td></tr>"
        } else {
            ""
        }

        val ofdProviderName = when (doc.ofdProvider?.uppercase()) {
            "KAZAKHTELECOM" -> translate("АО «Казахтелеком»", "«Қазақтелеком» АҚ", lang)
            "TRANSTELECOM" -> translate("АО «Транстелеком»", "«Транстелеком» АҚ", lang)
            "ALTECO" -> translate("ТОО «Alteco Partners»", "«Alteco Partners» ЖШС", lang)
            else -> doc.ofdProvider ?: "-"
        }
        val ofdProviderHtml = if (!doc.ofdProvider.isNullOrBlank()) {
            "<div class=\"footer-item\">${translate("ОФД", "ОФД", lang)}: ${ReceiptFormatter.escape(ofdProviderName)}</div>"
        } else {
            ""
        }
        val ofdWebSite = when (doc.ofdProvider?.uppercase()) {
            "KAZAKHTELECOM" -> "oofd.kz"
            "TRANSTELECOM" -> "o.oofd.kz"
            "ALTECO" -> "alteco.kz"
            else -> "consumer.oofd.kz"
        }
        val ofdSiteHtml = "<div class=\"footer-item\">${translate("Сайт проверки", "Тексеру сайты", lang)}: $ofdWebSite</div>"

        val receiptLinkHtml = receiptUrl?.let { url ->
            val escapedUrl = ReceiptFormatter.escape(url)
            """<div class="receipt-link">${translate("Ссылка на чек", "Чек сілтемесі", lang)}: """ +
                """<a href="$escapedUrl" target="_blank" rel="noopener noreferrer">$escapedUrl</a></div>"""
        } ?: ""

        val qrHtml = qrDataUri?.let { uri ->
            """<div class="qr"><img src="${ReceiptFormatter.escape(uri)}" alt="QR-код чека" /></div>"""
        } ?: ""

        val autonomousModeHtml = if (doc.isAutonomous) {
            "<div class=\"muted center\">${translate("Автономный режим", "Автономды режим", lang)}</div>"
        } else {
            ""
        }

        val headerLogoHtml = config.headerLogoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
            """<div class="brand-logo-img"><img src="${ReceiptFormatter.escape(url)}" alt="Logo" /></div>"""
        } ?: ""
        val headerHtmlContent = config.headerHtml?.trim() ?: ""
        val footerHtmlContent = config.footerHtml?.trim() ?: ""

        return """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8" />
            <title>Чек #$docNoStr</title>
            <style>
                ${ReceiptHtmlStyles.CSS}
                .brand-logo-img { text-align: center; margin-bottom: 8px; }
                .brand-logo-img img { max-width: 100%; max-height: 80px; object-fit: contain; }
                ${config.customCss ?: ""}
            </style>
        </head>
        <body>
            <div class="receipt">
                <div class="center brand-header">
                    $headerLogoHtml
                    $headerHtmlContent
                    $orgTitleHtml
                    $orgBinHtml
                    $orgAddressHtml
                    $customerBinHtml
                    <div class="rule"></div>
                    <div class="doc-title">$opTitle</div>
                </div>
                <table class="meta-table">
                    <tr><td>${translate("Документ", "Құжат", lang)}</td><td>№ $docNoStr</td></tr>
                    <tr><td>${translate("Смена", "Ауысым", lang)}</td><td>$shiftNoStr</td></tr>
                    <tr><td>${translate("РНМ", "ТНМ", lang)}</td><td>${ReceiptFormatter.escape(doc.registrationNumber ?: "-")}</td></tr>
                    $factoryNumberHtml
                    <tr><td>${translate("ККМ ID", "БАҚ ID", lang)}</td><td>${ReceiptFormatter.escape(doc.cashboxId ?: "-")}</td></tr>
                    <tr><td>${translate("Дата и время", "Күні мен уақыты", lang)}</td><td>$dateStr</td></tr>
                    <tr><td>${translate("Валюта", "Валюта", lang)}</td><td>$currency</td></tr>
                    <tr><td>${translate("Статус ОФД", "ОФД статусы", lang)}</td><td>$ofdStatusHtml</td></tr>
                </table>
                
                <div class="rule"></div>
                <table class="items-table">
                    <thead>
                        <tr>
                            <th>${translate("Наименование", "Атауы", lang)}</th>
                            <th class="num">${translate("Кол-во", "Саны", lang)}</th>
                            <th class="num">${translate("Цена", "Бағасы", lang)}</th>
                            <th class="num">${translate("Сумма", "Сомасы", lang)}</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>
        
                <div class="rule"></div>
                <table class="payments-table">
                    <tbody>
                        $paymentsHtml
                    </tbody>
                </table>
        
                <div class="rule"></div>
                <table class="summary-table">
                    <tbody>
                        $summaryHtml
                    </tbody>
                </table>
                
                $taxSectionHtml
        
                <div class="rule"></div>
                <div class="footer">
                    <div class="footer-item">${translate("Фискальный признак", "Фискалдық белгі", lang)}: $escapedSign</div>
                    $ofdProviderHtml
                    $ofdSiteHtml
                    $receiptLinkHtml
                    $qrHtml
                    $autonomousModeHtml
                    $footerHtmlContent
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun summaryRow(label: String, value: String, cssClass: String? = null): String {
        val classAttr = if (!cssClass.isNullOrBlank()) " class=\"$cssClass\"" else ""
        return "<tr$classAttr><td>${ReceiptFormatter.escape(
            label
        )}</td><td class=\"num\">${ReceiptFormatter.escape(value)}</td></tr>"
    }

    private fun operationTitle(op: ReceiptOperationType, lang: ReceiptLanguage): String = when (op) {
        ReceiptOperationType.SELL -> translate("ЧЕК ПРОДАЖИ", "САТУ ЧЕГІ", lang)
        ReceiptOperationType.SELL_RETURN -> translate("ВОЗВРАТ ПРОДАЖИ", "САТУДЫ ҚАЙТАРУ", lang)
        ReceiptOperationType.BUY -> translate("ЧЕК ПОКУПКИ", "САТЫП АЛУ ЧЕГІ", lang)
        ReceiptOperationType.BUY_RETURN -> translate("ВОЗВРАТ ПОКУПКИ", "САТЫП АЛУДЫ ҚАЙТАРУ", lang)
    }

    private fun buildFallbackReceiptUrl(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot): String? {
        val regNum = doc.registrationNumber?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val sign = doc.fiscalSign?.trim()?.takeIf { it.isNotEmpty() }
            ?: doc.autonomousSign?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val totalStr = ReceiptFormatter.formatMoney(receipt.total)
        val dtStr = Instant.ofEpochMilli(doc.createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        return "https://consumer.oofd.kz?i=$regNum&f=$sign&s=$totalStr&t=$dtStr"
    }

    override fun renderXReportHtml(shift: ShiftInfo, counters: Map<String, Long>, config: ReceiptBranding): String {
        return ReportPrintRenderer.renderXReportHtml(shift, counters, config)
    }

    override fun renderOpenShiftHtml(shift: ShiftInfo, config: ReceiptBranding): String {
        return ReportPrintRenderer.renderOpenShiftHtml(shift, config)
    }

    override fun renderCloseShiftHtml(shift: ShiftInfo, counters: Map<String, Long>, config: ReceiptBranding): String {
        return ReportPrintRenderer.renderCloseShiftHtml(shift, counters, config)
    }

    override fun renderCashOperationHtml(doc: FiscalDocumentSnapshot, config: ReceiptBranding): String {
        return ReportPrintRenderer.renderCashOperationHtml(doc, config)
    }
}
