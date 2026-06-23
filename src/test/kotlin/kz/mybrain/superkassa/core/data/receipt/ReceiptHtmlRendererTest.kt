package kz.mybrain.superkassa.core.data.receipt

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.ReceiptLanguage
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

    private class ZxingQrCodeGenerator : QrCodeGeneratorPort {
        override fun generatePngDataUri(text: String, sizePx: Int): String? {
            if (text.isBlank()) return null
            return try {
                val hints = mapOf(
                    com.google.zxing.EncodeHintType.MARGIN to 1,
                    com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
                )
                val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
                val image = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(matrix)
                val baos = java.io.ByteArrayOutputStream()
                javax.imageio.ImageIO.write(image, "PNG", baos)
                "data:image/png;base64,${java.util.Base64.getEncoder().encodeToString(baos.toByteArray())}"
            } catch (e: Exception) {
                null
            }
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
        assertTrue(xReport.contains("Смена № / Ауысым №"))
        assertTrue(xReport.contains("45"))

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
        assertTrue(cashOp.contains("500.00") || cashOp.contains("500,00"))
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
        assertTrue(html.contains("НДС 0% / ҚҚС 0%"))
        assertTrue(html.contains("НДС 5% / ҚҚС 5%"))
        assertTrue(html.contains("НДС 10% / ҚҚС 10%"))
        assertTrue(html.contains("НДС 16% / ҚҚС 16%"))
    }

    @Test
    fun testGenerateExampleHtmlFiles() {
        val renderer = ReceiptHtmlRenderer(ZxingQrCodeGenerator())
        val outputDir = java.io.File("../superkassa/receipt-examples")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // 1. Standard Sale Receipt
        val request1 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Хлеб бородинский", "001", 1, Money(180, 0), Money(180, 0)),
                ReceiptItem("Молоко 3.2%", "001", 2, Money(450, 0), Money(900, 0), listExciseStamp = listOf("01046002938172"))
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1100, 0))),
            total = Money(1080, 0),
            taken = Money(1100, 0),
            change = Money(20, 0),
            idempotencyKey = "key-ex1"
        )
        val doc1 = FiscalDocumentSnapshot(
            id = "doc-ex1",
            cashboxId = "kkm-123",
            shiftId = "shift-1",
            docType = "TICKET",
            docNo = 101,
            shiftNo = 5,
            createdAt = 1782200000000L,
            totalAmount = 108000L,
            currency = "KZT",
            fiscalSign = "1234567890",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = null,
            receiptUrl = "https://consumer.oofd.kz/r/101",
            registrationNumber = "RN-999888777",
            taxpayerName = "ИП Иванов С. П.",
            taxpayerBin = "920102300400",
            taxpayerAddress = "г. Алматы, ул. Толе Би, 50",
            factoryNumber = "FN-777666",
            ofdProvider = "KAZAKHTELECOM"
        )
        java.io.File(outputDir, "sale_receipt.html").writeText(renderer.renderHtml(request1, doc1))

        // 2. Mixed Payment, VAT 16%, whole receipt discount and markup
        val request2 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Телефон Samsung A35",
                    sectionCode = "001",
                    quantity = 1,
                    price = Money(149990, 0),
                    sum = Money(149990, 0),
                    vatGroup = VatGroup.VAT_16,
                    discount = Money(1000, 0),
                    markup = Money(200, 0)
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CARD, Money(100000, 0)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(45000, 0))
            ),
            total = Money(145000, 0),
            idempotencyKey = "key-ex2",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16,
            discount = Money(5000, 0),
            markup = Money(10, 0)
        )
        val doc2 = doc1.copy(id = "doc-ex2", docNo = 102, totalAmount = 14500000L)
        java.io.File(outputDir, "sale_with_vat_and_discounts.html").writeText(renderer.renderHtml(request2, doc2))

        // 3. Refund Sale Receipt (SELL_RETURN)
        val request3 = request1.copy(
            operation = ReceiptOperationType.SELL_RETURN,
            idempotencyKey = "key-ex3"
        )
        val doc3 = doc1.copy(id = "doc-ex3", docNo = 103)
        java.io.File(outputDir, "refund_sale_receipt.html").writeText(renderer.renderHtml(request3, doc3))

        // 4. Purchase Receipt (BUY)
        val request4 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = listOf(
                ReceiptItem("Прием металлолома", "001", 10, Money(100, 0), Money(1000, 0))
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "key-ex4"
        )
        val doc4 = doc1.copy(id = "doc-ex4", docNo = 104, totalAmount = 100000L)
        java.io.File(outputDir, "buy_receipt.html").writeText(renderer.renderHtml(request4, doc4))

        // 5. Refund Purchase Receipt (BUY_RETURN)
        val request5 = request4.copy(
            operation = ReceiptOperationType.BUY_RETURN,
            idempotencyKey = "key-ex5"
        )
        val doc5 = doc1.copy(id = "doc-ex5", docNo = 105)
        java.io.File(outputDir, "refund_buy_receipt.html").writeText(renderer.renderHtml(request5, doc5))

        // 6. Open Shift
        val shiftOpen = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-123",
            shiftNo = 5,
            status = ShiftStatus.OPEN,
            openedAt = 1782100000000L,
            closedAt = null
        )
        java.io.File(outputDir, "open_shift.html").writeText(renderer.renderOpenShiftHtml(shiftOpen))

        // 7. X-Report & Z-Report counters
        val counters = mapOf(
            // 1) Operation totals
            "operation.OPERATION_SELL.count" to 15L,
            "operation.OPERATION_SELL.sum" to 25000000L, // 250,000.00
            "operation.OPERATION_SELL_RETURN.count" to 1L,
            "operation.OPERATION_SELL_RETURN.sum" to 500000L, // 5,000.00
            "operation.OPERATION_BUY.count" to 2L,
            "operation.OPERATION_BUY.sum" to 200000L, // 2,000.00
            "operation.OPERATION_BUY_RETURN.count" to 1L,
            "operation.OPERATION_BUY_RETURN.sum" to 100000L, // 1,000.00

            // 2) Discounts & Markups
            "operation.OPERATION_SELL.discount_sum" to 500000L, // 5,000.00
            "operation.OPERATION_SELL.markup_sum" to 1000L, // 10.00

            // 3) Section / Department totals
            "section.1.operation.OPERATION_SELL.count" to 10L,
            "section.1.operation.OPERATION_SELL.sum" to 15000000L,
            "section.2.operation.OPERATION_SELL.count" to 5L,
            "section.2.operation.OPERATION_SELL.sum" to 10000000L,

            // 4) Ticket level counts, sums, change, discount
            "ticket.OPERATION_SELL.total_count" to 15L,
            "ticket.OPERATION_SELL.count" to 15L,
            "ticket.OPERATION_SELL.sum" to 24501000L, // 250000 - 5000 + 10 = 245010.00
            "ticket.OPERATION_SELL.offline_count" to 2L,
            "ticket.OPERATION_SELL.discount_sum" to 500000L,
            "ticket.OPERATION_SELL.markup_sum" to 1000L,
            "ticket.OPERATION_SELL.change_sum" to 20000L,

            "ticket.OPERATION_SELL_RETURN.total_count" to 1L,
            "ticket.OPERATION_SELL_RETURN.count" to 1L,
            "ticket.OPERATION_SELL_RETURN.sum" to 500000L,

            // 5) Payments details
            "ticket.OPERATION_SELL.payment.PAYMENT_CASH.sum" to 14501000L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CASH.count" to 10L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CARD.sum" to 10000000L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CARD.count" to 5L,

            "ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CASH.sum" to 500000L,
            "ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CASH.count" to 1L,

            // 6) Taxes (turnover, turnover_without_tax, sum)
            "tax.VAT_16.OPERATION_SELL.turnover" to 14999000L,
            "tax.VAT_16.OPERATION_SELL.turnover_without_tax" to 12930173L,
            "tax.VAT_16.OPERATION_SELL.sum" to 2068827L,

            "tax.VAT_0.OPERATION_SELL.turnover" to 1000000L,
            "tax.VAT_0.OPERATION_SELL.turnover_without_tax" to 1000000L,
            "tax.VAT_0.OPERATION_SELL.sum" to 0L,

            // 7) Money Placement (Cash In / Out)
            "money_placement.PLACEMENT_CASH_IN.count" to 2L,
            "money_placement.PLACEMENT_CASH_IN.total_count" to 2L,
            "money_placement.PLACEMENT_CASH_IN.sum" to 5000000L, // 50,000.00
            "money_placement.PLACEMENT_CASH_OUT.count" to 1L,
            "money_placement.PLACEMENT_CASH_OUT.total_count" to 1L,
            "money_placement.PLACEMENT_CASH_OUT.sum" to 3000000L, // 30,000.00

            // 8) Non-nullable sums (Start Shift and End of Shift)
            "start_shift_non_nullable.OPERATION_SELL.sum" to 1000000000L, // 10,000,000.00
            "non_nullable.OPERATION_SELL.sum" to 1025001000L,

            "start_shift_non_nullable.OPERATION_SELL_RETURN.sum" to 50000000L, // 500,000.00
            "non_nullable.OPERATION_SELL_RETURN.sum" to 50500000L,

            // 9) Cash sum & Revenue sum
            "cash.sum" to 26501000L, // 265,010.00
            "revenue.sum" to 24001000L // 240,010.00
        )

        // 7. X-Report
        java.io.File(outputDir, "x_report.html").writeText(renderer.renderXReportHtml(shiftOpen, counters))

        // 8. Z-Report (Close Shift)
        val shiftClose = shiftOpen.copy(
            status = ShiftStatus.CLOSED,
            closedAt = 1782200000000L
        )
        java.io.File(outputDir, "z_report.html").writeText(renderer.renderCloseShiftHtml(shiftClose, counters))

        // 9. Cash In
        val docCashIn = doc1.copy(
            id = "doc-cash-in",
            docType = "CASH_IN",
            docNo = 106,
            totalAmount = 5000000L // 50,000.00
        )
        java.io.File(outputDir, "cash_in.html").writeText(renderer.renderCashOperationHtml(docCashIn))

        // 10. Cash Out
        val docCashOut = doc1.copy(
            id = "doc-cash-out",
            docType = "CASH_OUT",
            docNo = 107,
            totalAmount = 3000000L // 30,000.00
        )
        java.io.File(outputDir, "cash_out.html").writeText(renderer.renderCashOperationHtml(docCashOut))

        // 11. Kazakh-only Sale Receipt
        val configKk = ReceiptBranding(language = ReceiptLanguage.KK)
        java.io.File(outputDir, "sale_receipt_kazakh.html").writeText(renderer.renderHtml(request1, doc1, configKk))

        // 12. Russian-only Sale Receipt
        val configRu = ReceiptBranding(language = ReceiptLanguage.RU)
        java.io.File(outputDir, "sale_receipt_russian.html").writeText(renderer.renderHtml(request1, doc1, configRu))

        // 13. Branded Sale Receipt
        val configBranded = ReceiptBranding(
            language = ReceiptLanguage.MIXED,
            headerLogoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop",
            headerHtml = "<div class='center' style='padding: 5px; background: #ffebeb; color: #cc0000; border-radius: 4px; font-weight: bold;'>СУПЕР АКЦИЯ: -20% НА ВСЁ!</div>",
            footerHtml = "<div class='center' style='margin-top: 10px; color: #666; font-size: 11px;'>Спасибо, что выбрали нас! / Бізді таңдағаныңызға рақмет!</div>",
            customCss = "body { background-color: #faf8f5; } .receipt-card { border-top: 5px solid #d97706; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }"
        )
        java.io.File(outputDir, "sale_receipt_branded.html").writeText(renderer.renderHtml(request1, doc1, configBranded))
    }
}
