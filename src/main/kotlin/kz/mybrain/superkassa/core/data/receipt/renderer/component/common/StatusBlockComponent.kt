package kz.mybrain.superkassa.core.data.receipt.renderer.component.common

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
