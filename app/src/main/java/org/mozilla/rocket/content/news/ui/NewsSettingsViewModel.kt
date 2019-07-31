package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.CharacterValidator
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import javax.inject.Inject

class NewsSettingsViewModel @Inject constructor(
    private val loadNewsLanguagesUseCase: LoadNewsLanguagesUseCase
) : ViewModel() {

    private val _uiModel = MutableLiveData<NewsSettingsUiModel>()
    val uiModel: LiveData<NewsSettingsUiModel>
        get() = _uiModel

    init {
        getLanguages()
    }

    private fun getLanguages() = viewModelScope.launch(Dispatchers.Default) {
        val result = loadNewsLanguagesUseCase()
        if (result is Result.Success) {
            val supportLanguages = filterSupportedLanguages(result.data)
            withContext(Dispatchers.Main) { emitUiModel(supportLanguages) }
        }
    }

    private fun filterSupportedLanguages(rawLanguages: List<NewsLanguage>): List<NewsLanguage> {
        val supportLanguages = ArrayList<NewsLanguage>()
        rawLanguages.let {
            val displayCharacterForNotSupportedCharacter = "\u2612"
            val characterValidator = CharacterValidator(displayCharacterForNotSupportedCharacter)
            supportLanguages.addAll(it.filterNot { item -> characterValidator.characterIsMissingInFont(item.name.substring(0, 1)) })

            try {
                supportLanguages.sortBy { item -> item.code.toInt() }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        return supportLanguages
    }

    private fun emitUiModel(comments: List<NewsLanguage>) {
        _uiModel.value = NewsSettingsUiModel(comments)
    }
}

// TODO update to hold the entire setting elements
data class NewsSettingsUiModel(
    val newsLanguages: List<NewsLanguage>
)