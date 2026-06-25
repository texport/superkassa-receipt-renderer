package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter
import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService


class SaleReceiptRenderer(
    private val qrCodeGenerator: QrCodeGeneratorPort,
    private val taxCalculationService: TaxCalculationService = TaxCalculationService(),
    private val ofdProviders: Map<String, OfdProviderConfig> = emptyMap()
) : BaseDocumentRenderer() {

    companion object {
        private const val QR_CODE_SIZE_PX = 180
    }

    fun render(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot, kkm: KkmInfo): String {
        val lang = kkm.branding.language
        val isNarrow = kkm.branding.paperWidthMm <= 58
        fun t(ru: String, kk: String, sep: String = " / "): String = translate(ru, kk, lang, sep, isNarrow)

        val dateStr = formatDate(doc.createdAt)
        val sign = doc.fiscalSign ?: doc.autonomousSign ?: "-"
        val totalStr = ReceiptFormatter.formatMoney(receipt.total)
        val opTitle = operationTitle(receipt.operation, lang, isNarrow)
        val docNoStr = doc.docNo?.toString() ?: doc.id
        val shiftNoStr = doc.shiftNo?.toString() ?: "-"

        val receiptUrl = doc.receiptUrl?.trim()?.takeIf { it.isNotEmpty() } ?: buildFallbackReceiptUrl(receipt, doc)
        val qrDataUri = receiptUrl?.let { qrCodeGenerator.generatePngDataUri(it, QR_CODE_SIZE_PX) }

        val ofdStatusHtml = when (doc.ofdStatus) {
            "DELIVERED", "SENT" -> renderStatusBadge("success", "Отправлен", "Жіберілді", lang)
            "PENDING", "OFFLINE" -> renderStatusBadge("warning", "Офлайн", "Автономды", lang)
            "FAILED", "ERROR" -> renderStatusBadge("error", "Ошибка", "Қате", lang)
            else -> renderStatusBadge("warning", doc.ofdStatus ?: "-", doc.ofdStatus ?: "-", lang)
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
                t("шт", "дана")
            } else {
                t(unit.shortRus ?: "", unit.shortKaz ?: "")
            }
            val exciseStamps = item.listExciseStamp
            val exciseHtml = if (!exciseStamps.isNullOrEmpty()) {
                val stamps = exciseStamps.joinToString(", ") { ReceiptFormatter.escape(it) }
                "<div class=\"excise-stamps\">${t("Маркировка:", "Таңбалау:")} $stamps</div>"
            } else {
                ""
            }
            val discountVal = item.discount
            val discountHtml = if (discountVal != null && receipt.discount == null) {
                "<div class=\"item-discount\">${t("Скидка:", "Жеңілдік:")} -${ReceiptFormatter.formatMoney(discountVal)}</div>"
            } else {
                ""
            }
            val markupVal = item.markup
            val markupHtml = if (markupVal != null) {
                "<div class=\"item-markup\">${t("Наценка:", "Үстеме:")} +${ReceiptFormatter.formatMoney(markupVal)}</div>"
            } else {
                ""
            }
            val itemVat = item.vatGroup ?: receipt.defaultVatGroup ?: VatGroup.NO_VAT
            val vatLabel = when (itemVat) {
                VatGroup.NO_VAT -> t("Без НДС", "ҚҚС-сыз")
                VatGroup.VAT_0 -> t("НДС 0%", "ҚҚС 0%")
                VatGroup.VAT_5 -> t("НДС 5%", "ҚҚС 5%")
                VatGroup.VAT_10 -> t("НДС 10%", "ҚҚС 10%")
                VatGroup.VAT_16 -> t("НДС 16%", "ҚҚС 16%")
            }
            val vatHtml = if (receipt.taxRegime == TaxRegime.MIXED) {
                "<div class=\"item-vat\">$vatLabel</div>"
            } else {
                ""
            }
            val itemClass = if (item.isStorno) "storno-item" else ""
            val stornoBadgeHtml = if (item.isStorno) {
                """ <span class="storno-badge">${t("Сторно", "Сторно")}</span>"""
            } else {
                ""
            }
            """
            <div class="item-row-card $itemClass">
                <table class="item-row-table">
                    <tr>
                        <td class="item-name-cell">${ReceiptFormatter.escape(item.name)}$stornoBadgeHtml</td>
                        <td class="item-sum-cell">${if (item.isStorno) "-" else ""}$sumStr</td>
                    </tr>
                    <tr>
                        <td class="item-details-cell" colspan="2">
                            ${item.quantity} $unitStr × $priceStr
                            $exciseHtml
                            $vatHtml
                            $discountHtml
                            $markupHtml
                        </td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }

        val paymentsHtml = receipt.payments.joinToString("") { p ->
            val typeStr = when (p.type) {
                PaymentType.CASH -> t("Наличные", "Қолма-қол")
                PaymentType.CARD -> t("Карта", "Карта")
                PaymentType.ELECTRONIC -> t("Электронно", "Электронды")
                PaymentType.MOBILE -> t("Мобильный платеж", "Мобильді төлем")
            }
            val paymentRow = """
            <tr>
                <td class="bold">$typeStr</td>
                <td class="num bold">${ReceiptFormatter.formatMoney(p.sum)}</td>
            </tr>
            """.trimIndent()

            if (p.type == PaymentType.CASH) {
                val details = StringBuilder(paymentRow)
                receipt.taken?.let {
                    details.append("\n<tr><td style=\"padding-left: 15px; font-size: 0.9em; opacity: 0.8;\">${t("Получено", "Алынды")}</td><td class=\"num\" style=\"font-size: 0.9em; opacity: 0.8;\">${ReceiptFormatter.formatMoney(it)}</td></tr>")
                }
                receipt.change?.let {
                    details.append("\n<tr><td style=\"padding-left: 15px; font-size: 0.9em; opacity: 0.8;\">${t("Сдача", "Қайтарым")}</td><td class=\"num\" style=\"font-size: 0.9em; opacity: 0.8;\">${ReceiptFormatter.formatMoney(it)}</td></tr>")
                }
                details.toString()
            } else {
                paymentRow
            }
        }

        val summaryRowsSb = StringBuilder()
        summaryRowsSb.append(summaryRow(t("Промежуточный итог", "Аралық жиынтық"), itemsSumStr))
        receipt.discount?.let {
            summaryRowsSb.append(summaryRow(t("Скидка", "Жеңілдік"), "-${ReceiptFormatter.formatMoney(it)}"))
        }
        receipt.markup?.let {
            summaryRowsSb.append(summaryRow(t("Наценка", "Үстеме"), ReceiptFormatter.formatMoney(it)))
        }
        summaryRowsSb.append(summaryRow(t("ИТОГО", "ЖИЫНЫ"), totalStr, "grand"))
        val summaryHtml = summaryRowsSb.toString()

        val taxResult = taxCalculationService.calculateTicketTaxes(
            items = receipt.items,
            taxRegime = receipt.taxRegime,
            defaultVatGroup = receipt.defaultVatGroup ?: VatGroup.NO_VAT
        )

        val taxSectionHtml = if (taxResult.ticketTaxes.isNotEmpty()) {
            val taxesHtml = taxResult.ticketTaxes.joinToString("") { line ->
                val label = when (line.vatGroup) {
                    VatGroup.NO_VAT -> t("Без НДС", "ҚҚС-сыз")
                    VatGroup.VAT_0 -> t("НДС 0%", "ҚҚС 0%")
                    VatGroup.VAT_5 -> t("НДС 5%", "ҚҚС 5%")
                    VatGroup.VAT_10 -> t("НДС 10%", "ҚҚС 10%")
                    VatGroup.VAT_16 -> t("НДС 16%", "ҚҚС 16%")
                }
                """
                <div class="tax-row-card">
                    <table class="tax-row-table">
                        <tr>
                            <td class="tax-rate-cell bold">$label</td>
                            <td class="tax-sum-cell num bold">${ReceiptFormatter.formatMoney(line.taxSum)}</td>
                        </tr>
                        <tr>
                            <td class="tax-details-cell" colspan="2">
                                ${t("Облагаемый оборот", "Салық салынатын айналым")}: ${ReceiptFormatter.formatMoney(line.taxBase)}
                            </td>
                        </tr>
                    </table>
                </div>
                """.trimIndent()
            }
            """
            <div class="rule"></div>
            <div class="tax-section">
                <div class="section-title center">${t("Налоги", "Салықтар")}</div>
                <div class="taxes-list">
                    $taxesHtml
                </div>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        val escapedSign = ReceiptFormatter.escape(sign)
        val customerBin = receipt.customerBin
        val customerBinHtml = if (!customerBin.isNullOrBlank()) {
            "<div class=\"org-bin\">${t("Покупатель БИН/ИИН:", "Сатып алушы БСН/ЖСН:")} ${ReceiptFormatter.escape(customerBin)}</div>"
        } else {
            ""
        }
        val factoryNumber = doc.factoryNumber
        val factoryNumberHtml = if (!factoryNumber.isNullOrBlank()) {
            "<tr><td>${t("ЗНМ", "ЗНМ")}</td><td>${ReceiptFormatter.escape(factoryNumber)}</td></tr>"
        } else {
            ""
        }

        val providerConfig = doc.ofdProvider?.uppercase()?.let { ofdProviders[it] }
        val ofdProviderName = providerConfig?.let {
            translate(it.nameRu, it.nameKk, lang, isNarrow = isNarrow)
        } ?: ReceiptFormatter.escape(doc.ofdProvider ?: "-")

        val qrCodeHtml = if (!qrDataUri.isNullOrBlank()) {
            """
            <div class="qr">
                <img src="$qrDataUri" alt="QR Link" />
            </div>
            """.trimIndent()
        } else {
            ""
        }

        val linkHtml = if (!receiptUrl.isNullOrBlank()) {
            """
            <div class="receipt-link center">
                ${t("Ссылка на чек:", "Чек сілтемесі:")} 
                <a href="${ReceiptFormatter.escape(receiptUrl)}" target="_blank">${ReceiptFormatter.escape(receiptUrl)}</a>
            </div>
            """.trimIndent()
        } else {
            ""
        }

        val headerHtml = renderHeaderHtml(opTitle, kkm, lang)
        val footerHtml = renderFooterHtml(kkm)

        val beforeItemsHtml = kkm.branding.beforeItemsHtml?.trim()?.let { "<div class=\"custom-before-items-container\">$it</div>" } ?: ""
        val afterItemsHtml = kkm.branding.afterItemsHtml?.trim()?.let { "<div class=\"custom-after-items-container\">$it</div>" } ?: ""
        val beforeTotalsHtml = kkm.branding.beforeTotalsHtml?.trim()?.let { "<div class=\"custom-before-totals-container\">$it</div>" } ?: ""
        val afterTotalsHtml = kkm.branding.afterTotalsHtml?.trim()?.let { "<div class=\"custom-after-totals-container\">$it</div>" } ?: ""
        val beforeQrHtml = kkm.branding.beforeQrHtml?.trim()?.let { "<div class=\"custom-before-qr-container\">$it</div>" } ?: ""

        val autonomousModeHtml = if (doc.isAutonomous) {
            "<div class=\"muted center\">${t("Автономный режим", "Автономды режим")}</div>"
        } else {
            ""
        }

        val parentTicketHtml = receipt.parentTicket?.let { parent ->
            val parentDateStr = formatDate(parent.parentTicketDateTimeMillis)
            val parentTotalStr = ReceiptFormatter.formatMoney(parent.parentTicketTotal)
            val parentTitle = if (receipt.operation == ReceiptOperationType.SELL_RETURN || receipt.operation == ReceiptOperationType.BUY_RETURN) {
                t("Чек-основание для возврата", "Қайтаруға негіз болған чек")
            } else {
                t("Сторно-основание", "Сторно негізі")
            }
            """
            <div class="parent-ticket-card">
                <div class="section-title center">$parentTitle</div>
                <table class="parent-ticket-table">
                    <tr><td>${t("Чек №", "Чек №")}</td><td>${parent.parentTicketNumber}</td></tr>
                    <tr><td>${t("Дата и время", "Күні мен уақыты")}</td><td>$parentDateStr</td></tr>
                    <tr><td>${t("ККМ ID", "БАҚ ID")}</td><td>${ReceiptFormatter.escape(parent.kgdKkmId)}</td></tr>
                    <tr><td>${t("Сумма чека", "Чек сомасы")}</td><td>$parentTotalStr</td></tr>
                </table>
            </div>
            <div class="rule"></div>
            """.trimIndent()
        } ?: ""

        val body = """
            $headerHtml
            <table class="meta-table">
                <tr><td>${t("Документ №", "Құжат №")}</td><td>$docNoStr</td></tr>
                <tr><td>${t("Смена №", "Ауысым №")}</td><td>$shiftNoStr</td></tr>
                <tr><td>${t("РНМ", "ТНМ")}</td><td>${ReceiptFormatter.escape(doc.registrationNumber ?: "-")}</td></tr>
                $factoryNumberHtml
                <tr><td>${t("ККМ ID", "БАҚ ID")}</td><td>${ReceiptFormatter.escape(doc.cashboxId)}</td></tr>
                <tr><td>${t("Дата и время", "Күні мен уақыты")}</td><td>$dateStr</td></tr>
                <tr><td>${t("Валюта", "Валюта")}</td><td>$currency</td></tr>
                <tr><td>${t("Статус ОФД", "ОФД статусы")}</td><td>$ofdStatusHtml</td></tr>
            </table>
            <div class="rule"></div>
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
            <div class="rule"></div>
            <table class="meta-table">
                <tr><td>${t("Фискальный признак:", "Фискалдық белгі:")}</td><td class="bold">$escapedSign</td></tr>
                <tr><td>${t("ОФД:", "ОФД:")}</td><td>$ofdProviderName</td></tr>
            </table>
            $customerBinHtml
            $autonomousModeHtml
            $linkHtml
            $beforeQrHtml
            $qrCodeHtml
            <div class="footer center">
                $footerHtml
            </div>
        """.trimIndent()

        return renderPageFrame(opTitle, body, kkm)
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

    private fun operationTitle(type: ReceiptOperationType, lang: ReceiptLanguage, isNarrow: Boolean): String = when (type) {
        ReceiptOperationType.SELL -> translate("ЧЕК ПРОДАЖИ", "САТУ ЧЕГІ", lang, separator = "<br/>", isNarrow = isNarrow)
        ReceiptOperationType.SELL_RETURN -> translate("ЧЕК ВОЗВРАТА ПРОДАЖИ", "САТУДЫ ҚАЙТАРУ ЧЕГІ", lang, separator = "<br/>", isNarrow = isNarrow)
        ReceiptOperationType.BUY -> translate("ЧЕК ПОКУПКИ", "САТЫП АЛУ ЧЕГІ", lang, separator = "<br/>", isNarrow = isNarrow)
        ReceiptOperationType.BUY_RETURN -> translate("ЧЕК ВОЗВРАТА ПОКУПКИ", "САТЫП АЛУДЫ ҚАЙТАРУ ЧЕГІ", lang, separator = "<br/>", isNarrow = isNarrow)
    }

    private fun buildFallbackReceiptUrl(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot): String? {
        val iinBin = doc.taxpayerBin ?: return null
        val regNum = doc.registrationNumber ?: return null
        val docNo = doc.docNo ?: return null
        val dateUnix = doc.createdAt / 1000L
        val fiscalSign = doc.fiscalSign ?: doc.autonomousSign ?: return null
        val sumCents = ReceiptFormatter.moneyToCents(receipt.total)
        val sumStr = String.format(java.util.Locale.US, "%.2f", sumCents.toDouble() / 100.0)
        val providerConfig = doc.ofdProvider?.uppercase()?.let { ofdProviders[it] }
        val domain = providerConfig?.checkDomain ?: "consumer.oofd.kz"
        return "https://$domain/r/$docNo?r=$regNum&i=$iinBin&d=$dateUnix&s=$sumStr&f=$fiscalSign"
    }
}
