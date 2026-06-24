package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.KkmInfo

class CashOperationRenderer : BaseDocumentRenderer() {

    fun render(doc: FiscalDocumentSnapshot, kkm: KkmInfo): String {
        val lang = kkm.branding.language
        val isNarrow = kkm.branding.paperWidthMm <= 58
        fun t(ru: String, kk: String, sep: String = " / "): String = translate(ru, kk, lang, sep, isNarrow)

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
        val docTitle = t(titleRu, titleKk, sep = "<br/>")
        val sumStr = doc.totalAmount?.let { formatAmount(it) } ?: "0.00"
        val currency = doc.currency ?: "KZT"

        val ofdStatusHtml = when (doc.ofdStatus) {
            "DELIVERED", "SENT" -> "<span class=\"badge badge-success\">${t("Отправлен", "Жіберілді")}</span>"
            "PENDING", "OFFLINE" -> "<span class=\"badge badge-warning\">${t("Офлайн", "Автономды")}</span>"
            "FAILED", "ERROR" -> "<span class=\"badge badge-error\">${t("Ошибка", "Қате")}</span>"
            else -> "<span class=\"badge badge-warning\">${ReceiptFormatter.escape(doc.ofdStatus ?: "-")}</span>"
        }

        val registrationNumber = doc.registrationNumber ?: kkm.registrationNumber ?: "-"
        val factoryNumber = doc.factoryNumber ?: kkm.factoryNumber ?: "-"

        val headerHtml = renderHeaderHtml(docTitle, kkm, lang)
        val footerHtml = renderFooterHtml(kkm)

        val body = """
            $headerHtml
            <table class="meta-table">
                <tr><td>${t("Документ №", "Құжат №")}</td><td>${doc.docNo ?: doc.id}</td></tr>
                <tr><td>${t("ККМ ID", "БАҚ ID")}</td><td>${doc.cashboxId}</td></tr>
                <tr><td>${t("Смена №", "Ауысым №")}</td><td>${doc.shiftNo ?: "-"}</td></tr>
                <tr><td>${t("РНМ", "ТНМ")}</td><td>${ReceiptFormatter.escape(registrationNumber)}</td></tr>
                <tr><td>${t("ЗНМ", "ЗНМ")}</td><td>${ReceiptFormatter.escape(factoryNumber)}</td></tr>
                <tr><td>${t("Дата и время", "Күні мен уақыты")}</td><td>${formatDate(doc.createdAt)}</td></tr>
                <tr class="bold"><td>${t("Сумма", "Сомасы")}</td><td>$sumStr $currency</td></tr>
                <tr><td>${t("Статус ОФД", "ОФД статусы")}</td><td>$ofdStatusHtml</td></tr>
            </table>
            <div class="rule"></div>
            <div class="footer center">
                <div class="footer-item muted">${t("Печатная форма операции.", "Операцияның баспа түрі.")}</div>
                $footerHtml
            </div>
        """.trimIndent()

        return renderPageFrame(t(titleRu, titleKk), body, kkm)
    }
}
