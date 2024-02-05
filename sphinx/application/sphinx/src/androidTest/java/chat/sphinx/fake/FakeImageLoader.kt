package chat.sphinx.fake

import android.widget.ImageView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.OnImageLoadListener
import java.io.File

class FakeImageLoader: ImageLoader<ImageView>() {
    override suspend fun load(
        imageView: ImageView,
        url: String,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener?
    ): Disposable {
        TODO("Not yet implemented")
    }

    override suspend fun load(
        imageView: ImageView,
        drawableResId: Int,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener?
    ): Disposable {
        TODO("Not yet implemented")
    }

    override suspend fun load(
        imageView: ImageView,
        file: File,
        options: ImageLoaderOptions?,
        listener: OnImageLoadListener?
    ): Disposable {
        TODO("Not yet implemented")
    }
}