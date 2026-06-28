package kz.mybrain.superkassa.core.data.receipt.renderer.base

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.NSUTF8StringEncoding

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual object ResourceLoader {
    actual fun readText(path: String): String? {
        val cleanPath = path.removePrefix("/")
        val extension = cleanPath.substringAfterLast('.', "")
        val name = cleanPath.substringBeforeLast('.')
        
        val bundle = NSBundle.mainBundle
        val filePath = bundle.pathForResource(name, extension) ?: return null
        return NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
    }
}
