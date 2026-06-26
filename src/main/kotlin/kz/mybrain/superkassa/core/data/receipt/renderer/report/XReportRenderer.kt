package kz.mybrain.superkassa.core.data.receipt.renderer.report

import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

class XReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        return renderZxReportHtml(
            titleKey = "x_report",
            shift = shift,
            counters = counters,
            isZReport = false,
            kkm = kkm,
            ofdStatus = ofdStatus
        )
    }
}
