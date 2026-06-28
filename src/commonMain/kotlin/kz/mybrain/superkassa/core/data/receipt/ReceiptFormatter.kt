package kz.mybrain.superkassa.core.data.receipt
import kz.mybrain.superkassa.core.domain.model.common.Money

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ReceiptFormatter {

    fun moneyToCents(m: Money): Long {
        return m.bills * 100L + m.coins
    }

    fun formatCents(cents: Long): String {
        val whole = cents / 100
        val fraction = cents % 100
        val absFraction = if (fraction < 0) -fraction else fraction
        val fractionStr = if (absFraction < 10) "0$absFraction" else "$absFraction"
        return "$whole.$fractionStr"
    }

    fun formatMoney(m: Money): String {
        return formatCents(moneyToCents(m))
    }

    fun formatDate(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
        val month = localDateTime.monthNumber.toString().padStart(2, '0')
        val year = localDateTime.year
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val second = localDateTime.second.toString().padStart(2, '0')
        return "$day.$month.$year $hour:$minute:$second"
    }

    fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
