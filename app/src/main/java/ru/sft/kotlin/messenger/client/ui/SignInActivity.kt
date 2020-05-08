package ru.sft.kotlin.messenger.client.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_sign_in.*
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.util.afterTextChanged

class SignInActivity : AppCompatActivity() {

    private lateinit var model: SignInViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        model = ViewModelProvider(this).get(SignInViewModel::class.java)

        // Показ ошибок при вводе данных
        model.state.observe(this, Observer {
            val loginState = it ?: return@Observer

            toolbarSignIn.isEnabled = loginState.isDataValid

            if (loginState.userIdError != null) {
                user_id.error = getString(loginState.userIdError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        // Реакция на изменение состояния формы
        model.result.observe(this, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            else {
                setResult(Activity.RESULT_OK)
                finish()
            }
        })

        // Обработчики ввода имени пользователя
        user_id.afterTextChanged {
            model.dataChanged(user_id.text.toString(), password.text.toString())
        }

        // Обработчики ввода пароля
        password.apply {
            afterTextChanged {
                model.dataChanged(user_id.text.toString(), password.text.toString())
            }

            // Кнопка Done при вводе пароля
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        model.signIn(user_id.text.toString(), password.text.toString())
                }
                false
            }

            // Кнопа Sign In
            toolbarSignIn.setOnClickListener {
                loading.visibility = View.VISIBLE
                model.signIn(user_id.text.toString(), password.text.toString())
            }
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_LONG).show()
    }
}
