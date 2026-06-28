package kz.mybrain.superkassa.core.data.receipt.renderer.report

import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.shift.*

class ZReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderZxReportHtml(
            titleKey = "z_report",
            shift = shift,
            counters = counters,
            isZReport = true,
            kkm = kkm,
            ofdStatus = ofdStatus,
            docNo = docNo
        )
    }
}
