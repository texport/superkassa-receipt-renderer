package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.application.zxreport.ZxReportBuilder
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.ReceiptLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Формирует печатные версии (HTML) для X-отчёта, открытия/закрытия смены, внесения/изъятия.
 * Унифицирован с общим дизайном чеков и полностью локализован и кастомизирован (RU / KK / MIXED).
 */
object ReportPrintRenderer {

    private fun formatDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return instant.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }

    private fun formatAmount(bills: Long): String =
        String.format(Locale.US, "%.2f", bills.toDouble() / 100.0)

    private fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage, separator: String = " / "): String {
        return when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> "$labelRu$separator$labelKk"
        }
    }

    private fun htmlPage(title: String, bodyContent: String, config: ReceiptBranding): String = """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <title>$title</title>
            <style>
                @page { size: A4; margin: 10mm; }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    padding: 20px 8px;
                    color: #2d3748;
                    background-color: #f7fafc;
                    font-family: "Inter", "DejaVu Sans Mono", "Courier New", monospace;
                    font-size: 11px;
                    line-height: 1.4;
                    -webkit-font-smoothing: antialiased;
                }
                .receipt {
                    max-width: 85mm;
                    margin: 0 auto;
                    background: #ffffff;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
                }
                .center { text-align: center; }
                .muted { color: #718096; font-size: 10px; margin-top: 4px; }
                .rule {
                    border-top: 1px dashed #cbd5e0;
                    margin: 12px 0;
                }
                .brand-header {
                    margin-bottom: 14px;
                }
                .brand-logo {
                    font-size: 16px;
                    font-weight: 800;
                    letter-spacing: 1px;
                    color: #1a202c;
                    margin-bottom: 4px;
                }
                .brand-logo-img { text-align: center; margin-bottom: 8px; }
                .brand-logo-img img { max-width: 100%; max-height: 80px; object-fit: contain; }
                .doc-title {
                    font-size: 12px;
                    font-weight: 700;
                    color: #2b6cb0;
                    text-transform: uppercase;
                    margin-top: 6px;
                }
                .meta-table, .data-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .meta-table td, .data-table td {
                    padding: 4px 0;
                    vertical-align: middle;
                }
                .meta-table td:first-child {
                    width: 55%;
                    color: #718096;
                }
                .meta-table td:last-child {
                    text-align: right;
                    font-weight: 600;
                    color: #2d3748;
                }
                .section-title {
                    font-size: 10px;
                    font-weight: 700;
                    text-transform: uppercase;
                    color: #4a5568;
                    margin: 16px 0 6px 0;
                    border-bottom: 2px solid #cbd5e0;
                    padding-bottom: 3px;
                }
                .data-table th {
                    font-size: 9px;
                    color: #718096;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    border-bottom: 1px solid #e2e8f0;
                    padding: 4px 0;
                    text-align: left;
                }
                .data-table td {
                    padding: 4px 0;
                    border-bottom: 1px solid #edf2f7;
                }
                .num {
                    text-align: right;
                    white-space: nowrap;
                }
                .bold {
                    font-weight: 700;
                    color: #1a202c;
                }
                .footer {
                    margin-top: 14px;
                    font-size: 10px;
                    color: #4a5568;
                }
                .footer-item {
                    margin-bottom: 4px;
                }
                @media print {
                    body { background-color: #ffffff; padding: 0; color: #000000; }
                    .receipt { border: 0; max-width: none; padding: 0; box-shadow: none; }
                }
                ${config.customCss ?: ""}
            </style>
        </head>
        <body>
            <div class="receipt">
                $bodyContent
            </div>
        </body>
        </html>
    """.trimIndent()

    private fun renderZxReportCommon(
        titleRu: String,
        titleKk: String,
        shift: ShiftInfo,
        counters: Map<String, Long>,
        isZReport: Boolean,
        config: ReceiptBranding
    ): String {
        val lang = config.language
        val reportInput = ZxReportBuilder.build(
            counters = counters,
            dateTimeMillis = shift.closedAt ?: System.currentTimeMillis(),
            shiftNumber = shift.shiftNo.toInt(),
            openShiftTimeMillis = shift.openedAt,
            closeShiftTimeMillis = shift.closedAt
        )

        // 1. Meta-information
        val closedRow = if (isZReport) {
            val closedStr = reportInput.closeShiftTimeMillis?.let { formatDate(it) } ?: "-"
            "<tr><td>${translate("Закрыта", "Жабылған күні", lang)}</td><td>$closedStr</td></tr>"
        } else {
            ""
        }

        val metaHtml = """
            <table class="meta-table">
                <tr><td>${translate("Смена №", "Ауысым №", lang)}</td><td>${reportInput.shiftNumber}</td></tr>
                <tr><td>${translate("ККМ ID", "БАҚ ID", lang)}</td><td>${shift.kkmId}</td></tr>
                <tr><td>${translate("Открыта", "Ашылған күні", lang)}</td><td>${formatDate(reportInput.openShiftTimeMillis)}</td></tr>
                $closedRow
                <tr><td>${translate("Время отчета", "Есеп уақыты", lang)}</td><td>${formatDate(reportInput.dateTimeMillis)}</td></tr>
            </table>
        """.trimIndent()

        // 2. Operations (Продажи, Возвраты, Покупки, Возвраты покупок)
        val operationsRows = reportInput.operations.filter { it.count > 0L || it.sumBills > 0L }.joinToString("") { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> translate("Продажа", "Сату", lang)
                "OPERATION_SELL_RETURN" -> translate("Возврат продажи", "Сатуды қайтару", lang)
                "OPERATION_BUY" -> translate("Покупка", "Сатып алу", lang)
                "OPERATION_BUY_RETURN" -> translate("Возврат покупки", "Сатып алуды қайтару", lang)
                else -> op.operation
            }
            """
            <tr>
                <td>$label</td>
                <td class="num">${op.count}</td>
                <td class="num">${formatAmount(op.sumBills)}</td>
            </tr>
            """.trimIndent()
        }.ifEmpty { "<tr><td colspan=\"3\">${translate("Нет данных", "Деректер жоқ", lang)}</td></tr>" }

        val operationsHtml = """
            <div class="section-title">${translate("Итоги операций", "Операциялар қорытындысы", lang)}</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>${translate("Операция", "Операция", lang)}</th>
                        <th class="num">${translate("Чеков", "Чектер", lang)}</th>
                        <th class="num">${translate("Сумма", "Сомасы", lang)}</th>
                    </tr>
                </thead>
                <tbody>
                    $operationsRows
                </tbody>
            </table>
        """.trimIndent()

        // 3. Section totals (Отделы)
        val sectionHtml = if (reportInput.sections.isNotEmpty() && reportInput.sections.any { s -> s.operations.any { it.count > 0 || it.sumBills > 0 } }) {
            val secRows = reportInput.sections.flatMap { sec ->
                sec.operations.filter { it.count > 0 || it.sumBills > 0 }.map { op ->
                    val label = when (op.operation) {
                        "OPERATION_SELL" -> translate("Продажа", "Сату", lang)
                        "OPERATION_SELL_RETURN" -> translate("Возврат продажи", "Сатуды қайтару", lang)
                        "OPERATION_BUY" -> translate("Покупка", "Сатып алу", lang)
                        "OPERATION_BUY_RETURN" -> translate("Возврат покупки", "Сатып алуды қайтару", lang)
                        else -> op.operation
                    }
                    """
                    <tr>
                        <td>${translate("Отдел", "Бөлім", lang)} ${sec.sectionCode} - $label</td>
                        <td class="num">${op.count}</td>
                        <td class="num">${formatAmount(op.sumBills)}</td>
                    </tr>
                    """.trimIndent()
                }
            }.joinToString("")
            """
            <div class="section-title">${translate("По отделам", "Бөлімдер бойынша", lang)}</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>${translate("Отдел и операция", "Бөлім мен операция", lang)}</th>
                        <th class="num">${translate("Чеков", "Чектер", lang)}</th>
                        <th class="num">${translate("Сумма", "Сомасы", lang)}</th>
                    </tr>
                </thead>
                <tbody>
                    $secRows
                </tbody>
            </table>
            """.trimIndent()
        } else {
            ""
        }

        // 4. Discounts and markups
        val discountRows = reportInput.discounts.filter { it.sumBills > 0L }.map { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> translate("Скидки продаж", "Сату жеңілдіктері", lang)
                "OPERATION_SELL_RETURN" -> translate("Скидки возвр. продаж", "Сатуды қайтару жеңілдіктері", lang)
                "OPERATION_BUY" -> translate("Скидки покупок", "Сатып алу жеңілдіктері", lang)
                "OPERATION_BUY_RETURN" -> translate("Скидки возвр. покупок", "Сатып алу қайтар. жеңілдіктері", lang)
                else -> "${translate("Скидки", "Жеңілдіктер", lang)} (${op.operation})"
            }
            "<tr><td>$label</td><td class=\"num\">-${formatAmount(op.sumBills)}</td></tr>"
        }
        val markupRows = reportInput.markups.filter { it.sumBills > 0L }.map { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> translate("Наценки продаж", "Сату үстемелері", lang)
                "OPERATION_SELL_RETURN" -> translate("Наценки возвр. продаж", "Сатуды қайтару үстемелері", lang)
                "OPERATION_BUY" -> translate("Наценки покупок", "Сатып алу үстемелері", lang)
                "OPERATION_BUY_RETURN" -> translate("Наценки возвр. покупок", "Сатып алу қайтар. үстемелері", lang)
                else -> "${translate("Наценки", "Үстемелер", lang)} (${op.operation})"
            }
            "<tr><td>$label</td><td class=\"num\">+${formatAmount(op.sumBills)}</td></tr>"
        }
        val discountsMarkupsHtml = if (discountRows.isNotEmpty() || markupRows.isNotEmpty()) {
            """
            <div class="section-title">${translate("Скидки и наценки", "Жеңілдіктер мен үстемелер", lang)}</div>
            <table class="data-table">
                <tbody>
                    ${discountRows.joinToString("")}
                    ${markupRows.joinToString("")}
                </tbody>
            </table>
            """.trimIndent()
        } else {
            ""
        }

        // 5. Total result
        val totalResultRows = reportInput.totalResult.filter { it.count > 0L || it.sumBills > 0L }.joinToString("") { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> translate("Итого продаж", "Сату қорытындысы", lang)
                "OPERATION_SELL_RETURN" -> translate("Итого возвр. продаж", "Сатуды қайтару қорытындысы", lang)
                "OPERATION_BUY" -> translate("Итого покупок", "Сатып алу қорытындысы", lang)
                "OPERATION_BUY_RETURN" -> translate("Итого возвр. покупок", "Сатып алуды қайтару қорытындысы", lang)
                else -> op.operation
            }
            """
            <tr class="bold">
                <td>$label</td>
                <td class="num">${op.count}</td>
                <td class="num">${formatAmount(op.sumBills)}</td>
            </tr>
            """.trimIndent()
        }
        val totalResultHtml = if (totalResultRows.isNotEmpty()) {
            """
            <div class="section-title">${translate("Окончательные итоги", "Соңғы қорытындылар", lang)}</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>${translate("Направление", "Бағыты", lang)}</th>
                        <th class="num">${translate("Чеков", "Чектер", lang)}</th>
                        <th class="num">${translate("Сумма", "Сомасы", lang)}</th>
                    </tr>
                </thead>
                <tbody>
                    $totalResultRows
                </tbody>
            </table>
            """.trimIndent()
        } else {
            ""
        }

        // 6. Taxes (Налоги)
        val taxRows = reportInput.taxes.flatMap { taxAgg ->
            val label = when (taxAgg.taxTypeCode) {
                "TAX_TYPE_VAT_0" -> translate("НДС 0%", "ҚҚС 0%", lang)
                "TAX_TYPE_VAT_5" -> translate("НДС 5%", "ҚҚС 5%", lang)
                "TAX_TYPE_VAT_10" -> translate("НДС 10%", "ҚҚС 10%", lang)
                "TAX_TYPE_VAT_12" -> translate("НДС 12%", "ҚҚС 12%", lang)
                "TAX_TYPE_VAT_16" -> translate("НДС 16%", "ҚҚС 16%", lang)
                "TAX_TYPE_NO_VAT" -> translate("Без НДС", "ҚҚС-сыз", lang)
                else -> taxAgg.taxTypeCode
            }
            taxAgg.operations.filter { it.turnoverBills > 0L || it.taxSumBills > 0L }.map { op ->
                val opLabel = when (op.operation) {
                    "OPERATION_SELL" -> translate("Продажа", "Сату", lang)
                    "OPERATION_SELL_RETURN" -> translate("Возврат продажи", "Сатуды қайтару", lang)
                    "OPERATION_BUY" -> translate("Покупка", "Сатып алу", lang)
                    "OPERATION_BUY_RETURN" -> translate("Возврат покупки", "Сатып алуды қайтару", lang)
                    else -> op.operation
                }
                """
                <tr>
                    <td>$label ($opLabel)</td>
                    <td class="num">${formatAmount(op.turnoverBills)}</td>
                    <td class="num">${formatAmount(op.taxSumBills)}</td>
                </tr>
                """.trimIndent()
            }
        }.joinToString("")
        val taxesHtml = if (taxRows.isNotEmpty()) {
            """
            <div class="section-title">${translate("Налоги по операциям", "Операциялар бойынша салықтар", lang)}</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>${translate("Налог и операция", "Салық пен операция", lang)}</th>
                        <th class="num">${translate("Оборот", "Айналым", lang)}</th>
                        <th class="num">${translate("НДС", "ҚҚС", lang)}</th>
                    </tr>
                </thead>
                <tbody>
                    $taxRows
                </tbody>
            </table>
            """.trimIndent()
        } else {
            ""
        }

        // 7. Payment types summary
        val paymentSectionsHtml = reportInput.ticketOperations.filter { it.ticketsCount > 0L || it.ticketsSumBills > 0L }.joinToString("") { op ->
            val opLabel = when (op.operation) {
                "OPERATION_SELL" -> translate("Продажи", "Сату", lang)
                "OPERATION_SELL_RETURN" -> translate("Возвраты продаж", "Сатуды қайтару", lang)
                "OPERATION_BUY" -> translate("Покупки", "Сатып алу", lang)
                "OPERATION_BUY_RETURN" -> translate("Возвраты покупок", "Сатып алуды қайтару", lang)
                else -> op.operation
            }
            val payRows = op.payments.filter { it.sumBills > 0L || it.count > 0L }.joinToString("") { pay ->
                val payLabel = when (pay.payment) {
                    "PAYMENT_CASH" -> translate("Наличные", "Қолма-қол", lang)
                    "PAYMENT_CARD" -> translate("Карта", "Карта", lang)
                    "PAYMENT_CREDIT" -> translate("Кредит", "Кредит", lang)
                    "PAYMENT_TARE" -> translate("Тара", "Тара", lang)
                    "PAYMENT_MOBILE" -> translate("Мобильные", "Мобильді", lang)
                    "PAYMENT_ELECTRONIC" -> translate("Электронные", "Электронды", lang)
                    else -> pay.payment
                }
                "<tr><td style=\"padding-left: 15px;\">$payLabel</td><td class=\"num\">${pay.count}</td><td class=\"num\">${formatAmount(pay.sumBills)}</td></tr>"
            }
            val detailRows = if (payRows.isNotEmpty()) payRows else "<tr><td colspan=\"3\" style=\"padding-left: 15px;\">${translate("Нет платежей", "Төлемдер жоқ", lang)}</td></tr>"
            """
            <tr class="bold">
                <td>$opLabel</td>
                <td class="num">${op.ticketsCount}</td>
                <td class="num">${formatAmount(op.ticketsSumBills)}</td>
            </tr>
            $detailRows
            """.trimIndent()
        }
        val paymentsSummaryHtml = if (paymentSectionsHtml.isNotEmpty()) {
            """
            <div class="section-title">${translate("Типы расчетов", "Төлем түрлері", lang)}</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>${translate("Операция и оплата", "Операция мен төлем", lang)}</th>
                        <th class="num">${translate("Чеков", "Чектер", lang)}</th>
                        <th class="num">${translate("Сумма", "Сомасы", lang)}</th>
                    </tr>
                </thead>
                <tbody>
                    $paymentSectionsHtml
                </tbody>
            </table>
            """.trimIndent()
        } else {
            ""
        }

        // 8. Non-nullable sums
        val nonNullableRows = mutableListOf<String>()
        for (i in reportInput.nonNullableSums.indices) {
            val endPair = reportInput.nonNullableSums[i]
            val op = endPair.first
            val startSum = reportInput.startShiftNonNullableSums.firstOrNull { it.first == op }?.second ?: 0L
            val endSum = endPair.second

            val label = when (op) {
                "OPERATION_SELL" -> translate("Продажи", "Сату", lang)
                "OPERATION_SELL_RETURN" -> translate("Возвраты продаж", "Сатуды қайтару", lang)
                "OPERATION_BUY" -> translate("Покупки", "Сатып алу", lang)
                "OPERATION_BUY_RETURN" -> translate("Возвраты покупок", "Сатып алуды қайтару", lang)
                else -> op
            }

            nonNullableRows += """
            <tr>
                <td>$label</td>
                <td class="num">${formatAmount(startSum)}</td>
                <td class="num">${formatAmount(endSum)}</td>
            </tr>
            """.trimIndent()
        }
        val nonNullableHtml = """
            <div class="section-title">${translate("Необнуляемые итоги", "Өшпелі жиынтықтар", lang)}</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>${translate("Операция", "Операция", lang)}</th>
                        <th class="num">${translate("На начало", "Басында", lang)}</th>
                        <th class="num">${translate("На конец", "Соңында", lang)}</th>
                    </tr>
                </thead>
                <tbody>
                    ${nonNullableRows.joinToString("")}
                </tbody>
            </table>
        """.trimIndent()

        // 9. Cash and revenue operations
        val cashInPl = reportInput.moneyPlacements.firstOrNull { it.operation == "PLACEMENT_CASH_IN" }
        val cashOutPl = reportInput.moneyPlacements.firstOrNull { it.operation == "PLACEMENT_CASH_OUT" }
        val cashInSum = cashInPl?.operationsSumBills ?: 0L
        val cashInCount = cashInPl?.operationsCount ?: 0L
        val cashOutSum = cashOutPl?.operationsSumBills ?: 0L
        val cashOutCount = cashOutPl?.operationsCount ?: 0L

        val revenueSign = if (reportInput.revenueBills < 0) "-" else ""
        val revenueSumStr = formatAmount(kotlin.math.abs(reportInput.revenueBills))

        val cashOperationsHtml = """
            <div class="section-title">${translate("Кассовые операции", "Кассалық операциялар", lang)}</div>
            <table class="meta-table">
                <tr><td>${translate("Внесений (кол-во)", "Енгізу (саны)", lang)}</td><td>$cashInCount</td></tr>
                <tr><td>${translate("Внесено всего", "Барлығы енгізілді", lang)}</td><td>${formatAmount(cashInSum)}</td></tr>
                <tr><td>${translate("Изъятий (кол-во)", "Алу (саны)", lang)}</td><td>$cashOutCount</td></tr>
                <tr><td>${translate("Изъято всего", "Барлығы алынды", lang)}</td><td>${formatAmount(cashOutSum)}</td></tr>
                <tr class="bold"><td>${translate("Наличных в кассе", "Кассадағы нақты ақша", lang)}</td><td>${formatAmount(reportInput.cashSumBills)}</td></tr>
                <tr class="bold"><td>${translate("Выручка смены", "Ауысым түсімі", lang)}</td><td>$revenueSign$revenueSumStr</td></tr>
            </table>
        """.trimIndent()

        // 10. Footer info
        val footerStatus = if (isZReport) {
            translate("Смена закрыта.", "Ауысым жабылды.", lang)
        } else {
            translate("Смена не закрыта.", "Ауысым жабылған жоқ.", lang)
        }
        val footerInfo = translate("Печатная форма отчета.", "Есептің баспа түрі.", lang)

        val title = translate(titleRu, titleKk, lang, separator = "<br>")

        val headerLogoHtml = config.headerLogoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
            """<div class="brand-logo-img"><img src="${ReceiptFormatter.escape(url)}" alt="Logo" /></div>"""
        } ?: ""
        val headerHtmlContent = config.headerHtml?.trim() ?: ""
        val footerHtmlContent = config.footerHtml?.trim() ?: ""

        val body = """
            <div class="center brand-header">
                $headerLogoHtml
                $headerHtmlContent
                <div class="brand-logo">SUPERKASSA</div>
                <div class="rule"></div>
                <div class="doc-title">$title</div>
            </div>
            $metaHtml
            <div class="rule"></div>
            $operationsHtml
            $sectionHtml
            $discountsMarkupsHtml
            $totalResultHtml
            $taxesHtml
            $paymentsSummaryHtml
            $nonNullableHtml
            <div class="rule"></div>
            $cashOperationsHtml
            <div class="rule"></div>
            <div class="footer center">
                <div class="footer-item bold">$footerStatus</div>
                <div class="footer-item muted">$footerInfo</div>
                $footerHtmlContent
            </div>
        """.trimIndent()

        return htmlPage(translate(titleRu, titleKk, lang), body, config)
    }

    /** Печатная форма X-отчёта (смена не закрывается). */
    fun renderXReportHtml(shift: ShiftInfo, counters: Map<String, Long>, config: ReceiptBranding = ReceiptBranding()): String {
        return renderZxReportCommon(
            titleRu = "X-ОТЧЁТ (БЕЗ ГАШЕНИЯ)",
            titleKk = "Х-ЕСЕП (АУЫСЫМДЫ ЖАППАЙ)",
            shift = shift,
            counters = counters,
            isZReport = false,
            config = config
        )
    }

    /** Печатная форма закрытия смены (Z-отчёт). */
    fun renderCloseShiftHtml(shift: ShiftInfo, counters: Map<String, Long>, config: ReceiptBranding = ReceiptBranding()): String {
        return renderZxReportCommon(
            titleRu = "Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)",
            titleKk = "Z-ЕСЕП (АУЫСЫМДЫ ЖАБУ)",
            shift = shift,
            counters = counters,
            isZReport = true,
            config = config
        )
    }

    /** Печатная форма открытия смены. */
    fun renderOpenShiftHtml(shift: ShiftInfo, config: ReceiptBranding = ReceiptBranding()): String {
        val lang = config.language
        val headerLogoHtml = config.headerLogoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
            """<div class="brand-logo-img"><img src="${ReceiptFormatter.escape(url)}" alt="Logo" /></div>"""
        } ?: ""
        val headerHtmlContent = config.headerHtml?.trim() ?: ""
        val footerHtmlContent = config.footerHtml?.trim() ?: ""

        val body = """
            <div class="center brand-header">
                $headerLogoHtml
                $headerHtmlContent
                <div class="brand-logo">SUPERKASSA</div>
                <div class="rule"></div>
                <div class="doc-title">${translate("ОТКРЫТИЕ СМЕНЫ", "АУЫСЫМДЫ АШУ", lang)}</div>
            </div>
            <table class="meta-table">
                <tr><td>${translate("Смена №", "Ауысым №", lang)}</td><td>${shift.shiftNo}</td></tr>
                <tr><td>${translate("ККМ ID", "БАҚ ID", lang)}</td><td>${shift.kkmId}</td></tr>
                <tr><td>${translate("Время открытия", "Ашу уақыты", lang)}</td><td>${formatDate(shift.openedAt)}</td></tr>
            </table>
            <div class="rule"></div>
            <div class="footer center">
                <div class="footer-item bold">${translate("Смена открыта.", "Ауысым ашылды.", lang)}</div>
                <div class="footer-item muted">${translate("Печатная форма документа.", "Құжаттың баспа түрі.", lang)}</div>
                $footerHtmlContent
            </div>
        """.trimIndent()
        return htmlPage(translate("Открытие смены", "Ауысымды ашу", lang), body, config)
    }

    /** Печатная форма внесения или изъятия наличных. */
    fun renderCashOperationHtml(doc: FiscalDocumentSnapshot, config: ReceiptBranding = ReceiptBranding()): String {
        val lang = config.language
        val titleRu = when (doc.docType) {
            "CASH_IN" -> "ВНЕСЕНИЕ НАЛИЧНЫХ"
            "CASH_OUT" -> "ИЗЪЯТИЕ НАЛИЧНЫХ"
            else -> "ОПЕРАЦИЯ С НАЛИЧНЫМИ"
        }
        val titleKk = when (doc.docType) {
            "CASH_IN" -> "НАҚТЫ АҚШАНЫ ЕНГІЗУ"
            "CASH_OUT" -> "НАҚТЫ АҚШАНЫ АЛУ"
            else -> "НАҚТЫ АҚША ОПЕРАЦИЯСЫ"
        }
        val sumStr = doc.totalAmount?.let { formatAmount(it) } ?: "0.00"

        val headerLogoHtml = config.headerLogoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
            """<div class="brand-logo-img"><img src="${ReceiptFormatter.escape(url)}" alt="Logo" /></div>"""
        } ?: ""
        val headerHtmlContent = config.headerHtml?.trim() ?: ""
        val footerHtmlContent = config.footerHtml?.trim() ?: ""

        val body = """
            <div class="center brand-header">
                $headerLogoHtml
                $headerHtmlContent
                <div class="brand-logo">${doc.taxpayerName ?: "SUPERKASSA"}</div>
                <div class="org-bin">${doc.taxpayerBin?.let { "${translate("БИН/ИИН", "БСН/ЖСН", lang)}: $it" } ?: ""}</div>
                <div class="rule"></div>
                <div class="doc-title">${translate(titleRu, titleKk, lang, separator = "<br>")}</div>
            </div>
            <table class="meta-table">
                <tr><td>${translate("Документ №", "Құжат №", lang)}</td><td>${doc.docNo ?: doc.id}</td></tr>
                <tr><td>${translate("ККМ ID", "БАҚ ID", lang)}</td><td>${doc.cashboxId}</td></tr>
                <tr><td>${translate("Смена №", "Ауысым №", lang)}</td><td>${doc.shiftNo ?: "-"}</td></tr>
                <tr><td>${translate("РНМ", "ТНМ", lang)}</td><td>${doc.registrationNumber ?: "-"}</td></tr>
                <tr><td>${translate("Дата и время", "Күні мен уақыты", lang)}</td><td>${formatDate(doc.createdAt)}</td></tr>
                <tr class="bold"><td>${translate("Сумма", "Сомасы", lang)}</td><td>$sumStr ${doc.currency ?: "KZT"}</td></tr>
            </table>
            <div class="rule"></div>
            <div class="footer center">
                <div class="footer-item muted">${translate("Печатная форма операции.", "Операцияның баспа түрі.", lang)}</div>
                $footerHtmlContent
            </div>
        """.trimIndent()
        return htmlPage(translate(titleRu, titleKk, lang), body, config)
    }
}
