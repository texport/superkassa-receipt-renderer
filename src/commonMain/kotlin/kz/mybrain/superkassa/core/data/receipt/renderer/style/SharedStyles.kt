package kz.mybrain.superkassa.core.data.receipt.renderer.style

import kz.mybrain.superkassa.core.data.receipt.renderer.base.ResourceLoader

object SharedStyles {
    val SHARED_CSS: String by lazy {
        ResourceLoader.readText("/css/shared_styles.css") ?: ""
    }
}
