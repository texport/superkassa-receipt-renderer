package kz.mybrain.superkassa.core.data.receipt.renderer.base
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

object DocumentConstants {
    const val QR_CODE_SIZE_PX = 180
    const val DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm:ss"
    const val DEFAULT_ORG_TITLE_KEY = "default_org_title"
}
