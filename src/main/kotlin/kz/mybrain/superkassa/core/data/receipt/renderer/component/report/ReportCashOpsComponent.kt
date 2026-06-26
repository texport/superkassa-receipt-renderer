package kz.mybrain.superkassa.core.data.receipt.renderer.component.report

object ReportCashOpsComponent {
    @Suppress("LongParameterList")
    fun render(
        cashInCount: Long,
        cashInSum: Long,
        cashOutCount: Long,
        cashOutSum: Long,
        cashSumBills: Long,
        revenueBills: Long,
        t: (String) -> String,
        translateInlineKey: (String) -> String,
        formatAmount: (Long) -> String
    ): String {
        val revenueSign = if (revenueBills < 0) "-" else ""
        val revenueSumStr = formatAmount(kotlin.math.abs(revenueBills))

        val cashOpsTitle = translateInlineKey("cash_operations")
        val depLabel = t("deposits_count")
        val depSumStr = formatAmount(cashInSum)
        val depSumLabel = t("deposited_total")
        val wdrLabel = t("withdrawals_count")
        val wdrSumStr = formatAmount(cashOutSum)
        val wdrSumLabel = t("withdrawn_total")
        val cashSumLabel = t("cash_in_drawer")
        val cashSumStr = formatAmount(cashSumBills)
        val revenueLabel = t("shift_revenue")

        return """
            <fieldset class="section-card">
                <legend class="card-label">$cashOpsTitle</legend>
                <table class="meta-table" style="margin-top: 4px;">
                    <tr><td>$depLabel</td><td>$cashInCount</td></tr>
                    <tr><td>$depSumLabel</td><td>$depSumStr</td></tr>
                    <tr><td>$wdrLabel</td><td>$cashOutCount</td></tr>
                    <tr><td>$wdrSumLabel</td><td>$wdrSumStr</td></tr>
                    <tr class="bold"><td>$cashSumLabel</td><td>$cashSumStr</td></tr>
                    <tr class="bold"><td>$revenueLabel</td><td>$revenueSign$revenueSumStr</td></tr>
                </table>
            </fieldset>
        """.trimIndent()
    }
}
