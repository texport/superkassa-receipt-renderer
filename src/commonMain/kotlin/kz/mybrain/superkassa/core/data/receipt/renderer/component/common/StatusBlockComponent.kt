package kz.mybrain.superkassa.core.data.receipt.renderer.component.common
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

object StatusBlockComponent {
    fun render(
        fiscalBadge: String,
        ofdBadge: String,
        errorReasonHtml: String
    ): String {
        return """
            <div class="status-chips-container">
                $fiscalBadge
                $ofdBadge
            </div>
            $errorReasonHtml
        """.trimIndent()
    }
}
