package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.application.zxreport.ZxReportBuilder
import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

abstract class ZxReportCommonRenderer : BaseDocumentRenderer() {

    protected fun renderZxReportHtml(
        titleRu: String,
        titleKk: String,
        shift: ShiftInfo,
        counters: Map<String, Long>,
        isZReport: Boolean,
        kkm: KkmInfo,
        ofdStatusHtml: String
    ): String {
        val lang = kkm.branding.language
        val isNarrow = kkm.branding.paperWidthMm <= 58
        fun t(ru: String, kk: String, sep: String = " / "): String = translate(ru, kk, lang, sep, isNarrow)

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
            "<tr><td>${t("Закрыта", "Жабылған күні")}</td><td>$closedStr</td></tr>"
        } else {
            ""
        }

        val registrationNumber = kkm.registrationNumber ?: "-"
        val factoryNumber = kkm.factoryNumber ?: "-"

        val metaHtml = """
            <table class="meta-table">
                <tr><td>${t("Смена №", "Ауысым №")}</td><td>${reportInput.shiftNumber}</td></tr>
                <tr><td>${t("ККМ ID", "БАҚ ID")}</td><td>${shift.kkmId}</td></tr>
                <tr><td>${t("РНМ", "ТНМ")}</td><td>${ReceiptFormatter.escape(registrationNumber)}</td></tr>
                <tr><td>${t("ЗНМ", "ЗНМ")}</td><td>${ReceiptFormatter.escape(factoryNumber)}</td></tr>
                <tr><td>${t("Открыта", "Ашылған күні")}</td><td>${formatDate(reportInput.openShiftTimeMillis)}</td></tr>
                $closedRow
                <tr><td>${t("Время отчета", "Есеп уақыты")}</td><td>${formatDate(reportInput.dateTimeMillis)}</td></tr>
                ${
                    if (isZReport) {
                        "<tr><td>${t("Статус ОФД", "ОФД статусы")}</td><td>$ofdStatusHtml</td></tr>"
                    } else {
                        val transmissionHtml = renderStatusBadge("neutral", "Не передается", "Жіберілмейді", lang)
                        """
                        <tr><td>${t("Фискальный статус", "Фискальді мәртебесі")}</td><td>$ofdStatusHtml</td></tr>
                        <tr><td>${t("Передача в ОФД", "ОФД-ға жіберу")}</td><td>$transmissionHtml</td></tr>
                        """.trimIndent()
                    }
                }
            </table>
        """.trimIndent()

