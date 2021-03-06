package ru.sft.kotlin.messenger.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.data.MessengerRepository
import ru.sft.kotlin.messenger.client.data.entity.User
import ru.sft.kotlin.messenger.client.util.Result

data class RegisterResult(
    val success: User? = null,
    val error: Int? = null
)

data class RegisterFormState(
    val userIdError: Int? = null,
    val passwordError: Int? = null,
    val displayNameError: Int? = null,
    val isDataValid: Boolean = false
)

class RegisterViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MessengerRepository.getInstance(app)

    private val _state = MutableLiveData<RegisterFormState>()
    val state: LiveData<RegisterFormState> = _state

    private val _result = MutableLiveData<RegisterResult>()
    val result: LiveData<RegisterResult> = _result

    fun register(userId: String, password: String, displayName: String) {
        viewModelScope.launch {
            val registerResult = repository.register(userId, password, displayName)
            if (registerResult is Result.Success) {
                val signInResult = repository.signIn(userId, password)
                if (signInResult is Result.Success)
                    _result.value = RegisterResult(success = signInResult.data)
                else
                    _result.value = RegisterResult(error = R.string.login_after_register_failed)
            } else
                _result.value = RegisterResult(error = R.string.register_failed)

        }
    }

    fun dataChanged(userId: String, password: String, displayName: String) {
        if (!isUserIdValid(userId)) {
            _state.value = RegisterFormState(userIdError = R.string.invalid_user_id)
        } else if (!isDisplayNameValid(displayName)) {
            _state.value = RegisterFormState(displayNameError = R.string.invalid_display_name)
        } else if (!isPasswordValid(password)) {
            _state.value = RegisterFormState(passwordError = R.string.invalid_password)
        } else {
            _state.value = RegisterFormState(isDataValid = true)
        }
    }

    private fun isUserIdValid(userId: String): Boolean {
        return userId.isNotBlank()
    }

    private fun isDisplayNameValid(displayName: String): Boolean {
        return displayName.isNotBlank()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank()
    }
}