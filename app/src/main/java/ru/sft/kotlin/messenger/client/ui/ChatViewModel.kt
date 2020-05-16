package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.api.JoinChatInfo
import ru.sft.kotlin.messenger.client.api.NewMessageInfo
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

    fun sendMessage(chatId: Int, message: NewMessageInfo) = viewModelScope.launch(Dispatchers.IO) {
        repository.sendMessage(chatId, message)
    }

    fun joinChat(chatId: Int, secret: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.joinChat(chatId, JoinChatInfo(defaultName = null, secret = secret))
    }

    fun leaveChat(chatId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.leaveChat(chatId)
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
