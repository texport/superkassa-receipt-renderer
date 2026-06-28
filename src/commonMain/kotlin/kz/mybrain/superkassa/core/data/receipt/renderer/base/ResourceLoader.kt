package kz.mybrain.superkassa.core.data.receipt.renderer.base

expect object ResourceLoader {
    fun readText(path: String): String?
}
