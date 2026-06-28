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

import kz.mybrain.superkassa.core.data.receipt.renderer.base.BaseDocumentRenderer

class OpenShiftRenderer : BaseDocumentRenderer() {

    fun render(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderStandardDocument(
            titleKey = "open_shift",
            kkm = kkm,
            createdAt = shift.openedAt,
            shiftNo = shift.shiftNo,
            docNo = docNo,
            ofdStatus = ofdStatus,
            isFiscal = false,
            showOfdStatus = false,
            bodyContent = ""
        )
    }
}
