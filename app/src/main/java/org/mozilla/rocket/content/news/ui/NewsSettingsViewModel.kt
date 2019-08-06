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
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import javax.inject.Inject

class NewsSettingsViewModel @Inject constructor(
    private val loadNewsSettings: LoadNewsSettingsUseCase,
    private val loadNewsLanguages: LoadNewsLanguagesUseCase
) : ViewModel() {

    private val _uiModel = MutableLiveData<NewsSettingsUiModel>()
    val uiModel: LiveData<NewsSettingsUiModel>
        get() = _uiModel

    init {
        getNewsSettings()
    }

    private fun getNewsSettings() = viewModelScope.launch(Dispatchers.Default) {
        val newsSettingsResult = loadNewsSettings()
        if (newsSettingsResult is Result.Success) {
            val newsSettings = newsSettingsResult.data

            val languagesResult = loadNewsLanguages()
            if (languagesResult is Result.Success) {
                val supportLanguages = filterSupportedLanguages(languagesResult.data)
                withContext(Dispatchers.Main) { emitUiModel(newsSettings, supportLanguages) }
            }
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

    private fun emitUiModel(newsSettings: Pair<NewsLanguage, List<NewsCategory>>, newsLanguages: List<NewsLanguage>) {
        _uiModel.value = NewsSettingsUiModel(newsSettings, newsLanguages)
    }
}

data class NewsSettingsUiModel(
    val newsSettings: Pair<NewsLanguage, List<NewsCategory>>,
    val newsLanguages: List<NewsLanguage>
)