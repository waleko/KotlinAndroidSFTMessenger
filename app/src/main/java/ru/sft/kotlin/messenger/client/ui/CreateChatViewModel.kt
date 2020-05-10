package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.api.ChatInfo
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.util.Result

data class CreateChatResult(
    val success: ChatInfo? = null,
    val error: Int? = null
)

data class CreateChatFormState(
    val chatNameError: Int? = null,
    val isDataValid: Boolean = false
)

class CreateChatViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MessengerRepository.getInstance(app)

    private val _state = MutableLiveData<CreateChatFormState>()
    val state: LiveData<CreateChatFormState> = _state

    private val _result = MutableLiveData<CreateChatResult>()
    val result:  LiveData<CreateChatResult> = _result

    fun createChat(chatName: String) {
        viewModelScope.launch {
            val createChatResult = repository.createChat(chatName)
            if (createChatResult is Result.Success) {
                val updateChatsResult = repository.updateChatsList()
                if (updateChatsResult is Result.Success) {
                    _result.value = CreateChatResult(success = createChatResult.data)
                } else {
                    _result.value = CreateChatResult(error = R.string.update_chat_after_creatin_failed)
                }
            } else {
                _result.value = CreateChatResult(error = R.string.create_chat_failed)
            }
        }
    }

    fun dataChanged(chatName: String) {
        if (!isChatNameValid(chatName)) {
            _state.value = CreateChatFormState(chatNameError = R.string.invalid_chat_name)
        } else {
            _state.value = CreateChatFormState(isDataValid = true)
        }
    }

    private fun isChatNameValid(chatName: String): Boolean {
        return chatName.isNotBlank()
    }
}