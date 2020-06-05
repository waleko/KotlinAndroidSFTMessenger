package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.MINIMAL_USER_ID_LENGTH
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.User

class FindUsersViewModel(
    application: Application,
    val chatId: Int
) : AndroidViewModel(application) {

    private val repository = MessengerRepository.getInstance(application)

    val users: MutableLiveData<List<User>> = MutableLiveData()

    fun sendInvite(chatId: Int, userId: String) = viewModelScope.launch {
        Log.i("FindUsersViewModel", "invite to chat $chatId pending to user $userId")
        repository.sendInvite(chatId, userId)
    }

    fun updateUsersByPartOfId(partOfId: String) = viewModelScope.launch(Dispatchers.IO) {
        if (partOfId.length == MINIMAL_USER_ID_LENGTH) { // FIXME: Triggers both when typing text (as intended) and when erasing text (not as intended)
            repository.updateUsers(partOfId)
        }

        users.postValue(
            if (partOfId.length < MINIMAL_USER_ID_LENGTH)
                listOf()
            else
                repository.getUsersByPartOfName(partOfId)
        )
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
