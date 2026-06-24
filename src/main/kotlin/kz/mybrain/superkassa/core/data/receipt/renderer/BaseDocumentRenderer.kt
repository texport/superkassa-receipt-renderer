package kz.mybrain.superkassa.core.data.receipt.renderer

import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter
import kz.mybrain.superkassa.core.data.receipt.ReceiptHtmlStyles
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ReceiptLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class BaseDocumentRenderer {

    protected fun formatDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return instant.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }

    protected fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage, separator: String = " / ", isNarrow: Boolean = false): String {
        if (labelRu == labelKk) return labelRu
        return when (lang) {
            ReceiptLanguage.RU -> labelRu
            ReceiptLanguage.KK -> labelKk
            ReceiptLanguage.MIXED -> {
                """<span class="lang-kk">$labelKk</span><span class="lang-sep">$separator</span><span class="lang-ru">$labelRu</span>"""
            }
        }
    }

    protected fun renderStatusBadge(
        classSuffix: String,
        labelRu: String,
        labelKk: String,
        lang: ReceiptLanguage
    ): String {
        return when (lang) {
            ReceiptLanguage.RU -> "<span class=\"badge badge-$classSuffix\">$labelRu</span>"
            ReceiptLanguage.KK -> "<span class=\"badge badge-$classSuffix\">$labelKk</span>"
            ReceiptLanguage.MIXED -> {
                "<span class=\"badge badge-$classSuffix\">" +
                        "<span class=\"badge-main\">$labelKk</span>" +
                        "<span class=\"badge-divider\"></span>" +
                        "<span class=\"badge-sub\">$labelRu</span>" +
                        "</span>"
            }
        }
    }

    protected fun formatAmount(bills: Long): String =
        String.format(java.util.Locale.US, "%.2f", bills.toDouble() / 100.0)

    protected fun renderPageFrame(title: String, bodyContent: String, kkm: KkmInfo): String {
        val cleanTitle = title.replace(Regex("<[^>]*>"), "")
        val widthMm = kkm.branding.paperWidthMm
        val widthClass = "tape-${widthMm}mm"
        val customCss = kkm.branding.customCss ?: ""
        val themeColor = kkm.branding.themeColor
        val themeMode = if (customCss.contains("/* force-dark */") || customCss.contains("theme-dark")) "dark" else "light"
        val bodyClass = "$themeMode accent-$themeColor"
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8" />
                <title>$cleanTitle</title>
                <style>
                    @page { size: ${widthMm}mm auto; margin: 0; }
                    ${ReceiptHtmlStyles.CSS}
                    $customCss
                </style>
            </head>
            <body class="$bodyClass">
                <div class="receipt $widthClass">
                    $bodyContent
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    protected fun renderHeaderHtml(
        title: String,
        kkm: KkmInfo,
        lang: ReceiptLanguage
    ): String {
        val logoUrl = kkm.branding.headerLogoUrl?.trim()
        val logoHtml = if (!logoUrl.isNullOrEmpty()) {
            """<div class="brand-logo-img"><img src="${ReceiptFormatter.escape(logoUrl)}" alt="Logo" /></div>"""
        } else {
            ""
        }
        val beforeHeader = kkm.branding.beforeHeaderHtml?.trim()?.let { "<div class=\"custom-before-header-container\">$it</div>" } ?: ""
        val customHeader = kkm.branding.headerHtml?.trim()?.let { "<div class=\"custom-header-container\">$it</div>" } ?: ""
        val afterHeader = kkm.branding.afterHeaderHtml?.trim()?.let { "<div class=\"custom-after-header-container\">$it</div>" } ?: ""

        val taxpayerName = kkm.ofdServiceInfo?.orgTitle ?: "SUPERKASSA"
        val taxpayerBin = kkm.ofdServiceInfo?.orgInn
        val taxpayerAddress = if (lang == ReceiptLanguage.KK) {
            kkm.ofdServiceInfo?.orgAddressKz ?: kkm.ofdServiceInfo?.orgAddress
        } else {
            kkm.ofdServiceInfo?.orgAddress
        }

        val orgTitleHtml = "<div class=\"brand-logo\">${ReceiptFormatter.escape(taxpayerName)}</div>"
        val orgBinHtml = if (!taxpayerBin.isNullOrBlank()) {
            val isNarrow = kkm.branding.paperWidthMm <= 58
            "<div class=\"org-bin\">${translate("БИН/ИИН", "БСН/ЖСН", lang, isNarrow = isNarrow)}: ${ReceiptFormatter.escape(taxpayerBin)}</div>"
        } else {
            ""
        }
        val orgAddressHtml = if (!taxpayerAddress.isNullOrBlank()) {
            "<div class=\"org-address\">${ReceiptFormatter.escape(taxpayerAddress)}</div>"
        } else {
            ""
        }

        return """
            <div class="center brand-header">
                $beforeHeader
                $logoHtml
                $customHeader
                $orgTitleHtml
                $orgBinHtml
                $orgAddressHtml
                $afterHeader
                <div class="rule"></div>
                <div class="doc-title">$title</div>
                <div class="doc-title-divider"></div>
            </div>
        """.trimIndent()
    }

    protected fun renderFooterHtml(kkm: KkmInfo): String {
        return kkm.branding.footerHtml?.trim()?.let { "<div class=\"custom-footer-container\">$it</div>" } ?: ""
    }
}
