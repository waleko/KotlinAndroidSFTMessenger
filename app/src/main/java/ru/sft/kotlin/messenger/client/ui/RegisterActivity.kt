package ru.sft.kotlin.messenger.client.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_register.*
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.util.afterTextChanged

class RegisterActivity : AppCompatActivity() {

    private lateinit var model: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        model = ViewModelProvider(this).get(RegisterViewModel::class.java)

        model.state.observe(this, Observer {
            val registerState = it ?: return@Observer

            toolbarRegister.isEnabled = registerState.isDataValid

            if (registerState.userIdError != null) {
                user_id_register.error = getString(registerState.userIdError)
            }
            if (registerState.displayNameError != null) {
                display_name.error = getString(registerState.displayNameError)
            }
            if (registerState.passwordError != null) {
                password_register.error = getString(registerState.passwordError)
            }
        })

        model.result.observe(this, Observer {
            val registerResult = it ?: return@Observer

            loading_register.visibility = View.GONE

            if (registerResult.error != null) {
                showRegisterFailed(registerResult.error)
            } else {
                setResult(Activity.RESULT_OK)
                finish()
            }
        })

        user_id_register.afterTextChanged {
            model.dataChanged(
                userId = user_id_register.text.toString(),
                password = password_register.text.toString(),
                displayName = display_name.text.toString()
            )
        }
        display_name.afterTextChanged {
            model.dataChanged(
                userId = user_id_register.text.toString(),
                password = password_register.text.toString(),
                displayName = display_name.text.toString()
            )
        }

        password_register.apply {
            afterTextChanged {
                model.dataChanged(
                    userId = user_id_register.text.toString(),
                    password = password_register.text.toString(),
                    displayName = display_name.text.toString()
                )
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        model.register(
                            userId = user_id_register.text.toString(),
                            password = password_register.text.toString(),
                            displayName = display_name.text.toString()
                        )
                }
                false
            }

            toolbarRegister.setOnClickListener {
                model.register(
                    userId = user_id_register.text.toString(),
                    password = password_register.text.toString(),
                    displayName = display_name.text.toString()
                )
            }
        }
    }

    private fun showRegisterFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_LONG).show()
    }
}
