package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

class ZReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        val lang = kkm.branding.language
        val status = ofdStatus ?: "DELIVERED"
        val ofdStatusHtml = when (status) {
            "DELIVERED", "SENT" -> "<span class=\"badge badge-success\">${translate("Отправлен", "Жіберілді", lang)}</span>"
            "PENDING", "OFFLINE" -> "<span class=\"badge badge-warning\">${translate("Офлайн", "Автономды", lang)}</span>"
            "FAILED", "ERROR" -> "<span class=\"badge badge-error\">${translate("Ошибка", "Қате", lang)}</span>"
            else -> "<span class=\"badge badge-warning\">$status</span>"
        }
        return renderZxReportHtml(
            titleRu = "Z-ОТЧЁТ (ЗАКРЫТИЕ СМЕНЫ)",
            titleKk = "Z-ЕСЕП (АУЫСЫМДЫ ЖАБУ)",
            shift = shift,
            counters = counters,
            isZReport = true,
            kkm = kkm,
            ofdStatusHtml = ofdStatusHtml
        )
    }
}
