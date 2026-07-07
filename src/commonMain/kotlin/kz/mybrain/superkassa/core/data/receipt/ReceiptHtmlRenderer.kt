package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.data.receipt.renderer.operation.CashOperationRenderer
import kz.mybrain.superkassa.core.data.receipt.renderer.report.OpenShiftRenderer
import kz.mybrain.superkassa.core.data.receipt.renderer.report.XReportRenderer
import kz.mybrain.superkassa.core.data.receipt.renderer.report.ZReportRenderer
import kz.mybrain.superkassa.core.data.receipt.renderer.ticket.SaleReceiptRenderer
import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.ofd.*
import kz.mybrain.superkassa.core.domain.model.shift.*
import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.domain.model.receipt.*
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.data.receipt.renderer.base.DocumentConstants

/**
 * HTML-реализация движка рендеринга чеков. Делегирует запросы рендеринга соответствующим компонентным саб-рендерерам.
 *
 * @property qrCodeGenerator генератор QR-кодов для фискальных документов
 * @property ofdProviders список настроенных провайдеров ОФД
 */
class ReceiptHtmlRenderer(
    qrCodeGenerator: QrCodeGeneratorPort
) : ReceiptRenderPort {

    private val saleRenderer = SaleReceiptRenderer(qrCodeGenerator)
    private val xReportRenderer = XReportRenderer()
    private val zReportRenderer = ZReportRenderer()
    private val openShiftRenderer = OpenShiftRenderer()
    private val cashOperationRenderer = CashOperationRenderer()

    private fun ReceiptLayoutType.toPaperWidthMm(): Int = when (this) {
        ReceiptLayoutType.TAPE_80MM -> DocumentConstants.TAPE_WIDTH_80_MM
        ReceiptLayoutType.TAPE_58MM -> DocumentConstants.TAPE_WIDTH_58_MM
        ReceiptLayoutType.FULLSCREEN -> DocumentConstants.FULLSCREEN_WIDTH
    }

    private fun overrideKkmLayout(kkm: KkmInfo, layoutType: ReceiptLayoutType?): KkmInfo {
        if (layoutType == null) return kkm
        return kkm.copy(branding = kkm.branding.copy(paperWidthMm = layoutType.toPaperWidthMm()))
    }

    private fun overrideBrandingLayout(branding: ReceiptBranding, layoutType: ReceiptLayoutType?): ReceiptBranding {
        if (layoutType == null) return branding
        return branding.copy(paperWidthMm = layoutType.toPaperWidthMm())
    }

    /**
     * Рендерит фискальный чек в формате HTML (продажа, покупка, возврат).
     *
     * @param receipt запрос на чек с позициями и платежами
     * @param doc снимок фискального документа
     * @param kkm информация о ККМ с настройками брендирования
     * @param layoutType тип разметки (ширина ленты или полноэкранный режим)
     * @return HTML-строка отрендеренного чека
     */
    override fun renderHtml(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType?
    ): String {
        return saleRenderer.render(receipt, doc, overrideKkmLayout(kkm, layoutType))
    }

    /**
     * Рендерит X-отчет (сменный отчет без гашения) в формате HTML.
     *
     * @param shift информация о текущей смене
     * @param counters счетчики операций за смену
     * @param kkm информация о ККМ с настройками брендирования
     * @param ofdStatus статус отправки данных в ОФД
     * @param layoutType тип разметки
     * @return HTML-строка отрендеренного X-отчета
     */
    override fun renderXReportHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        layoutType: ReceiptLayoutType?
    ): String {
        return xReportRenderer.render(shift, counters, overrideKkmLayout(kkm, layoutType), ofdStatus)
    }

    /**
     * Рендерит документ открытия смены в формате HTML.
     *
     * @param shift информация об открытой смене
     * @param kkm информация о ККМ
     * @param ofdStatus статус отправки данных в ОФД
     * @param docNo номер фискального документа
     * @param layoutType тип разметки
     * @return HTML-строка документа открытия смены
     */
    override fun renderOpenShiftHtml(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String?,
        layoutType: ReceiptLayoutType?
    ): String {
        return openShiftRenderer.render(shift, overrideKkmLayout(kkm, layoutType), ofdStatus, docNo)
    }

    /**
     * Рендерит Z-отчет (закрытие смены с гашением) в формате HTML.
     *
     * @param shift информация о закрытой смене
     * @param counters сменные счетчики итогов
     * @param kkm информация о ККМ
     * @param ofdStatus статус отправки данных в ОФД
     * @param docNo номер фискального документа
     * @param layoutType тип разметки
     * @return HTML-строка отрендеренного Z-отчета
     */
    override fun renderCloseShiftHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String?,
        layoutType: ReceiptLayoutType?
    ): String {
        return zReportRenderer.render(shift, counters, overrideKkmLayout(kkm, layoutType), ofdStatus, docNo)
    }

    /**
     * Рендерит документ внесения/изъятия наличных в формате HTML.
     *
     * @param doc снимок фискального документа
     * @param kkm информация о ККМ
     * @param layoutType тип разметки
     * @return HTML-строка документа нефискальной операции
     */
    override fun renderCashOperationHtml(
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType?
    ): String {
        return cashOperationRenderer.render(doc, overrideKkmLayout(kkm, layoutType))
    }

    /**
     * Генерирует демонстрационный (превью) чек для настройки брендирования и предпросмотра внешнего вида в редакторе.
     *
     * @param branding настройки брендирования
     * @param layoutType тип разметки
     * @return HTML-строка демонстрационного чека
     */
    override fun renderPreviewHtml(branding: ReceiptBranding, layoutType: ReceiptLayoutType?): String {
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
            createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
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
            branding = overrideBrandingLayout(branding, layoutType),
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
