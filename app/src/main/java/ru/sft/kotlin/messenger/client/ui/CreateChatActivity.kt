package ru.sft.kotlin.messenger.client.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_create_chat.*
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.util.afterTextChanged

class CreateChatActivity : AppCompatActivity() {

    private lateinit var model: CreateChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat)

        model = ViewModelProvider(this).get(CreateChatViewModel::class.java)

        model.state.observe(this, Observer {
            val createChatState = it ?: return@Observer

            toolbarCreateChat.isEnabled = createChatState.isDataValid

            if (createChatState.chatNameError != null) {
                chat_name_create.error = getString(createChatState.chatNameError)
            }

        })

        model.result.observe(this, Observer {
            val createChatResult = it ?: return@Observer

            loading_create_chat.visibility = View.GONE

            if (createChatResult.error != null) {
                showCreateChatFailed(createChatResult.error)
            } else {
                setResult(Activity.RESULT_OK)

                finish()
            }
        })

        chat_name_create.afterTextChanged {
            model.dataChanged(
                chatName = chat_name_create.text.toString()
            )
        }

        chat_name_create.apply {
            afterTextChanged {
                model.dataChanged(
                    chatName = chat_name_create.text.toString()
                )
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        model.createChat(
                            chatName = chat_name_create.text.toString()
                        )
                }
                false
            }

            toolbarCreateChat.setOnClickListener {
                model.createChat(
                    chatName = chat_name_create.text.toString()
                )
            }
        }
    }

    private fun showCreateChatFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_LONG).show()
    }
}
