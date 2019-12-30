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
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase

class NewsLanguageOnboardingViewModel(
    private val loadNewsLanguages: LoadNewsLanguagesUseCase,
    private val setUserPreferenceLanguage: SetUserPreferenceLanguageUseCase
) : ViewModel() {

    private lateinit var newsLanguages: List<NewsLanguage>

    private val _uiModel = MutableLiveData<NewsLanguageOnboardingUiModel>()
    val uiModel: LiveData<NewsLanguageOnboardingUiModel>
        get() = _uiModel

    init {
        getSupportLanguages()
    }

    fun onLanguageSelected(language: NewsLanguage) = viewModelScope.launch(Dispatchers.Default) {
        setUserPreferenceLanguage(language)
    }

    private fun getSupportLanguages() = viewModelScope.launch(Dispatchers.Default) {
        val languagesResult = loadNewsLanguages()
        if (languagesResult is Result.Success) {
            val supportLanguages = filterSupportedLanguages(languagesResult.data)
            withContext(Dispatchers.Main) {
                newsLanguages = supportLanguages
                emitUiModel(newsLanguages)
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

    private fun emitUiModel(newsLanguages: List<NewsLanguage>) {
        _uiModel.value = NewsLanguageOnboardingUiModel(newsLanguages)
    }
}

data class NewsLanguageOnboardingUiModel(
    val allLanguages: List<NewsLanguage>
)