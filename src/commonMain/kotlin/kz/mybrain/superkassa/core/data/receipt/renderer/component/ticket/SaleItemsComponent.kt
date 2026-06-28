package kz.mybrain.superkassa.core.data.receipt.renderer.component.ticket
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

import kz.mybrain.superkassa.core.data.receipt.renderer.base.escaped
import kz.mybrain.superkassa.core.data.receipt.renderer.base.formatted
import kz.mybrain.superkassa.core.data.receipt.renderer.base.translationKey

object SaleItemsComponent {
    fun render(
        items: List<ReceiptItem>,
        defaultVatGroup: VatGroup,
        taxRegime: TaxRegime,
        receiptDiscount: Money?,
        t: (String) -> String,
        translateInlineKey: (String) -> String
    ): String {
        return items.joinToString("") { item ->
            val priceStr = item.price.formatted()
            val sumStr = item.sum.formatted()
            val unit = try {
                item.measureUnitCode?.let { UnitOfMeasurement.fromCode(it) } ?: UnitOfMeasurement.DEFAULT
            } catch (_: Exception) {
                UnitOfMeasurement.DEFAULT
            }
            val unitStr = translateInlineKey("unit_" + unit.name.lowercase())
            val exciseStamps = item.listExciseStamp
            val exciseHtml = if (!exciseStamps.isNullOrEmpty()) {
                val stamps = exciseStamps.joinToString(", ") { it.escaped() }
                "<div class=\"excise-stamps\">${t("excise_stamp")} $stamps</div>"
            } else {
                ""
            }
            val discountVal = item.discount
            val discountHtml = if (discountVal != null && receiptDiscount == null) {
                "<div class=\"item-discount\">${t("discount")} -${discountVal.formatted()}</div>"
            } else {
                ""
            }
            val markupVal = item.markup
            val markupHtml = if (markupVal != null) {
                "<div class=\"item-markup\">${t("markup")} +${markupVal.formatted()}</div>"
            } else {
                ""
            }
            val itemVat = item.vatGroup ?: defaultVatGroup
            val vatLabel = t(itemVat.translationKey)
            val vatHtml = if (taxRegime == TaxRegime.MIXED) {
                "<div class=\"item-vat\">$vatLabel</div>"
            } else {
                ""
            }
            val itemClass = if (item.isStorno) "storno-item" else ""
            val stornoBadgeHtml = if (item.isStorno) {
                """ <span class="storno-badge">${t("storno")}</span>"""
            } else {
                ""
            }
            """
            <div class="item-row-card $itemClass">
                <table class="item-row-table">
                    <tr>
                        <td class="item-name-cell">${item.name.escaped()}$stornoBadgeHtml</td>
                        <td class="item-sum-cell">${if (item.isStorno) "-" else ""}$sumStr</td>
                    </tr>
                    <tr>
                        <td class="item-details-cell" colspan="2">
                            ${item.quantity} $unitStr × $priceStr
                            $exciseHtml
                            $vatHtml
                            $discountHtml
                            $markupHtml
                        </td>
                    </tr>
                </table>
            </div>
            """.trimIndent()
        }
    }
}
