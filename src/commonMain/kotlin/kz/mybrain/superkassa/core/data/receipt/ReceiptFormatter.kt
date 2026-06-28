package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.domain.model.common.Money
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formatting utility for receipts and money values in Kazakh/Russian/English layouts.
 *
 * Утилита форматирования чеков и денежных сумм для казахской/русской/английской разметки.
 *
 * Чектерді және ақша сомаларын қазақ/орыс/ағылшын тілдерінде форматтауға арналған көмекші құрал.
 */
object ReceiptFormatter {

    /**
     * Converts a Money structure to total value in cents.
     */
    fun moneyToCents(m: Money): Long {
        return m.bills * 100L + m.coins
    }

    /**
     * Formats raw cents value to a standard dot-separated decimal string (e.g. 100.50).
     */
    fun formatCents(cents: Long): String {
        val whole = cents / 100
        val fraction = cents % 100
        val absFraction = if (fraction < 0) -fraction else fraction
        val fractionStr = if (absFraction < 10) "0$absFraction" else "$absFraction"
        return "$whole.$fractionStr"
    }

    /**
     * Formats a Money structure directly to a dot-separated decimal string.
     */
    fun formatMoney(m: Money): String {
        return formatCents(moneyToCents(m))
    }

    /**
     * Formats epoch timestamp in milliseconds to standard date time string dd.MM.yyyy HH:mm:ss.
     */
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

    /**
     * Escapes standard HTML special characters.
     */
    fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
