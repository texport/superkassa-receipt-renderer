package kz.mybrain.superkassa.core.data.receipt.renderer.component.common

import kz.mybrain.superkassa.core.domain.model.KkmInfo

object FooterComponent {
    fun render(
        kkm: KkmInfo,
        titleKey: String,
        translateInlineKey: (String) -> String
    ): String {
        val adapter = kz.mybrain.superkassa.core.data.receipt.renderer.base.HtmlBrandingAdapter(kkm.branding)
        val footerHtml = adapter.footerHtml
        val footerStatus = if (titleKey.contains("z_report")) {
            translateInlineKey("shift_closed")
        } else if (titleKey.contains("open_shift")) {
            translateInlineKey("shift_opened")
        } else {
            ""
        }

        val footerInfo = if (titleKey.contains("report")) {
            translateInlineKey("print_form_report")
        } else if (titleKey.contains("cash_in") || titleKey.contains("cash_out") || titleKey.contains("cash_operation")) {
            translateInlineKey("print_form_operation")
        } else {
            translateInlineKey("print_form_document")
        }

        val footerStatusHtml = if (footerStatus.isNotEmpty()) {
            "<div class=\"footer-item bold\">$footerStatus</div>"
        } else {
            ""
        }

        return """
            <div class="footer center">
                $footerStatusHtml
                <div class="footer-item muted">$footerInfo</div>
                $footerHtml
            </div>
        """.trimIndent()
    }
}
