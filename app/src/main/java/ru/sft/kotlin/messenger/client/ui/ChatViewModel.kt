package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.Message
import ru.sft.kotlin.messenger.client.data.entity.User

class ChatViewModel(
    application: Application,
    val chatId: Int,
    val isSystemChat: Boolean
) :
    AndroidViewModel(application) {

    private val repository = MessengerRepository.getInstance(application)

    val messages = repository.allChatMessagesWithMembers(chatId)

    val chat = repository.chatById(chatId)

    val currentUser: LiveData<User?>
        get() = repository.currentUser

    fun updateMessages() = viewModelScope.launch(Dispatchers.IO) {
        repository.updateMessages(chatId)
    }

    fun createMessage(message: Message) = viewModelScope.launch(Dispatchers.IO) {
        repository.createMessage(message)
    }

    fun deleteMessage(message: Message) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteMessage(message)
    }
}

class ChatViewModelFactory(
    private val application: Application,
    private val chatId: Int,
    private val isSystemChat: Boolean
) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(application, chatId, isSystemChat) as T
    }
}
