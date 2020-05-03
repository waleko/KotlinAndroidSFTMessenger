package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.util.Result

import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.User

data class SignInResult(
    val success: User? = null,
    val error: Int? = null
)

data class SignInFormState(val userIdError: Int? = null,
                           val passwordError: Int? = null,
                           val isDataValid: Boolean = false)

class SignInViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MessengerRepository.getInstance(application)

    private val _state = MutableLiveData<SignInFormState>()
    val state: LiveData<SignInFormState> = _state

    private val _result = MutableLiveData<SignInResult>()
    val result: LiveData<SignInResult> = _result

    fun signIn(userId: String, password: String) {
        // can be launched in a separate asynchronous job
        viewModelScope.launch {
            val result = repository.signIn(userId, password)
            if (result is Result.Success) {
                _result.value =
                    SignInResult(success = result.data)
            } else {
                _result.value =
                    SignInResult(error = R.string.login_failed)
            }
        }
    }

    fun dataChanged(userId: String, password: String) {
        if (!isUserIdValid(userId)) {
            _state.value =
                SignInFormState(userIdError = R.string.invalid_user_id)
        } else if (!isPasswordValid(password)) {
            _state.value =
                SignInFormState(passwordError = R.string.invalid_password)
        } else {
            _state.value =
                SignInFormState(isDataValid = true)
        }
    }

    private fun isUserIdValid(userId: String): Boolean {
        return userId.isNotBlank()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank()
    }
}
