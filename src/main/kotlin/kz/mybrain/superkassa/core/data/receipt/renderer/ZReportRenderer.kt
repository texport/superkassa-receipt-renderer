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
            "DELIVERED", "SENT" -> renderStatusBadge("success", "Отправлен", "Жіберілді", lang)
            "PENDING", "OFFLINE" -> renderStatusBadge("warning", "Офлайн", "Автономды", lang)
            "FAILED", "ERROR" -> renderStatusBadge("error", "Ошибка", "Қате", lang)
            else -> renderStatusBadge("warning", status, status, lang)
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
