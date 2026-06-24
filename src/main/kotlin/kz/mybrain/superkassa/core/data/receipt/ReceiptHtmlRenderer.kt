package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.data.receipt.renderer.*
import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort

import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService

class ReceiptHtmlRenderer(
    private val qrCodeGenerator: QrCodeGeneratorPort,
    private val taxCalculationService: TaxCalculationService = TaxCalculationService(),
    private val ofdProviders: Map<String, OfdProviderConfig> = defaultOfdProviders
) : ReceiptRenderPort {

    companion object {
        val defaultOfdProviders = mapOf(
            "KAZAKHTELECOM" to OfdProviderConfig(
                nameRu = "АО «Казахтелеком»",
                nameKk = "«Қазақтелеком» АҚ",
                website = "oofd.kz",
                checkDomain = "consumer.oofd.kz"
            ),
            "TRANSTELECOM" to OfdProviderConfig(
                nameRu = "АО «Транстелеком»",
                nameKk = "«Транстелеком» АҚ",
                website = "o.oofd.kz",
                checkDomain = "o.oofd.kz"
            ),
            "ALTECO" to OfdProviderConfig(
                nameRu = "ТОО «Alteco Partners»",
                nameKk = "«Alteco Partners» ЖШС",
                website = "alteco.kz",
                checkDomain = "alteco.kz"
            )
        )
    }

    private val saleRenderer = SaleReceiptRenderer(qrCodeGenerator, taxCalculationService, ofdProviders)
    private val xReportRenderer = XReportRenderer()
    private val zReportRenderer = ZReportRenderer()
    private val openShiftRenderer = OpenShiftRenderer()
    private val cashOperationRenderer = CashOperationRenderer()

    override fun renderHtml(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo
    ): String {
        return saleRenderer.render(receipt, doc, kkm)
    }

    override fun renderXReportHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        return xReportRenderer.render(shift, counters, kkm, ofdStatus)
    }

    override fun renderOpenShiftHtml(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        return openShiftRenderer.render(shift, kkm, ofdStatus)
    }

    override fun renderCloseShiftHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        return zReportRenderer.render(shift, counters, kkm, ofdStatus)
    }

    override fun renderCashOperationHtml(
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo
    ): String {
        return cashOperationRenderer.render(doc, kkm)
    }

    override fun renderPreviewHtml(branding: ReceiptBranding): String {
        val mockItems = listOf(
            ReceiptItem(
                name = "Товар Предпросмотра / Алдын ала қарау тауары",
                sectionCode = "001",
                quantity = 1L,
                price = Money(150, 0),
                sum = Money(150, 0),
                measureUnitCode = UnitOfMeasurement.PIECE.code,
                vatGroup = VatGroup.VAT_16
            ),
            ReceiptItem(
                name = "Услуга Демонстрации / Демонстрациялық қызмет",
                sectionCode = "001",
                quantity = 2L,
                price = Money(250, 0),
                sum = Money(500, 0),
                measureUnitCode = UnitOfMeasurement.PIECE.code,
                vatGroup = VatGroup.VAT_16
            )
        )
        val mockPayments = listOf(
            ReceiptPayment(
                type = PaymentType.CASH,
                sum = Money(300, 0)
            ),
            ReceiptPayment(
                type = PaymentType.CARD,
                sum = Money(350, 0)
            )
        )
        val mockReceipt = ReceiptRequest(
            kkmId = "KKM-PREVIEW",
            pin = "0000",
            operation = ReceiptOperationType.SELL,
            items = mockItems,
            payments = mockPayments,
            total = Money(650, 0),
            taken = Money(650, 0),
            change = Money(0, 0),
            idempotencyKey = "mock-preview-id",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16
        )
        val mockDoc = FiscalDocumentSnapshot(
            id = "mock-preview-id",
            cashboxId = "KKM-PREVIEW",
            shiftId = "shift-7",
            docType = "CHECK",
            docNo = 12345,
            shiftNo = 7,
            createdAt = System.currentTimeMillis(),
            totalAmount = 65000L,
            currency = "KZT",
            fiscalSign = "987654321012",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = null,
            receiptUrl = "https://consumer.oofd.kz/r/12345?r=NZ7700123456&i=123456789012&d=1774567890&s=650.00&f=987654321012",
            registrationNumber = "NZ7700123456",
            taxpayerName = "ТОО ДЕМО-БРЕНДИНГ / DEMO-BRANDING",
            taxpayerBin = "123456789012",
            taxpayerAddress = "г. Алматы, пр. Абая, 123 / Алматы қ., Абай даңғылы, 123",
            factoryNumber = "SW7700987654",
            ofdProvider = "KAZAKHTELECOM"
        )
        val mockKkm = KkmInfo(
            id = "KKM-PREVIEW",
            createdAt = 1774567890000L,
            updatedAt = 1774567890000L,
            mode = "PRODUCTION",
            state = "READY",
            registrationNumber = "NZ7700123456",
            factoryNumber = "SW7700987654",
            branding = branding,
            ofdServiceInfo = OfdServiceInfo(
                orgTitle = "ТОО ДЕМО-БРЕНДИНГ / DEMO-BRANDING",
                orgInn = "123456789012",
                orgAddress = "г. Алматы, пр. Абая, 123",
                orgAddressKz = "Алматы қ., Абай даңғылы, 123",
                orgOkved = "62010",
                geoLatitude = 432389,
                geoLongitude = 768897,
                geoSource = "GPS"
            )
        )
        return saleRenderer.render(mockReceipt, mockDoc, mockKkm)
    }
}
