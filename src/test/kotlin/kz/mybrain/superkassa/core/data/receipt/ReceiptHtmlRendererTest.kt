package kz.mybrain.superkassa.core.data.receipt

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.ShiftStatus
import kz.mybrain.superkassa.core.domain.model.TaxRegime
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService
import kotlin.test.Test
import kotlin.test.assertTrue

class ReceiptHtmlRendererTest {

    private class StubQrCodeGenerator : QrCodeGeneratorPort {
        override fun generatePngDataUri(text: String, sizePx: Int): String? {
            return "data:image/png;base64,stub-qr-code-for-$text"
        }
    }

    private class NullQrCodeGenerator : QrCodeGeneratorPort {
        override fun generatePngDataUri(text: String, sizePx: Int): String? {
            return null
        }
    }

    @Test
    fun testRenderHtmlWithAllFields() {
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Apple",
                    sectionCode = "001",
                    quantity = 2,
                    price = Money(50, 0),
                    sum = Money(100, 0),
                    vatGroup = VatGroup.VAT_16,
                    measureUnitCode = "796" // Piece
                ),
                ReceiptItem(
                    name = "Milk",
                    sectionCode = "001",
                    quantity = 1,
                    price = Money(200, 50),
                    sum = Money(200, 50),
                    vatGroup = VatGroup.NO_VAT,
                    measureUnitCode = "166", // kg (custom short unit)
                    listExciseStamp = listOf("EXCISE-123", "EXCISE-456")
                ),
                ReceiptItem(
                    name = "Bread",
                    sectionCode = "001",
                    quantity = 1,
                    price = Money(100, 0),
                    sum = Money(100, 0),
                    vatGroup = VatGroup.VAT_0,
                    measureUnitCode = "999" // Unknown unit to cover default fallback
                ),
                ReceiptItem(
                    name = "Cheese",
                    sectionCode = "001",
                    quantity = 1,
                    price = Money(500, 0),
                    sum = Money(500, 0),
                    vatGroup = VatGroup.VAT_5
                ),
                ReceiptItem(
                    name = "Juice",
                    sectionCode = "001",
                    quantity = 1,
                    price = Money(300, 0),
                    sum = Money(300, 0),
                    vatGroup = VatGroup.VAT_10
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CASH, Money(200, 0)),
                ReceiptPayment(PaymentType.CARD, Money(100, 50)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(1000, 0))
            ),
            total = Money(1200, 50),
            taken = Money(1300, 0),
            change = Money(99, 50),
            idempotencyKey = "key-123",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.NO_VAT,
            discount = Money(10, 0),
            markup = Money(5, 0),
            customerBin = "123456789012"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 12,
            shiftNo = 34,
            createdAt = 1782200000000L,
            totalAmount = 120050L,
            currency = "KZT",
            fiscalSign = "FS-12345",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1782200010000L,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            taxpayerName = "Individual Entrepreneur Ivanov",
            taxpayerBin = "987654321012",
            taxpayerAddress = "Almaty, Abay Ave 10",
            factoryNumber = "FN-888",
            ofdProvider = "KAZAKHTELECOM"
        )

        val html = renderer.renderHtml(request, doc)

        assertTrue(html.contains("ЧЕК ПРОДАЖИ / САТУ ЧЕГІ"))
        assertTrue(html.contains("№ 12"))
        assertTrue(html.contains("Apple"))
        assertTrue(html.contains("шт / дана"))
        assertTrue(html.contains("Milk"))
        assertTrue(html.contains("Маркировка / Таңбалау: EXCISE-123, EXCISE-456"))
        assertTrue(html.contains("Наличные / Қолма-қол"))
        assertTrue(html.contains("Карта / Карта"))
        assertTrue(html.contains("Электронно / Электронды"))
        assertTrue(html.contains("Получено / Алынды"))
        assertTrue(html.contains("Сдача / Қайтарым"))
        assertTrue(html.contains("БИН/ИИН: 987654321012"))
        assertTrue(html.contains("Покупатель / Сатып алушы БИН/ЖСН: 123456789012"))
        assertTrue(html.contains("Individual Entrepreneur Ivanov"))
        assertTrue(html.contains("Almaty, Abay Ave 10"))
        assertTrue(html.contains("ЗНМ / ЗНМ"))
        assertTrue(html.contains("FN-888"))
        assertTrue(html.contains("АО «Казахтелеком» / «Қазақтелеком» АҚ"))
        assertTrue(html.contains("oofd.kz"))
        assertTrue(html.contains("data:image/png;base64,stub-qr-code-for-https://kassa.kz/receipt/789"))
        assertTrue(html.contains("Отправлен / Жіберілді"))
        // Check for VAT Groups (should be empty since defaultVatGroup is NO_VAT under VAT_PAYER)
        assertTrue(!html.contains("Налоги / Салықтар"))
    }

    @Test
    fun testRenderHtmlWithFallbackUrlAndAutonomous() {
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL_RETURN,
            items = emptyList(),
            payments = emptyList(),
            total = Money(150, 0),
            idempotencyKey = "key-456"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = null,
            shiftNo = null,
            createdAt = 1782200000000L,
            totalAmount = 15000L,
            currency = null,
            fiscalSign = null,
            autonomousSign = "AS-54321",
            isAutonomous = true,
            ofdStatus = "OFFLINE",
            deliveredAt = null,
            receiptUrl = null, // Trigger fallback URL building
            registrationNumber = "RN-999",
            taxpayerName = null,
            taxpayerBin = null,
            taxpayerAddress = null,
            factoryNumber = null,
            ofdProvider = "TRANSTELECOM"
        )

        val html = renderer.renderHtml(request, doc)

        assertTrue(html.contains("ВОЗВРАТ ПРОДАЖИ / САТУДЫ ҚАЙТАРУ"))
        assertTrue(html.contains("Офлайн / Автономды"))
        assertTrue(html.contains("Автономный режим / Автономды режим"))
        assertTrue(html.contains("АО «Транстелеком» / «Транстелеком» АҚ"))
        assertTrue(html.contains("o.oofd.kz"))
        // Check fallback url in qr code
        assertTrue(html.contains("stub-qr-code-for-https://consumer.oofd.kz"))
    }

    @Test
    fun testRenderHtmlWithOtherStatusesAndProviders() {
        val renderer = ReceiptHtmlRenderer(NullQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-789"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 10,
            shiftNo = 20,
            createdAt = 1782200000000L,
            totalAmount = 0L,
            currency = "USD",
            fiscalSign = "FS-123",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "UNKNOWN_STATUS",
            deliveredAt = null,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            ofdProvider = "ALTECO"
        )

        val html = renderer.renderHtml(request, doc)

        assertTrue(html.contains("ЧЕК ПОКУПКИ / САТЫП АЛУ ЧЕГІ"))
        assertTrue(html.contains("UNKNOWN_STATUS"))
        assertTrue(html.contains("ТОО «Alteco Partners» / «Alteco Partners» ЖШС"))
        assertTrue(html.contains("alteco.kz"))
        // Since Qr generator returns null, no qr img tag should be present
        assertTrue(!html.contains("<img src=\"\""))
    }

    @Test
    fun testRenderHtmlBuyReturnAndOtherProvider() {
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY_RETURN,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-789"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 10,
            shiftNo = 20,
            createdAt = 1782200000000L,
            totalAmount = 0L,
            currency = "USD",
            fiscalSign = "FS-123",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            ofdProvider = "SOME_OTHER_PROVIDER"
        )

        val html = renderer.renderHtml(request, doc)

        assertTrue(html.contains("ВОЗВРАТ ПОКУПКИ / САТЫП АЛУДЫ ҚАЙТАРУ"))
        assertTrue(html.contains("SOME_OTHER_PROVIDER"))
        assertTrue(html.contains("consumer.oofd.kz"))
    }

    @Test
    fun testProxyMethods() {
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator())
        val shift = ShiftInfo(
            id = "shift-123",
            kkmId = "kkm-123",
            shiftNo = 45,
            status = ShiftStatus.OPEN,
            openedAt = 1782200000000L,
            closedAt = 1782200050000L
        )
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 5L,
            "operation.OPERATION_SELL.sum" to 15000L
        )

        val xReport = renderer.renderXReportHtml(shift, counters)
        assertTrue(xReport.contains("X-ОТЧЁТ"))
        assertTrue(xReport.contains("Смена № 45"))

        val openShift = renderer.renderOpenShiftHtml(shift)
        assertTrue(openShift.contains("ОТКРЫТИЕ СМЕНЫ"))

        val closeShift = renderer.renderCloseShiftHtml(shift, counters)
        assertTrue(closeShift.contains("Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)"))

        val doc = FiscalDocumentSnapshot(
            id = "doc-999",
            cashboxId = "kkm-123",
            shiftId = "shift-123",
            docType = "CASH_IN",
            docNo = 100,
            shiftNo = 45,
            createdAt = 1782200000000L,
            totalAmount = 50000L,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null
        )
        val cashOp = renderer.renderCashOperationHtml(doc)
        assertTrue(cashOp.contains("ВНЕСЕНИЕ НАЛИЧНЫХ"))
        assertTrue(cashOp.contains("50000.00") || cashOp.contains("50000,00"))
    }

    @Test
    fun testRenderHtmlWithNullOfdAndRegistration() {
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator())

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-abc"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = null,
            shiftNo = null,
            createdAt = 1782200000000L,
            totalAmount = null,
            currency = null,
            fiscalSign = null,
            autonomousSign = null, // No signs and no registration number means buildFallbackReceiptUrl returns null
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = null,
            registrationNumber = null,
            ofdProvider = null
        )

        val html = renderer.renderHtml(request, doc)

        // Verify that the HTML was rendered successfully and does not contain ofdProvider info or fallback URL
        assertTrue(html.contains("SUPERKASSA"))
        assertTrue(!html.contains("ОФД / ОФД:"))
        assertTrue(!html.contains("Ссылка на чек"))
    }

    @Test
    fun testBuildFallbackReceiptUrlReturnsNullWhenNoSigns() {
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator())
        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-abc"
        )
        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = null,
            shiftNo = null,
            createdAt = 1782200000000L,
            totalAmount = null,
            currency = null,
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = null,
            registrationNumber = "RN-999", // Registration number present, but signs are null
            ofdProvider = null
        )
        val html = renderer.renderHtml(request, doc)
        assertTrue(!html.contains("Ссылка на чек"))
    }

    @Test
    fun testRenderHtmlWithMockedTaxes() {
        val mockTaxService = mockk<TaxCalculationService>()
        val renderer = ReceiptHtmlRenderer(StubQrCodeGenerator(), mockTaxService)

        val request = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0),
            idempotencyKey = "key-mocked-taxes"
        )

        val doc = FiscalDocumentSnapshot(
            id = "doc-789",
            cashboxId = "kkm-123",
            shiftId = "shift-456",
            docType = "TICKET",
            docNo = 12,
            shiftNo = 34,
            createdAt = 1782200000000L,
            totalAmount = 0L,
            currency = "KZT",
            fiscalSign = "FS-12345",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1782200010000L,
            receiptUrl = "https://kassa.kz/receipt/789",
            registrationNumber = "RN-999",
            taxpayerName = "IE Ivanov",
            ofdProvider = "KAZAKHTELECOM"
        )

        every {
            mockTaxService.calculateTicketTaxes(any(), any(), any(), any())
        } returns TaxCalculationService.TicketTaxResult(
            listOf(
                TaxCalculationService.TaxLine(VatGroup.NO_VAT, 0, Money(100, 0), Money(0, 0)),
                TaxCalculationService.TaxLine(VatGroup.VAT_0, 0, Money(200, 0), Money(0, 0)),
                TaxCalculationService.TaxLine(VatGroup.VAT_5, 5, Money(300, 0), Money(15, 0)),
                TaxCalculationService.TaxLine(VatGroup.VAT_10, 10, Money(400, 0), Money(40, 0)),
                TaxCalculationService.TaxLine(VatGroup.VAT_16, 16, Money(500, 0), Money(80, 0))
            )
        )

        val html = renderer.renderHtml(request, doc)
        assertTrue(html.contains("Без НДС / ҚҚС-сыз"))
        assertTrue(html.contains("НДС / ҚҚС 0%"))
        assertTrue(html.contains("НДС / ҚҚС 5%"))
        assertTrue(html.contains("НДС / ҚҚС 10%"))
        assertTrue(html.contains("НДС / ҚҚС 16%"))
    }
}
