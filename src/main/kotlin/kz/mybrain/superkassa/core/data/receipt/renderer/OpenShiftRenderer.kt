package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

class OpenShiftRenderer : BaseDocumentRenderer() {

    fun render(
        shift: ShiftInfo,
        kkm: KkmInfo,
        @Suppress("UNUSED_PARAMETER") ofdStatus: String?
    ): String {
        val lang = kkm.branding.language
        val isNarrow = kkm.branding.paperWidthMm <= 58
        fun t(ru: String, kk: String, sep: String = " / "): String = translate(ru, kk, lang, sep, isNarrow)

        val docTitle = t("ОТКРЫТИЕ СМЕНЫ", "АУЫСЫМДЫ АШУ", sep = "<br/>")
        val fiscalStatusHtml = renderStatusBadge("warning", "Не фискальный", "Фискальді емес", lang)
        val transmissionHtml = renderStatusBadge("neutral", "Не передается", "Жіберілмейді", lang)

        val registrationNumber = kkm.registrationNumber ?: "-"
        val factoryNumber = kkm.factoryNumber ?: "-"

        val headerHtml = renderHeaderHtml(docTitle, kkm, lang)
        val footerHtml = renderFooterHtml(kkm)

        val body = """
            $headerHtml
            <table class="meta-table">
                <tr><td>${t("Смена №", "Ауысым №")}</td><td>${shift.shiftNo}</td></tr>
                <tr><td>${t("ККМ ID", "БАҚ ID")}</td><td>${shift.kkmId}</td></tr>
                <tr><td>${t("РНМ", "ТНМ")}</td><td>${ReceiptFormatter.escape(registrationNumber)}</td></tr>
                <tr><td>${t("ЗНМ", "ЗНМ")}</td><td>${ReceiptFormatter.escape(factoryNumber)}</td></tr>
                <tr><td>${t("Время открытия", "Ашу уақыты")}</td><td>${formatDate(shift.openedAt)}</td></tr>
                <tr><td>${t("Фискальный статус", "Фискальді мәртебесі")}</td><td>$fiscalStatusHtml</td></tr>
                <tr><td>${t("Передача в ОФД", "ОФД-ға жіберу")}</td><td>$transmissionHtml</td></tr>
            </table>
            <div class="rule"></div>
            <div class="footer center">
                <div class="footer-item bold">${t("Смена открыта.", "Ауысым ашылды.")}</div>
                <div class="footer-item muted">${t("Печатная форма документа.", "Құжаттың баспа түрі.")}</div>
                $footerHtml
            </div>
        """.trimIndent()

        return renderPageFrame(t("ОТКРЫТИЕ СМЕНЫ", "АУЫСЫМДЫ АШУ"), body, kkm)
    }
}
