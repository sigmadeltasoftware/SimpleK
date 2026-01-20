package io.github.kankan.demo

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade

actual fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}
