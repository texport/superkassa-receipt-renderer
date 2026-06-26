package kz.mybrain.superkassa.core.data.receipt.renderer.base

import kz.mybrain.superkassa.core.data.receipt.renderer.style.MetaDocumentStyles
import kz.mybrain.superkassa.core.data.receipt.renderer.style.SharedStyles
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ReceiptLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class BaseDocumentRenderer {

    protected fun getTranslationMap(key: String): Map<String, String>? {
        return ReceiptTranslator.getTranslationMap(key)
    }

    protected fun formatDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return instant.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(DocumentConstants.DATE_TIME_PATTERN))
    }

    protected fun translate(key: String, lang: ReceiptLanguage): String {
        return ReceiptTranslator.translate(key, lang)
    }

    protected fun translate(labelRu: String, labelKk: String, lang: ReceiptLanguage): String {
        return ReceiptTranslator.translate(labelRu, labelKk, lang)
    }

    protected fun translateInline(key: String, lang: ReceiptLanguage): String {
        return ReceiptTranslator.translateInline(key, lang)
    }

    protected fun renderOfdStatus(ofdStatus: String?, lang: ReceiptLanguage, isFiscal: Boolean = true): String {
        if (ofdStatus == null && !isFiscal) {
            val trans = getTranslationMap("status_not_transmitted")
            return renderStatusBadge(
                "neutral",
                trans?.get("ru") ?: "Not Transmitted",
                trans?.get("kk") ?: "Not Transmitted",
                lang
            )
        }
        return when (ofdStatus) {
            "DELIVERED", "SENT" -> {
                val trans = getTranslationMap("status_sent")
                renderStatusBadge(
                    "success",
                    trans?.get("ru") ?: "Sent",
                    trans?.get("kk") ?: "Sent",
                    lang
                )
            }
            "PENDING", "OFFLINE" -> {
                val trans = getTranslationMap("status_offline")
                renderStatusBadge(
                    "warning",
                    trans?.get("ru") ?: "Offline",
                    trans?.get("kk") ?: "Offline",
                    lang
                )
            }
            "FAILED", "ERROR" -> {
                val trans = getTranslationMap("status_error")
                renderStatusBadge(
                    "error",
                    trans?.get("ru") ?: "Error",
                    trans?.get("kk") ?: "Error",
                    lang
                )
            }
            else -> {
                val status = ofdStatus ?: "-"
                renderStatusBadge(
                    "warning",
                    status,
                    status,
                    lang
                )
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
            ReceiptLanguage.MIXED -> "<span class=\"badge badge-$classSuffix\">$labelKk / $labelRu</span>"
        }
    }

    protected fun formatAmount(bills: Long): String =
        String.format(java.util.Locale.US, "%.2f", bills.toDouble() / 100.0)

    protected fun renderPageFrame(title: String, bodyContent: String, kkm: KkmInfo, docCss: String = ""): String {
        val cleanTitle = title.replace(Regex("<[^>]*>"), "")
        val widthMm = kkm.branding.paperWidthMm
        val sizeRule = if (widthMm == 0) "auto" else "${widthMm}mm auto"
        val marginRule = if (widthMm == 0) "" else "margin: 0;"
        val widthClass = if (widthMm == 0) "tape-fullscreen" else "tape-${widthMm}mm"
        val brandingAdapter = HtmlBrandingAdapter(kkm.branding)
        val customCss = brandingAdapter.customCss
        val themeColor = kkm.branding.themeColor
        val themeMode = if (kkm.branding.useForceDarkTheme || customCss.contains("/* force-dark */") || customCss.contains("theme-dark")) "dark" else "light"
        val bodyClass = "$themeMode accent-$themeColor"

        return TemplateRenderer.render(
            "page_frame.html",
            mapOf(
                "title" to cleanTitle,
                "sizeRule" to sizeRule,
                "marginRule" to marginRule,
                "sharedCss" to SharedStyles.SHARED_CSS,
                "docCss" to docCss,
                "customCss" to customCss,
                "bodyClass" to bodyClass,
                "widthClass" to widthClass,
                "bodyContent" to bodyContent
            )
        )
    }

    @Suppress("LongParameterList")
    protected fun renderStandardDocument(
        titleKey: String,
        kkm: KkmInfo,
        createdAt: Long,
        shiftNo: Long?,
        docNo: String?,
        ofdStatus: String?,
        isFiscal: Boolean,
        isAutonomous: Boolean = false,
        fiscalSign: String? = null,
        ofdProvider: String? = null,
        receiptUrl: String? = null,
        qrDataUri: String? = null,
        additionalMeta: List<Pair<String, String>> = emptyList(),
        docCss: String = "",
        showOfdStatus: Boolean = true,
        bodyContent: String
    ): String {
        val lang = kkm.branding.language
        val docTitle = translate(titleKey, lang)

        val headerHtml = kz.mybrain.superkassa.core.data.receipt.renderer.component.common.HeaderComponent.render(
            title = docTitle,
            kkm = kkm,
            lang = lang,
            translateKey = { translate(it, lang) },
            translateMixed = { ru, kk -> translate(ru, kk, lang) }
        )

        val fiscalBadge = if (isFiscal) {
            val trans = getTranslationMap("status_fiscal")
            val ru = trans?.get("ru") ?: "Fiscal"
            val kk = trans?.get("kk") ?: "Fiscal"
            renderStatusBadge("success", ru, kk, lang)
        } else {
            val trans = getTranslationMap("status_not_fiscal")
            val ru = trans?.get("ru") ?: "Non-Fiscal"
            val kk = trans?.get("kk") ?: "Non-Fiscal"
            renderStatusBadge("warning", ru, kk, lang)
        }

        val ofdBadge = if (showOfdStatus) renderOfdStatus(ofdStatus, lang, isFiscal) else ""

        val errorReasonHtml = if (ofdStatus == "FAILED" || ofdStatus == "ERROR") {
            val trans = translate("ofd_error_reason", lang)
            "<div class=\"status-error-reason\">$trans</div>"
        } else {
            ""
        }

        val statusBlockHtml = kz.mybrain.superkassa.core.data.receipt.renderer.component.common.StatusBlockComponent.render(
            fiscalBadge = fiscalBadge,
            ofdBadge = ofdBadge,
            errorReasonHtml = errorReasonHtml
        )

        val metaCardHtml = kz.mybrain.superkassa.core.data.receipt.renderer.component.common.KkmMetadataComponent.render(
            kkm = kkm,
            shiftNo = shiftNo,
            docNo = docNo,
            formattedDateTime = formatDate(createdAt),
            additionalMeta = additionalMeta,
            translateInlineKey = { translateInline(it, lang) }
        )

        val brandingAdapter = HtmlBrandingAdapter(kkm.branding)
        val fiscalSectionHtml = kz.mybrain.superkassa.core.data.receipt.renderer.component.common.FiscalSectionComponent.render(
            isAutonomous = isAutonomous,
            fiscalSign = fiscalSign,
            ofdProvider = ofdProvider,
            receiptUrl = receiptUrl,
            qrDataUri = qrDataUri,
            beforeQrHtml = brandingAdapter.beforeQrHtml,
            translateInlineKey = { translateInline(it, lang) }
        )

        val footerHtml = kz.mybrain.superkassa.core.data.receipt.renderer.component.common.FooterComponent.render(
            kkm = kkm,
            titleKey = titleKey,
            translateInlineKey = { translateInline(it, lang) }
        )

        val body = """
            $headerHtml
            $statusBlockHtml
            $metaCardHtml
            <div class="rule"></div>
            $bodyContent
            $fiscalSectionHtml
            <div class="rule"></div>
            $footerHtml
        """.trimIndent()

        val fullCss = SharedStyles.SHARED_CSS + "\n" + docCss + "\n" + MetaDocumentStyles.META_DOCUMENT_CSS
        return renderPageFrame(translate(titleKey, lang), body, kkm, fullCss)
    }
}
