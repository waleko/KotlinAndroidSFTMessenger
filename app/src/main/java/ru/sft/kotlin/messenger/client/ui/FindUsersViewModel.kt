package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.User

class FindUsersViewModel(
    application: Application,
    val chatId: Int
) : AndroidViewModel(application) {

    private val repository = MessengerRepository.getInstance(application)

    val users: MutableLiveData<List<User>> = MutableLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUsers()
            updateUsersByPartOfId("")
        }
    }

    fun sendInvite(chatId: Int, userId: String) = viewModelScope.launch {
        Log.i("FindUsersViewModel", "invite to chat $chatId pending to user $userId")
        repository.sendInvite(chatId, userId)
    }

    fun updateUsersByPartOfId(partOfId: String) = viewModelScope.launch(Dispatchers.IO) {
        users.postValue(repository.getUsersByPartOfName(partOfId))
    }
}

class FindUsersViewModelFactory(
    private val application: Application,
    private val chatId: Int
) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FindUsersViewModel(application, chatId) as T
    }
}
