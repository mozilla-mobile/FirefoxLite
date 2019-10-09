package org.mozilla.rocket.content.game.domain

import android.graphics.Bitmap
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.game.data.GameRepository

class GetBitmapFromImageLinkUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(imageUrl: String): Result<Bitmap> {
        return repository.getBitmapFromImageLink(imageUrl)
    }
}