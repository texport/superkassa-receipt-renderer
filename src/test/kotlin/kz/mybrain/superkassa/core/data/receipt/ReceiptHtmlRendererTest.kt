package kz.mybrain.superkassa.core.data.receipt

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.model.*
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

    private val defaultKkm = KkmInfo(
        id = "kkm-123",
        createdAt = 1782200000000L,
        updatedAt = 1782200000000L,
        mode = "PRODUCTION",
        state = "READY",
        registrationNumber = "RN-999",
        factoryNumber = "FN-888",
        branding = ReceiptBranding(),
        ofdServiceInfo = OfdServiceInfo(
            orgTitle = "Individual Entrepreneur Ivanov",
            orgAddress = "Almaty, Abay Ave 10",
            orgAddressKz = "Алматы, Абай даңғылы, 10",
            orgInn = "987654321012",
            orgOkved = "62010",
            geoLatitude = 432389,
            geoLongitude = 768897,
            geoSource = "GPS"
        )
    )

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
                    quantity = 2L,
                    price = Money(50, 0),
                    sum = Money(100, 0),
                    vatGroup = VatGroup.VAT_16,
                    measureUnitCode = "796" // Piece
                ),
                ReceiptItem(
                    name = "Milk",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(200, 50),
                    sum = Money(200, 50),
                    vatGroup = VatGroup.NO_VAT,
                    measureUnitCode = "166", // kg (custom short unit)
                    listExciseStamp = listOf("EXCISE-123", "EXCISE-456")
                ),
                ReceiptItem(
                    name = "Bread",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(100, 0),
                    sum = Money(100, 0),
                    vatGroup = VatGroup.VAT_0,
                    measureUnitCode = "999" // Unknown unit to cover default fallback
                ),
                ReceiptItem(
                    name = "Cheese",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(500, 0),
                    sum = Money(500, 0),
                    vatGroup = VatGroup.VAT_5
                ),
                ReceiptItem(
                    name = "Juice",
                    sectionCode = "001",
                    quantity = 1L,
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

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            factoryNumber = "FN-888",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgTitle = "Individual Entrepreneur Ivanov",
                orgInn = "987654321012",
                orgAddress = "Almaty, Abay Ave 10"
            )
        )

        val html = renderer.renderHtml(request, doc, kkm)
        println("=== DEBUG CHAR CODES ===")
        val target = "Получено / Алынды"
        println("Target string: '$target'")
        println("Target char codes: " + target.map { it.code })
        val index = html.indexOf("Получено")
        if (index >= 0) {
            val slice = html.substring(index, index + target.length)
            println("HTML slice: '$slice'")
            println("HTML slice char codes: " + slice.map { it.code })
        } else {
            println("Target 'Получено' not found in HTML!")
        }
        println("=== END DEBUG CHAR CODES ===")

        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("САТУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ПРОДАЖИ"))
        assertTrue(html.contains("<span class=\"lang-kk\">САТУ ЧЕГІ</span>"))
        assertTrue(cleanHtml.contains("12"))
        assertTrue(cleanHtml.contains("Apple"))
        assertTrue(cleanHtml.contains("дана / шт"))
        assertTrue(cleanHtml.contains("Milk"))
        assertTrue(cleanHtml.contains("Таңбалау: / Маркировка: EXCISE-123, EXCISE-456"))
        assertTrue(cleanHtml.contains("Қолма-қол / Наличные"))
        assertTrue(cleanHtml.contains("Карта"))
        assertTrue(cleanHtml.contains("Электронды / Электронно"))
        assertTrue(cleanHtml.contains("Алынды / Получено"))
        assertTrue(cleanHtml.contains("Қайтарым / Сдача"))
        assertTrue(cleanHtml.contains("БСН/ЖСН / БИН/ИИН: 987654321012"))
        assertTrue(cleanHtml.contains("Сатып алушы БСН/ЖСН: / Покупатель БИН/ИИН: 123456789012"))
        assertTrue(cleanHtml.contains("Individual Entrepreneur Ivanov"))
        assertTrue(cleanHtml.contains("Almaty, Abay Ave 10"))
        assertTrue(cleanHtml.contains("ЗНМ"))
        assertTrue(cleanHtml.contains("FN-888"))
        assertTrue(cleanHtml.contains("«Қазақтелеком» АҚ / АО «Казахтелеком»"))
        assertTrue(cleanHtml.contains("oofd.kz"))
        assertTrue(html.contains("data:image/png;base64,stub-qr-code-for-https://kassa.kz/receipt/789"))
        assertTrue(cleanHtml.contains("Жіберілді / Отправлен"))
        // Check for VAT Groups (should be empty since defaultVatGroup is NO_VAT under VAT_PAYER)
        assertTrue(!cleanHtml.contains("Налоги / Салықтар"))
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
            docNo = 108L,
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
            taxpayerBin = "987654321012",
            taxpayerAddress = null,
            factoryNumber = null,
            ofdProvider = "TRANSTELECOM"
        )

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgInn = "987654321012"
            )
        )

        val html = renderer.renderHtml(request, doc, kkm)

        val cleanHtml = stripHtml(html)

        assertTrue(cleanHtml.contains("САТУДЫ ҚАЙТАРУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ВОЗВРАТА ПРОДАЖИ"))
        assertTrue(cleanHtml.contains("Автономды / Офлайн"))
        assertTrue(cleanHtml.contains("Автономды режим / Автономный режим"))
        assertTrue(cleanHtml.contains("«Транстелеком» АҚ / АО «Транстелеком»"))
        assertTrue(cleanHtml.contains("o.oofd.kz"))
        // Check fallback url in qr code
        assertTrue(html.contains("stub-qr-code-for-https://o.oofd.kz"))
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

        val kkm = defaultKkm.copy(registrationNumber = "RN-999")

        val html = renderer.renderHtml(request, doc, kkm)
        println("=== DEBUG OTHER STATUSES CHAR CODES ===")
        val target = "ЧЕК ПОКУПКИ / САТЫП АЛУ ЧЕГІ"
        println("Target string: '$target'")
        println("Target char codes: " + target.map { it.code })
        val index = html.indexOf("ЧЕК ПОКУПКИ")
        if (index >= 0) {
            val slice = html.substring(index, index + target.length)
            println("HTML slice: '$slice'")
            println("HTML slice char codes: " + slice.map { it.code })
        } else {
            println("Target 'ЧЕК ПОКУПКИ' not found in HTML!")
        }
        println("=== END DEBUG OTHER STATUSES CHAR CODES ===")

        val cleanHtml = stripHtml(html)

        assertTrue(cleanHtml.contains("САТЫП АЛУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ПОКУПКИ"))
        assertTrue(cleanHtml.contains("UNKNOWN_STATUS"))
        assertTrue(cleanHtml.contains("«Alteco Partners» ЖШС / ТОО «Alteco Partners»"))
        assertTrue(cleanHtml.contains("alteco.kz"))
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

        val kkm = defaultKkm.copy(registrationNumber = "RN-999")

        val html = renderer.renderHtml(request, doc, kkm)

        val cleanHtml = stripHtml(html)

        assertTrue(cleanHtml.contains("САТЫП АЛУДЫ ҚАЙТАРУ ЧЕГІ"))
        assertTrue(cleanHtml.contains("ЧЕК ВОЗВРАТА ПОКУПКИ"))
        assertTrue(cleanHtml.contains("SOME_OTHER_PROVIDER"))
        assertTrue(cleanHtml.contains("consumer.oofd.kz"))
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

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            factoryNumber = "FN-888"
        )

        val xReport = renderer.renderXReportHtml(shift, counters, kkm, null)
        val cleanXReport = stripHtml(xReport)
        assertTrue(cleanXReport.contains("X-ОТЧЁТ"))
        assertTrue(cleanXReport.contains("Ауысым № / Смена №"))
        assertTrue(cleanXReport.contains("45"))

        val openShift = renderer.renderOpenShiftHtml(shift, kkm, null)
        val cleanOpenShift = stripHtml(openShift)
        assertTrue(cleanOpenShift.contains("ОТКРЫТИЕ СМЕНЫ"))

        val closeShift = renderer.renderCloseShiftHtml(shift, counters, kkm, null)
        val cleanCloseShift = stripHtml(closeShift)
        assertTrue(cleanCloseShift.contains("Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)"))

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

        val cashOp = renderer.renderCashOperationHtml(doc, kkm)
        val cleanCashOp = stripHtml(cashOp)
        assertTrue(cleanCashOp.contains("ВНЕСЕНИЕ НАЛИЧНЫХ"))
        assertTrue(cleanCashOp.contains("500.00") || cleanCashOp.contains("500,00"))
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
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = null,
            deliveredAt = null,
            receiptUrl = null,
            registrationNumber = null,
            ofdProvider = null
        )

        val kkm = defaultKkm.copy(
            registrationNumber = null,
            ofdServiceInfo = null
        )

        val html = renderer.renderHtml(request, doc, kkm)

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
        val kkm = defaultKkm.copy(registrationNumber = "RN-999")
        val html = renderer.renderHtml(request, doc, kkm)
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

        val kkm = defaultKkm.copy(
            registrationNumber = "RN-999",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgTitle = "IE Ivanov"
            )
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

        val html = renderer.renderHtml(request, doc, kkm)
        val cleanHtml = stripHtml(html)
        assertTrue(cleanHtml.contains("ҚҚС-сыз / Без НДС"))
        assertTrue(cleanHtml.contains("ҚҚС 0% / НДС 0%"))
        assertTrue(cleanHtml.contains("ҚҚС 5% / НДС 5%"))
        assertTrue(cleanHtml.contains("ҚҚС 10% / НДС 10%"))
        assertTrue(cleanHtml.contains("ҚҚС 16% / НДС 16%"))
    }

    @Test
    fun testGenerateExampleHtmlFiles() {
        val renderer = ReceiptHtmlRenderer(ZxingQrCodeGenerator())
        val rootOutputDir = java.io.File("../superkassa/receipt-examples")
        if (rootOutputDir.exists()) {
            rootOutputDir.deleteRecursively()
        }
        rootOutputDir.mkdirs()

        val baseKkm = defaultKkm.copy(
            registrationNumber = "RN-999888777",
            factoryNumber = "FN-777666",
            ofdServiceInfo = defaultKkm.ofdServiceInfo?.copy(
                orgTitle = "ИП Иванов С. П.",
                orgInn = "920102300400",
                orgAddress = "г. Алматы, ул. Толе Би, 50"
            )
        )

        // 1. Standard Sale Receipt data
        val request1 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Хлеб бородинский", "001", 1L, Money(180, 0), Money(180, 0)),
                ReceiptItem("Молоко 3.2%", "001", 2L, Money(450, 0), Money(900, 0), listExciseStamp = listOf("01046002938172"))
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

        // 2. Mixed Payment, MIXED VAT, whole receipt discount and markup (no item discounts)
        val request2 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy (SM-A356B) / Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(149990, 0),
                    sum = Money(149990, 0),
                    vatGroup = VatGroup.VAT_16
                ),
                ReceiptItem(
                    name = "Защитное стекло",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(5000, 0),
                    sum = Money(5000, 0),
                    vatGroup = VatGroup.NO_VAT
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CARD, Money(100000, 0)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(50000, 0))
            ),
            total = Money(150000, 0),
            idempotencyKey = "key-ex2",
            taxRegime = TaxRegime.MIXED,
            defaultVatGroup = VatGroup.VAT_16,
            discount = Money(5000, 0),
            markup = Money(10, 0)
        )
        val doc2 = doc1.copy(id = "doc-ex2", docNo = 102, totalAmount = 15000000L)

        // 2b. Mixed Payment, MIXED VAT, ONLY item-level discounts (receipt discount/markup are null)
        val request2ItemDiscountOnly = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem(
                    name = "Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy (SM-A356B) / Смартфон Samsung Galaxy A35 5G 8GB/256GB Awesome Navy",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(149990, 0),
                    sum = Money(149990, 0),
                    vatGroup = VatGroup.VAT_16,
                    discount = Money(1000, 0),
                    markup = Money(200, 0)
                ),
                ReceiptItem(
                    name = "Защитное стекло",
                    sectionCode = "001",
                    quantity = 1L,
                    price = Money(5000, 0),
                    sum = Money(5000, 0),
                    vatGroup = VatGroup.NO_VAT
                )
            ),
            payments = listOf(
                ReceiptPayment(PaymentType.CARD, Money(100000, 0)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(50000, 0))
            ),
            total = Money(150000, 0),
            idempotencyKey = "key-ex2-item-disc-only",
            taxRegime = TaxRegime.MIXED,
            defaultVatGroup = VatGroup.VAT_16,
            discount = null,
            markup = null
        )

        // 3. Refund Sale Receipt (SELL_RETURN)
        val request3 = request1.copy(
            operation = ReceiptOperationType.SELL_RETURN,
            idempotencyKey = "key-ex3",
            parentTicket = ParentTicket(
                parentTicketNumber = 101L,
                parentTicketDateTimeMillis = 1782200000000L,
                kgdKkmId = "kkm-123",
                parentTicketTotal = Money(1080, 0),
                parentTicketIsOffline = false
            )
        )
        val doc3 = doc1.copy(id = "doc-ex3", docNo = 103)

        // 4. Purchase Receipt (BUY)
        val request4 = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.BUY,
            items = listOf(
                ReceiptItem("Прием металлолома", "001", 10L, Money(100, 0), Money(1000, 0))
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "key-ex4"
        )
        val doc4 = doc1.copy(id = "doc-ex4", docNo = 104, totalAmount = 100000L)

        // 5. Refund Purchase Receipt (BUY_RETURN)
        val request5 = request4.copy(
            operation = ReceiptOperationType.BUY_RETURN,
            idempotencyKey = "key-ex5",
            parentTicket = ParentTicket(
                parentTicketNumber = 104L,
                parentTicketDateTimeMillis = 1782200000000L,
                kgdKkmId = "kkm-123",
                parentTicketTotal = Money(1000, 0),
                parentTicketIsOffline = false
            )
        )
        val doc5 = doc1.copy(id = "doc-ex5", docNo = 105)

        // 5b. Sale Receipt with All VAT rates (NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16)
        val requestAllVats = ReceiptRequest(
            kkmId = "kkm-123",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                ReceiptItem("Товар без НДС", "001", 1L, Money(1000, 0), Money(1000, 0), vatGroup = VatGroup.NO_VAT),
                ReceiptItem("Товар НДС 0%", "001", 1L, Money(2000, 0), Money(2000, 0), vatGroup = VatGroup.VAT_0),
                ReceiptItem("Товар НДС 5%", "001", 1L, Money(3000, 0), Money(3000, 0), vatGroup = VatGroup.VAT_5),
                ReceiptItem("Товар НДС 10%", "001", 1L, Money(4000, 0), Money(4000, 0), vatGroup = VatGroup.VAT_10),
                ReceiptItem("Товар НДС 16%", "001", 1L, Money(5000, 0), Money(5000, 0), vatGroup = VatGroup.VAT_16)
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(15000, 0))),
            total = Money(15000, 0),
            idempotencyKey = "key-ex-all-vats"
        )
        val docAllVats = doc1.copy(id = "doc-ex-all-vats", docNo = 110, totalAmount = 1500000L)

        // 6. Open Shift data
        val shiftOpen = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-123",
            shiftNo = 5,
            status = ShiftStatus.OPEN,
            openedAt = 1782100000000L,
            closedAt = null
        )

        // Z-Report (Close Shift) data
        val shiftClose = shiftOpen.copy(
            status = ShiftStatus.CLOSED,
            closedAt = 1782200000000L
        )

        // 7. X-Report & Z-Report counters
        val counters = mapOf(
            "operation.OPERATION_SELL.count" to 15L,
            "operation.OPERATION_SELL.sum" to 25000000L, // 250,000.00
            "operation.OPERATION_SELL_RETURN.count" to 1L,
            "operation.OPERATION_SELL_RETURN.sum" to 500000L, // 5,000.00
            "operation.OPERATION_BUY.count" to 2L,
            "operation.OPERATION_BUY.sum" to 200000L, // 2,000.00
            "operation.OPERATION_BUY_RETURN.count" to 1L,
            "operation.OPERATION_BUY_RETURN.sum" to 100000L, // 1,000.00

            "operation.OPERATION_SELL.discount_sum" to 500000L,
            "operation.OPERATION_SELL.markup_sum" to 1000L,

            "section.1.operation.OPERATION_SELL.count" to 10L,
            "section.1.operation.OPERATION_SELL.sum" to 15000000L,
            "section.2.operation.OPERATION_SELL.count" to 5L,
            "section.2.operation.OPERATION_SELL.sum" to 10000000L,

            "ticket.OPERATION_SELL.total_count" to 15L,
            "ticket.OPERATION_SELL.count" to 15L,
            "ticket.OPERATION_SELL.sum" to 24501000L,
            "ticket.OPERATION_SELL.offline_count" to 2L,
            "ticket.OPERATION_SELL.discount_sum" to 500000L,
            "ticket.OPERATION_SELL.markup_sum" to 1000L,
            "ticket.OPERATION_SELL.change_sum" to 20000L,

            "ticket.OPERATION_SELL_RETURN.total_count" to 1L,
            "ticket.OPERATION_SELL_RETURN.count" to 1L,
            "ticket.OPERATION_SELL_RETURN.sum" to 500000L,

            "ticket.OPERATION_SELL.payment.PAYMENT_CASH.sum" to 14501000L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CASH.count" to 10L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CARD.sum" to 10000000L,
            "ticket.OPERATION_SELL.payment.PAYMENT_CARD.count" to 5L,

            "ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CASH.sum" to 500000L,
            "ticket.OPERATION_SELL_RETURN.payment.PAYMENT_CASH.count" to 1L,

            "tax.VAT_16.OPERATION_SELL.turnover" to 14999000L,
            "tax.VAT_16.OPERATION_SELL.turnover_without_tax" to 12930173L,
            "tax.VAT_16.OPERATION_SELL.sum" to 2068827L,

            "tax.VAT_0.OPERATION_SELL.turnover" to 1000000L,
            "tax.VAT_0.OPERATION_SELL.turnover_without_tax" to 1000000L,
            "tax.VAT_0.OPERATION_SELL.sum" to 0L,

            "money_placement.MONEY_PLACEMENT_DEPOSIT.count" to 2L,
            "money_placement.MONEY_PLACEMENT_DEPOSIT.total_count" to 2L,
            "money_placement.MONEY_PLACEMENT_DEPOSIT.sum" to 5000000L,
            "money_placement.MONEY_PLACEMENT_WITHDRAWAL.count" to 1L,
            "money_placement.MONEY_PLACEMENT_WITHDRAWAL.total_count" to 1L,
            "money_placement.MONEY_PLACEMENT_WITHDRAWAL.sum" to 3000000L,

            "start_shift_non_nullable.OPERATION_SELL.sum" to 1000000000L,
            "non_nullable.OPERATION_SELL.sum" to 1025001000L,

            "start_shift_non_nullable.OPERATION_SELL_RETURN.sum" to 50000000L,
            "non_nullable.OPERATION_SELL_RETURN.sum" to 50500000L,

            "cash.sum" to 26501000L,
            "revenue.sum" to 24001000L
        )

        // 9. Cash In data
        val docCashIn = doc1.copy(
            id = "doc-cash-in",
            docType = "CASH_IN",
            docNo = 106,
            totalAmount = 5000000L
        )

        // 10. Cash Out data
        val docCashOut = doc1.copy(
            id = "doc-cash-out",
            docType = "CASH_OUT",
            docNo = 107,
            totalAmount = 3000000L
        )

        val widths = listOf(80, 58)
        val themes = listOf(false, true) // false = light, true = dark

        for (width in widths) {
            for (isDark in themes) {
                val themeName = if (isDark) "dark" else "light"
                val dir = java.io.File(rootOutputDir, "${width}mm/$themeName")
                
                val ticketsDir = java.io.File(dir, "tickets")
                val reportsDir = java.io.File(dir, "reports")
                val operationsDir = java.io.File(dir, "operations")
                
                ticketsDir.mkdirs()
                reportsDir.mkdirs()
                operationsDir.mkdirs()

                val themeColor = when {
                    width == 80 && !isDark -> "teal"
                    width == 80 && isDark -> "green"
                    width == 58 && !isDark -> "blue"
                    else -> "orange"
                }
                val css = if (isDark) "/* force-dark */" else ""
                val folderBranding = ReceiptBranding(
                    paperWidthMm = width,
                    customCss = css,
                    themeColor = themeColor
                )
                val kkmForFolder = baseKkm.copy(branding = folderBranding)

                // 1. Standard Sale Receipt
                ticketsDir.resolve("sale_receipt.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder)
                )

                // 2. Mixed Payment, VAT 16%, discounts
                ticketsDir.resolve("sale_with_vat_and_discounts.html").writeText(
                    renderer.renderHtml(request2, doc2, kkmForFolder)
                )

                // 2b. Mixed Payment, MIXED VAT, ONLY item-level discounts
                ticketsDir.resolve("sale_with_only_item_discounts.html").writeText(
                    renderer.renderHtml(request2ItemDiscountOnly, doc2, kkmForFolder)
                )

                // 3. Refund Sale Receipt
                ticketsDir.resolve("refund_sale_receipt.html").writeText(
                    renderer.renderHtml(request3, doc3, kkmForFolder)
                )

                // 4. Purchase Receipt
                ticketsDir.resolve("buy_receipt.html").writeText(
                    renderer.renderHtml(request4, doc4, kkmForFolder)
                )

                // 5. Refund Purchase Receipt
                ticketsDir.resolve("refund_buy_receipt.html").writeText(
                    renderer.renderHtml(request5, doc5, kkmForFolder)
                )

                // 6. Open Shift
                reportsDir.resolve("open_shift.html").writeText(
                    renderer.renderOpenShiftHtml(shiftOpen, kkmForFolder, null)
                )

                // 7. X-Report
                reportsDir.resolve("x_report.html").writeText(
                    renderer.renderXReportHtml(shiftOpen, counters, kkmForFolder, null)
                )

                // 7a. Russian-only X-Report
                reportsDir.resolve("x_report_russian.html").writeText(
                    renderer.renderXReportHtml(shiftOpen, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)), null)
                )

                // 7b. Kazakh-only X-Report
                reportsDir.resolve("x_report_kazakh.html").writeText(
                    renderer.renderXReportHtml(shiftOpen, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)), null)
                )

                // 8. Z-Report
                reportsDir.resolve("z_report.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder, null)
                )

                // 8a. Russian-only Z-Report
                reportsDir.resolve("z_report_russian.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)), null)
                )

                // 8b. Kazakh-only Z-Report
                reportsDir.resolve("z_report_kazakh.html").writeText(
                    renderer.renderCloseShiftHtml(shiftClose, counters, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)), null)
                )

                // 9. Cash In
                operationsDir.resolve("cash_in.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn, kkmForFolder)
                )

                // 10. Cash Out
                operationsDir.resolve("cash_out.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut, kkmForFolder)
                )

                // 11. Kazakh-only Sale Receipt
                val brandingKk = folderBranding.copy(language = ReceiptLanguage.KK)
                ticketsDir.resolve("sale_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingKk))
                )

                // 12. Russian-only Sale Receipt
                val brandingRu = folderBranding.copy(language = ReceiptLanguage.RU)
                ticketsDir.resolve("sale_receipt_russian.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingRu))
                )

                // 13. Branded Sale Receipt
                val brandingBranded = folderBranding.copy(
                    language = ReceiptLanguage.MIXED,
                    headerLogoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop",
                    headerHtml = "<div class='center' style='padding: 5px; background: #ffebeb; color: #cc0000; border-radius: 4px; font-weight: bold;'>СУПЕР АКЦИЯ: -20% НА ВСЁ!</div>",
                    footerHtml = "<div class='center' style='margin-top: 10px; color: #666; font-size: 11px;'>Спасибо, что выбрали нас! / Бізді таңдағаныңызға рақмет!</div>",
                    customCss = if (isDark) {
                        "/* force-dark */\nbody { background-color: #1e1d20; } .receipt-card { border-top: 5px solid #ffb833; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.5); }"
                    } else {
                        "body { background-color: #faf8f5; } .receipt-card { border-top: 5px solid #d97706; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }"
                    }
                )
                ticketsDir.resolve("sale_receipt_branded.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingBranded))
                )

                // 13b. Fully Branded Sale Receipt
                val brandingFullyBranded = folderBranding.copy(
                    language = ReceiptLanguage.MIXED,
                    themeColor = "rose",
                    headerLogoUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop",
                    beforeHeaderHtml = "<div class='center' style='color: #ff0000; font-weight: bold; padding: 4px; border: 1px solid red; border-radius: 4px;'>!!! BEFORE HEADER BLOCK !!!</div>",
                    headerHtml = "<div class='center' style='background: #ffebeb; padding: 4px;'>HEADER HTML</div>",
                    afterHeaderHtml = "<div class='center' style='color: #00aa00; font-size: 11px;'>AFTER HEADER (BEFORE TITLE)</div>",
                    beforeItemsHtml = "<div style='border: 1px solid #ccc; padding: 6px; border-radius: 8px; background: #fafafa; color: #555;'>BEFORE ITEMS CONTENT</div>",
                    afterItemsHtml = "<div style='border: 1px dashed #aaa; padding: 6px; border-radius: 8px; background: #fdfdfd;'>AFTER ITEMS INFO</div>",
                    beforeTotalsHtml = "<div style='color: #777; font-size: 11px; padding: 2px;'>BEFORE TOTALS MSG</div>",
                    afterTotalsHtml = "<div style='background: #eef; padding: 6px; border-radius: 8px; font-weight: 500;'>AFTER TOTALS / TAXES BANNER</div>",
                    beforeQrHtml = "<div class='center' style='font-style: italic; font-size: 11px; color: #4f46e5;'>BEFORE QR PROMO</div>",
                    footerHtml = "<div class='center' style='font-size: 10px; color: #999;'>FOOTER THANK YOU</div>"
                )
                ticketsDir.resolve("sale_receipt_fully_branded.html").writeText(
                    renderer.renderHtml(request1, doc1, kkmForFolder.copy(branding = brandingFullyBranded))
                )

                // 14. Offline Sale Receipt
                val docOffline = doc1.copy(id = "doc-ex-offline", docNo = 108, ofdStatus = "PENDING")
                ticketsDir.resolve("sale_receipt_offline.html").writeText(
                    renderer.renderHtml(request1, docOffline, kkmForFolder)
                )

                // 15. Failed/Error sending to OFD Sale Receipt
                val docFailed = doc1.copy(id = "doc-ex-failed", docNo = 109, ofdStatus = "ERROR")
                ticketsDir.resolve("sale_receipt_failed.html").writeText(
                    renderer.renderHtml(request1, docFailed, kkmForFolder)
                )

                // 16. Visual Branding Preview
                ticketsDir.resolve("sale_receipt_preview.html").writeText(
                    renderer.renderPreviewHtml(brandingBranded)
                )

                // 17. Sale Receipt with All VAT rates (MIXED, RU, KK)
                ticketsDir.resolve("sale_with_all_vat_rates.html").writeText(
                    renderer.renderHtml(requestAllVats, docAllVats, kkmForFolder)
                )
                ticketsDir.resolve("sale_with_all_vat_rates_russian.html").writeText(
                    renderer.renderHtml(requestAllVats, docAllVats, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                ticketsDir.resolve("sale_with_all_vat_rates_kazakh.html").writeText(
                    renderer.renderHtml(requestAllVats, docAllVats, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                // Additional RU/KK matrix files for completeness
                ticketsDir.resolve("refund_sale_receipt_russian.html").writeText(
                    renderer.renderHtml(request3, doc3, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                ticketsDir.resolve("refund_sale_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request3, doc3, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                ticketsDir.resolve("buy_receipt_russian.html").writeText(
                    renderer.renderHtml(request4, doc4, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                ticketsDir.resolve("buy_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request4, doc4, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                ticketsDir.resolve("refund_buy_receipt_russian.html").writeText(
                    renderer.renderHtml(request5, doc5, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                ticketsDir.resolve("refund_buy_receipt_kazakh.html").writeText(
                    renderer.renderHtml(request5, doc5, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                reportsDir.resolve("open_shift_russian.html").writeText(
                    renderer.renderOpenShiftHtml(shiftOpen, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)), null)
                )
                reportsDir.resolve("open_shift_kazakh.html").writeText(
                    renderer.renderOpenShiftHtml(shiftOpen, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)), null)
                )

                operationsDir.resolve("cash_in_russian.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                operationsDir.resolve("cash_in_kazakh.html").writeText(
                    renderer.renderCashOperationHtml(docCashIn, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )

                operationsDir.resolve("cash_out_russian.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.RU)))
                )
                operationsDir.resolve("cash_out_kazakh.html").writeText(
                    renderer.renderCashOperationHtml(docCashOut, kkmForFolder.copy(branding = folderBranding.copy(language = ReceiptLanguage.KK)))
                )
            }
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }
}
