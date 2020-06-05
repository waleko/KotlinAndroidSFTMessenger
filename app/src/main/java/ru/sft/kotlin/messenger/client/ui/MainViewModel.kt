package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.ChatWithMembers
import ru.sft.kotlin.messenger.client.data.entity.User

class MainViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = MessengerRepository.getInstance(application)

    private var job: Job? = null

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

    fun updateJobStart() = viewModelScope.launch(Dispatchers.IO) {
        if (job != null) {
            return@launch
        }
        println("job_start")
        job = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            while (isActive) {
                println("job_check")
                if (currentUserChats.value != null) {
                    currentUserChats.value!!.forEach {
                        // TODO: load only one message (requires new server API)
                        repository.updateMessages(it.id)
                    }
                }
                delay(10000)
            }
        }
    }

    fun updateJobStop() = viewModelScope.launch(Dispatchers.IO) {
        job?.let{
            println("job_stop")
            it.cancelAndJoin()
        }
        job = null
    }

    fun updateList() = viewModelScope.launch(Dispatchers.IO) {
        repository.updateChatsList()
    }
}