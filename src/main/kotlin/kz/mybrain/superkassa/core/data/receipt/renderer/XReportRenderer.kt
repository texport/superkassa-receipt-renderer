package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

class XReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        @Suppress("UNUSED_PARAMETER") ofdStatus: String?
    ): String {
        val lang = kkm.branding.language
        val ofdStatusHtml = "<span class=\"badge badge-warning\">${translate("Не фискальный", "Фискальді емес", lang)}</span>"
        return renderZxReportHtml(
            titleRu = "X-ОТЧЁТ (БЕЗ ГАШЕНИЯ)",
            titleKk = "Х-ЕСЕП (АУЫСЫМДЫ ЖАППАЙ)",
            shift = shift,
            counters = counters,
            isZReport = false,
            kkm = kkm,
            ofdStatusHtml = ofdStatusHtml
        )
    }
}
