package kz.mybrain.superkassa.core.data.receipt.renderer.base

import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.data.receipt.ReceiptFormatter

fun Money.formatted(): String {
    return ReceiptFormatter.formatMoney(this)
}

fun String.escaped(): String {
    return ReceiptFormatter.escape(this)
}
