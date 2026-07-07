package kz.mybrain.superkassa.core.data.receipt.renderer.base

actual object ResourceLoader {
    actual fun readText(path: String): String? {
        val stream = ResourceLoader::class.java.getResourceAsStream(path) ?: return null
        return stream.bufferedReader().use { it.readText() }
    }
}
