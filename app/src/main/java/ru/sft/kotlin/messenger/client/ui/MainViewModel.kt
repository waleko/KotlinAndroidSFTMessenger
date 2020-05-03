package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.ChatWithMembers
import ru.sft.kotlin.messenger.client.data.entity.User

class MainViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = MessengerRepository.getInstance(application)

    val isSignedIn: Boolean
        get() = repository.isSignedIn

    val currentUser: LiveData<User?>
        get() = repository.currentUser

    val currentUserChats : LiveData<List<ChatWithMembers>>
        get() = repository.currentUserChats

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}