package kz.mybrain.superkassa.core.data.receipt.renderer.report
import kz.mybrain.superkassa.core.domain.model.kkm.*
import kz.mybrain.superkassa.core.domain.model.ofd.*
import kz.mybrain.superkassa.core.domain.model.shift.*
import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.domain.model.auth.*
import kz.mybrain.superkassa.core.domain.model.delivery.*
import kz.mybrain.superkassa.core.domain.model.queue.*
import kz.mybrain.superkassa.core.domain.model.report.*
import kz.mybrain.superkassa.core.domain.model.receipt.*
import kz.mybrain.superkassa.core.domain.model.zxreport.*

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
