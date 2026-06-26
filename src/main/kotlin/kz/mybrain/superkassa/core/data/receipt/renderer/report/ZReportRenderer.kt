package kz.mybrain.superkassa.core.data.receipt.renderer.report

import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

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