        // 2. Operations (Продажи, Возвраты, Покупки, Возвраты покупок)
        val operationsCards = reportInput.operations.filter { it.count > 0L || it.sumBills > 0L }.joinToString("") { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> t("Продажа", "Сату")
                "OPERATION_SELL_RETURN" -> t("Возврат продажи", "Сатуды қайтару")
                "OPERATION_BUY" -> t("Покупка", "Сатып алу")
                "OPERATION_BUY_RETURN" -> t("Возврат покупки", "Сатып алуды қайтару")
                else -> op.operation
            }
            """
            <div class="tax-row-card">
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-rate-cell bold">$label</td>
                        <td class="tax-sum-cell">${formatAmount(op.sumBills)}</td>
                    </tr>
                    <tr>
                        <td colspan="2" class="tax-details-cell">${t("Чеков", "Чектер")}: ${op.count}</td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }.ifEmpty {
            """
            <div class="tax-row-card">
                <div class="center muted">${t("Нет данных", "Деректер жоқ")}</div>
            </div>
            """.trimIndent()
        }

        val operationsHtml = """
            <div class="section-title">${t("Итоги операций", "Операциялар қорытындысы")}</div>
            <div class="taxes-list">
                $operationsCards
            </div>
        """.trimIndent()

        // 3. Section totals (Отделы)
        val sectionHtml = if (reportInput.sections.isNotEmpty() && reportInput.sections.any { s -> s.operations.any { it.count > 0 || it.sumBills > 0 } }) {
            val secCards = reportInput.sections.flatMap { sec ->
                sec.operations.filter { it.count > 0 || it.sumBills > 0 }.map { op ->
                    val label = when (op.operation) {
                        "OPERATION_SELL" -> t("Продажа", "Сату")
                        "OPERATION_SELL_RETURN" -> t("Возврат продажи", "Сатуды қайтару")
                        "OPERATION_BUY" -> t("Покупка", "Сатып алу")
                        "OPERATION_BUY_RETURN" -> t("Возврат покупки", "Сатып алуды қайтару")
                        else -> op.operation
                    }
                    """
                    <div class="tax-row-card">
                        <table class="tax-row-table">
                            <tr>
                                <td class="tax-rate-cell bold">${t("Отдел", "Бөлім")} ${sec.sectionCode} - $label</td>
                                <td class="tax-sum-cell">${formatAmount(op.sumBills)}</td>
                            </tr>
                            <tr>
                                <td colspan="2" class="tax-details-cell">${t("Чеков", "Чектер")}: ${op.count}</td>
                            </tr>
                        </table>
                    </div>
                    """.trimIndent()
                }
            }.joinToString("")
            """
            <div class="section-title">${t("По отделам", "Бөлімдер бойынша")}</div>
            <div class="taxes-list">
                $secCards
            </div>
            """.trimIndent()
        } else {
            ""
        }

        // 4. Discounts and markups
        val discountCards = reportInput.discounts.filter { it.sumBills > 0L }.map { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> t("Скидки продаж", "Сату жеңілдіктері")
                "OPERATION_SELL_RETURN" -> t("Скидки возвр. продаж", "Сатуды қайтару жеңілдіктері")
                "OPERATION_BUY" -> t("Скидки покупок", "Сатып алу жеңілдіктері")
                "OPERATION_BUY_RETURN" -> t("Скидки возвр. покупок", "Сатып алу қайтар. жеңілдіктері")
                else -> "${t("Скидки", "Жеңілдіктер")} (${op.operation})"
            }
            """
            <div class="tax-row-card">
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-rate-cell">$label</td>
                        <td class="tax-sum-cell" style="color: var(--m3-error); font-weight: bold;">-${formatAmount(op.sumBills)}</td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }
        val markupCards = reportInput.markups.filter { it.sumBills > 0L }.map { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> t("Наценки продаж", "Сату үстемелері")
                "OPERATION_SELL_RETURN" -> t("Наценки возвр. продаж", "Сатуды қайтару үстемелері")
                "OPERATION_BUY" -> t("Наценки покупок", "Сатып алу үстемелері")
                "OPERATION_BUY_RETURN" -> t("Наценки возвр. покупок", "Сатып алу қайтар. үстемелері")
                else -> "${t("Наценки", "Үстемелер")} (${op.operation})"
            }
            """
            <div class="tax-row-card">
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-rate-cell">$label</td>
                        <td class="tax-sum-cell" style="color: var(--m3-success); font-weight: bold;">+${formatAmount(op.sumBills)}</td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }
        val discountsMarkupsHtml = if (discountCards.isNotEmpty() || markupCards.isNotEmpty()) {
            """
            <div class="section-title">${t("Скидки и наценки", "Жеңілдіктер мен үстемелер")}</div>
            <div class="taxes-list">
                ${discountCards.joinToString("")}
                ${markupCards.joinToString("")}
            </div>
            """.trimIndent()
        } else {
            ""
        }

        // 5. Total result
        val totalResultCards = reportInput.totalResult.filter { it.count > 0L || it.sumBills > 0L }.joinToString("") { op ->
            val label = when (op.operation) {
                "OPERATION_SELL" -> t("Итого продаж", "Сату қорытындысы")
                "OPERATION_SELL_RETURN" -> t("Итого возвр. продаж", "Сатуды қайтару қорытындысы")
                "OPERATION_BUY" -> t("Итого покупок", "Сатып алу қорытындысы")
                "OPERATION_BUY_RETURN" -> t("Итого возвр. покупок", "Сатып алуды қайтару қорытындысы")
                else -> op.operation
            }
            """
            <div class="tax-row-card highlighted">
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-rate-cell bold" style="font-size: 1.05em;">$label</td>
                        <td class="tax-sum-cell bold" style="font-size: 1.05em;">${formatAmount(op.sumBills)}</td>
                    </tr>
                    <tr>
                        <td colspan="2" class="tax-details-cell">${t("Чеков", "Чектер")}: ${op.count}</td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }
        val totalResultHtml = if (totalResultCards.isNotEmpty()) {
            """
            <div class="section-title">${t("Окончательные итоги", "Соңғы қорытындылар")}</div>
            <div class="taxes-list">
                $totalResultCards
            </div>
            """.trimIndent()
        } else {
            ""
        }

        // 6. Taxes (Налоги)
        val taxCards = reportInput.taxes.flatMap { taxAgg ->
            val label = when (taxAgg.taxTypeCode) {
                "TAX_TYPE_VAT_0" -> t("НДС 0%", "ҚҚС 0%")
                "TAX_TYPE_VAT_5" -> t("НДС 5%", "ҚҚС 5%")
                "TAX_TYPE_VAT_10" -> t("НДС 10%", "ҚҚС 10%")
                "TAX_TYPE_VAT_12" -> t("НДС 12%", "ҚҚС 12%")
                "TAX_TYPE_VAT_16" -> t("НДС 16%", "ҚҚС 16%")
                "TAX_TYPE_NO_VAT" -> t("Без НДС", "ҚҚС-сыз")
                else -> taxAgg.taxTypeCode
            }
            taxAgg.operations.filter { it.turnoverBills > 0L || it.taxSumBills > 0L }.map { op ->
                val opLabel = when (op.operation) {
                    "OPERATION_SELL" -> t("Продажа", "Сату")
                    "OPERATION_SELL_RETURN" -> t("Возврат продажи", "Сатуды қайтару")
                    "OPERATION_BUY" -> t("Покупка", "Сатып алу")
                    "OPERATION_BUY_RETURN" -> t("Возврат покупки", "Сатып алуды қайтару")
                    else -> op.operation
                }
                """
                <div class="tax-row-card">
                    <table class="tax-row-table">
                        <tr>
                            <td class="tax-rate-cell bold">$label ($opLabel)</td>
                            <td class="tax-sum-cell">${formatAmount(op.taxSumBills)}</td>
                        </tr>
                        <tr>
                            <td colspan="2" class="tax-details-cell">${t("Оборот", "Айналым")}: ${formatAmount(op.turnoverBills)}</td>
                        </tr>
                    </table>
                </div>
                """.trimIndent()
            }
        }.joinToString("")
        val taxesHtml = if (taxCards.isNotEmpty()) {
            """
            <div class="section-title">${t("Налоги по операциям", "Операциялар бойынша салықтар")}</div>
            <div class="taxes-list">
                $taxCards
            </div>
            """.trimIndent()
        } else {
            ""
        }

        // 7. Payment types summary
        val paymentSectionsHtml = reportInput.ticketOperations.filter { it.ticketsCount > 0L || it.ticketsSumBills > 0L }.joinToString("") { op ->
            val opLabel = when (op.operation) {
                "OPERATION_SELL" -> t("Продажи", "Сату")
                "OPERATION_SELL_RETURN" -> t("Возвраты продаж", "Сатуды қайтару")
                "OPERATION_BUY" -> t("Покупки", "Сатып алу")
                "OPERATION_BUY_RETURN" -> t("Возвраты покупок", "Сатып алуды қайтару")
                else -> op.operation
            }
            val payRows = op.payments.filter { it.sumBills > 0L || it.count > 0L }.joinToString("") { pay ->
                val payLabel = when (pay.payment) {
                    "PAYMENT_CASH" -> t("Наличные", "Қолма-қол")
                    "PAYMENT_CARD" -> t("Карта", "Карта")
                    "PAYMENT_CREDIT" -> t("Кредит", "Кредит")
                    "PAYMENT_TARE" -> t("Тара", "Тара")
                    "PAYMENT_MOBILE" -> t("Мобильные", "Мобильді")
                    "PAYMENT_ELECTRONIC" -> t("Электронные", "Электронды")
                    else -> pay.payment
                }
                """
                <tr>
                    <td class="tax-details-cell" style="padding-left: 8px;">&bull; $payLabel (${pay.count})</td>
                    <td class="tax-sum-cell" style="font-size: 0.95em; color: var(--m3-on-surface-variant); font-weight: normal;">${formatAmount(pay.sumBills)}</td>
                </tr>
                """.trimIndent()
            }
            val detailRows = if (payRows.isNotEmpty()) payRows else """
                <tr>
                    <td colspan="2" class="tax-details-cell" style="padding-left: 8px;">${t("Нет төлемдер", "Төлемдер жоқ")}</td>
                </tr>
            """.trimIndent()

            """
            <div class="tax-row-card dashed">
                <table class="tax-row-table">
                    <tr>
                        <td class="tax-rate-cell bold">$opLabel</td>
                        <td class="tax-sum-cell bold">${formatAmount(op.ticketsSumBills)}</td>
                    </tr>
                    <tr>
                        <td colspan="2" class="tax-details-cell" style="padding-bottom: 4px;">${t("Чеков", "Чектер")}: ${op.ticketsCount}</td>
                    </tr>
                    $detailRows
                </table>
            </div>
            """.trimIndent()
        }
        val paymentsSummaryHtml = if (paymentSectionsHtml.isNotEmpty()) {
            """
            <div class="section-title">${t("Типы расчетов", "Төлем түрлері")}</div>
            <div class="taxes-list">
                $paymentSectionsHtml
            </div>
            """.trimIndent()
        } else {
            ""
        }

        // 8. Non-nullable sums
        val nonNullableCards = mutableListOf<String>()
        for (i in reportInput.nonNullableSums.indices) {
            val endPair = reportInput.nonNullableSums[i]
            val op = endPair.first
            val startSum = reportInput.startShiftNonNullableSums.firstOrNull { it.first == op }?.second ?: 0L
            val endSum = endPair.second

            val label = when (op) {
                "OPERATION_SELL" -> t("Продажи", "Сату")
                "OPERATION_SELL_RETURN" -> t("Возвраты продаж", "Сатуды қайтару")
                "OPERATION_BUY" -> t("Покупки", "Сатып алу")
                "OPERATION_BUY_RETURN" -> t("Возвраты покупок", "Сатып алуды қайтару")
                else -> op
            }

            nonNullableCards += """
            <div class="tax-row-card">
                <table class="tax-row-table">
                    <tr>
                        <td colspan="2" class="tax-rate-cell bold">$label</td>
                    </tr>
                    <tr>
                        <td class="tax-details-cell">${t("На начало", "Басында")}:</td>
                        <td class="tax-sum-cell" style="font-size: 0.95em; color: var(--m3-on-surface-variant); font-weight: normal;">${formatAmount(startSum)}</td>
                    </tr>
                    <tr>
                        <td class="tax-details-cell">${t("На конец", "Соңында")}:</td>
                        <td class="tax-sum-cell" style="font-size: 0.95em; color: var(--m3-on-surface); font-weight: bold;">${formatAmount(endSum)}</td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }
        val nonNullableHtml = """
            <div class="section-title">${t("Необнуляемые итоги", "Өшпелі жиынтықтар")}</div>
            <div class="taxes-list">
                ${nonNullableCards.joinToString("")}
            </div>
        """.trimIndent()

        // 9. Cash and revenue operations
        val cashInPl = reportInput.moneyPlacements.firstOrNull { it.operation == "MONEY_PLACEMENT_DEPOSIT" }
        val cashOutPl = reportInput.moneyPlacements.firstOrNull { it.operation == "MONEY_PLACEMENT_WITHDRAWAL" }
        val cashInSum = cashInPl?.operationsSumBills ?: 0L
        val cashInCount = cashInPl?.operationsCount ?: 0L
        val cashOutSum = cashOutPl?.operationsSumBills ?: 0L
        val cashOutCount = cashOutPl?.operationsCount ?: 0L

        val revenueSign = if (reportInput.revenueBills < 0) "-" else ""
        val revenueSumStr = formatAmount(kotlin.math.abs(reportInput.revenueBills))

        val cashOperationsHtml = """
            <div class="section-title">${t("Кассовые операции", "Кассалық операциялар")}</div>
            <table class="meta-table">
                <tr><td>${t("Внесений (кол-во)", "Енгізу (саны)")}</td><td>$cashInCount</td></tr>
                <tr><td>${t("Внесено всего", "Барлығы енгізілді")}</td><td>${formatAmount(cashInSum)}</td></tr>
                <tr><td>${t("Изъятий (кол-во)", "Алу (саны)")}</td><td>$cashOutCount</td></tr>
                <tr><td>${t("Изъято всего", "Барлығы алынды")}</td><td>${formatAmount(cashOutSum)}</td></tr>
                <tr class="bold"><td>${t("Наличных в кассе", "Кассадағы нақты ақша")}</td><td>${formatAmount(reportInput.cashSumBills)}</td></tr>
                <tr class="bold"><td>${t("Выручка смены", "Ауысым түсімі")}</td><td>$revenueSign$revenueSumStr</td></tr>
            </table>
        """.trimIndent()

        // 10. Footer info
        val footerStatus = if (isZReport) {
            t("Смена закрыта.", "Ауысым жабылды.")
        } else {
            t("Смена не закрыта.", "Ауысым жабылған жоқ.")
        }
        val footerInfo = t("Печатная форма отчета.", "Есептің баспа түрі.")

        val title = translate(titleRu, titleKk, lang, separator = "<br/>", isNarrow = isNarrow)

        val headerHtml = renderHeaderHtml(title, kkm, lang)
        val footerHtml = renderFooterHtml(kkm)

        val body = """
            $headerHtml
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
                $footerHtml
            </div>
        """.trimIndent()

        return renderPageFrame(translate(titleRu, titleKk, lang), body, kkm)
    }
}
