package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.ShiftStatus
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportPrintRendererTest {

    @Test
    fun testRenderXReportHtml() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L
        )
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 10L,
            "operation.OPERATION_SELL.sum" to 100050L,
            "operation.OPERATION_SELL_RETURN.count" to 0L,
            "operation.OPERATION_SELL_RETURN.sum" to 0L
        )

        val html = ReportPrintRenderer.renderXReportHtml(shift, counters)
        assertTrue(html.contains("X-ОТЧЁТ"))
        assertTrue(html.contains("Смена № / Ауысым №"))
        assertTrue(html.contains("12"))
        assertTrue(html.contains("Продажа / Сату"))
        assertTrue(html.contains("10"))
        assertTrue(html.contains("1000.50"))
        // 0 counters should be omitted from output
        assertTrue(!html.contains("Возврат продажи"))
    }

    @Test
    fun testRenderXReportHtmlEmptyCounters() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L
        )
        val html = ReportPrintRenderer.renderXReportHtml(shift, emptyMap())
        assertTrue(html.contains("Нет данных / Деректер жоқ"))
    }

    @Test
    fun testRenderOpenShiftHtml() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L
        )
        val html = ReportPrintRenderer.renderOpenShiftHtml(shift)
        assertTrue(html.contains("ОТКРЫТИЕ СМЕНЫ / АУЫСЫМДЫ АШУ"))
        assertTrue(html.contains("Смена № / Ауысым №"))
        assertTrue(html.contains("12"))
    }

    @Test
    fun testRenderCloseShiftHtml() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.CLOSED,
            openedAt = 1782200000000L,
            closedAt = 1782200050000L
        )
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 10L,
            "operation.OPERATION_SELL.sum" to 100050L
        )

        val html = ReportPrintRenderer.renderCloseShiftHtml(shift, counters)
        assertTrue(html.contains("Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)"))
        assertTrue(html.contains("Смена № / Ауысым №"))
        assertTrue(html.contains("12"))
        assertTrue(html.contains("Продажа / Сату"))
        assertTrue(html.contains("1000.50"))
    }

    @Test
    fun testRenderCloseShiftHtmlNullClosedAt() {
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 12,
            status = ShiftStatus.CLOSED,
            openedAt = 1782200000000L,
            closedAt = null
        )
        val html = ReportPrintRenderer.renderCloseShiftHtml(shift, emptyMap())
        assertTrue(html.contains("Закрыта / Жабылған күні"))
        assertTrue(html.contains("-"))
    }

    @Test
    fun testRenderCashOperationHtml() {
        val docIn = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "CASH_IN",
            docNo = 45L,
            shiftNo = 12L,
            createdAt = 1782200000000L,
            totalAmount = 250000L,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val htmlIn = ReportPrintRenderer.renderCashOperationHtml(docIn)
        assertTrue(htmlIn.contains("ВНЕСЕНИЕ НАЛИЧНЫХ"))
        assertTrue(htmlIn.contains("2500.00 KZT") || htmlIn.contains("2500,00 KZT"))
        assertTrue(htmlIn.contains("Документ № / Құжат №"))

        val docOut = FiscalDocumentSnapshot(
            id = "doc-2",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "CASH_OUT",
            docNo = null,
            shiftNo = 12L,
            createdAt = 1782200000000L,
            totalAmount = 10000L,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val htmlOut = ReportPrintRenderer.renderCashOperationHtml(docOut)
        assertTrue(htmlOut.contains("ИЗЪЯТИЕ НАЛИЧНЫХ"))
        assertTrue(htmlOut.contains("100.00 KZT") || htmlOut.contains("100,00 KZT"))

        val docOther = FiscalDocumentSnapshot(
            id = "doc-3",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "OTHER",
            docNo = null,
            shiftNo = 12L,
            createdAt = 1782200000000L,
            totalAmount = null,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )

        val htmlOther = ReportPrintRenderer.renderCashOperationHtml(docOther)
        assertTrue(htmlOther.contains("ОПЕРАЦИЯ С НАЛИЧНЫМИ"))
        assertTrue(htmlOther.contains("0.00 KZT"))
    }
}
