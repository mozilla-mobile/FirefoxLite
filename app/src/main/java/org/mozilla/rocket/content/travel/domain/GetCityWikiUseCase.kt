package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.data.Wiki

class GetCityWikiUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String): Result<Wiki> {
        val resultName = travelRepository.getCityWikiName(name)
        val result = travelRepository.getCityWiki(if (resultName is Result.Success) { resultName.data } else { name })

        if (result !is Result.Success) {
            return result
        }

        val revisedWiki = Wiki(result.data.imageUrl, result.data.introduction.trimContentWithinParentheses(), result.data.linkUrl)
        return Result.Success(revisedWiki)
    }
}

fun String.trimContentWithinParentheses(): String {
    var parenthesesCounter = 0

    val trimmed = this.filter {

        if (it == '(' || it == '（') {
            parenthesesCounter += 1
        }

        val keep = parenthesesCounter == 0
        if ((it == ')' || it == '）') && parenthesesCounter > 0) {
            parenthesesCounter -= 1
        }
        keep
    }
    return trimmed
}