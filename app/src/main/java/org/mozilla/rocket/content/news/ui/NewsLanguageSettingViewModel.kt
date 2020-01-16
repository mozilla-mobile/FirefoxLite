package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.utils.CharacterValidator
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.domain.LoadRawNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase

class NewsLanguageSettingViewModel(
    private val loadRawNewsLanguages: LoadRawNewsLanguagesUseCase,
    private val setUserPreferenceLanguage: SetUserPreferenceLanguageUseCase
) : ViewModel() {

    private lateinit var newsLanguages: List<NewsLanguage>

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _uiModel = MutableLiveData<NewsLanguageSettingUiModel>()
    val uiModel: LiveData<NewsLanguageSettingUiModel>
        get() = _uiModel

    fun requestLanguageList() {
        getSupportLanguages()
    }

    fun onRetryButtonClicked() {
        getSupportLanguages()
    }

    fun onLanguageSelected(language: NewsLanguage) = viewModelScope.launch {
        setUserPreferenceLanguage(language)
    }

    private fun getSupportLanguages() {
        launchDataLoad {
            val languagesResult = loadRawNewsLanguages()
            if (languagesResult is Result.Success) {
                val supportLanguages = filterSupportedLanguages(languagesResult.data)
                newsLanguages = supportLanguages
                emitUiModel(newsLanguages)
            } else if (languagesResult is Result.Error) {
                throw (languagesResult.exception)
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
        _uiModel.value = NewsLanguageSettingUiModel(newsLanguages)
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _isDataLoading.value = State.Loading
                block()
                _isDataLoading.value = State.Idle
            } catch (t: Throwable) {
                _isDataLoading.value = State.Error(t)
            }
        }
    }

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}

data class NewsLanguageSettingUiModel(
    val allLanguages: List<NewsLanguage>
)