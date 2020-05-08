package ru.sft.kotlin.messenger.client.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.others_message_item.view.*
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.api.NewMessageInfo
import ru.sft.kotlin.messenger.client.data.entity.MessageWithMember
import ru.sft.kotlin.messenger.client.util.colorByUserId
import ru.sft.kotlin.messenger.client.util.formatTimeString
import java.lang.IllegalArgumentException
import java.util.*

class ChatActivity : AppCompatActivity() {

    private var menu: Menu? = null
    private lateinit var adapter: ChatAdapter
    private lateinit var model: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        // Показываем кнопку Back в заголовке, также надо задать android:parentActivityName в AndroidManifest.xml
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val chatId = intent.getIntExtra("chatId", -1)
        val isSystemChat = intent.getBooleanExtra("isSystemChat", false)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            // Настройка для того, чтобы сообщения показывались прижатыми к низу экрана
            stackFromEnd = true
        }

        model = ViewModelProvider(this, ChatViewModelFactory(this.application, chatId, isSystemChat))
            .get(ChatViewModel::class.java)

        // Берём id пользователя
        val userId = model.currentUser.value!!.userId

        title = model.chat.value?.name ?: "..."

        adapter = ChatAdapter(isSystemChat, userId)
        messagesRecyclerView.adapter = adapter

        model.chat.observe(this, Observer {
            title = model.chat.value?.name ?: "No chat name"
            updateUi(isSystemChat, menu)
        })

        adapter.setMessages(model.messages.value ?: emptyList())
        model.messages.observe(this, Observer { messages ->
            adapter.setMessages(messages)
            model.updateMessages()
        })

        sendButton.setOnClickListener {
            val text = messageText.text.toString()
            model.sendMessage(chatId, NewMessageInfo(text))
            messageText.text.clear()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        this.menu = menu
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        updateUi(model.isSystemChat, menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateUi(isSystemChat: Boolean, menu: Menu?) {
        if (menu != null) {
            menu.findItem(R.id.invite).isVisible = !isSystemChat
            menu.findItem(R.id.leave).isVisible = !isSystemChat
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.invite -> {
                inviteToChat()
                return true
            }
            R.id.leave -> {
                leaveChatAndFinish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun leaveChatAndFinish() {
        // TODO: покинуть чат, вернуться в MainActivity
        Toast.makeText(
            this,
            "TODO: покинуть чат, вернуться в MainActivity",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun inviteToChat() {
        // TODO: запустить Activity для поиска и ввода userId приглашаемого человека
        Toast.makeText(
            this,
            "TODO: запустить Activity для поиска и ввода userId приглашаемого человека",
            Toast.LENGTH_LONG
        ).show()
    }
}

enum class MessageViewType
{
    OTHERS, MY
}

class ChatAdapter(private val isSystemChat: Boolean, private val userId: String) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    class ChatViewHolder(val itemLayout: View) : RecyclerView.ViewHolder(itemLayout)

    // Cached messages
    private val messages = mutableListOf<MessageWithMember>()

    fun setMessages(messages: List<MessageWithMember>) {
        this.messages.clear()
        this.messages.addAll(messages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        // Check if it's my message
        val type = if (messages[position].userId == this.userId) MessageViewType.MY else MessageViewType.OTHERS
        return type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // Get the layout according to viewType
        val layout = when(viewType) {
            MessageViewType.OTHERS.ordinal -> R.layout.others_message_item
            MessageViewType.MY.ordinal -> R.layout.my_message_item
            else -> throw IllegalArgumentException("Invalid view type")
        }

        val itemLayout = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(
            itemLayout
        )
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val fromUser = message.memberDisplayName
        val dateTime = formatTimeString(Date(message.createdOn))
        val itemLayout = holder.itemLayout

        itemLayout.bodyTextView.text = message.text
        itemLayout.timeTextView.text = dateTime

        if (holder.itemViewType == MessageViewType.OTHERS.ordinal)
        {
            itemLayout.messageHeaderTextView.text = fromUser
            itemLayout.messageHeaderTextView.setTextColor(
                ContextCompat.getColor(holder.itemLayout.context, colorByUserId(fromUser))
            )
        }

        if (isSystemChat) {
//             TODO: в случае системного чата надо определять, является ли сообщение приглашением, и отображать кнопку "Join" под текстом сообщения
//             Для этого придётся проверять, что сообщение от системного пользователя и использовать регулярное выражение, т.к. другого способа API не предоставляет
//             Сделать кнопку видимой можно так: itemLayout.joinButton.visibility = View.VISIBLE
//             TODO: Также надо добавить обработчик нажатия на кнопку Join
        }

        itemLayout.setOnClickListener {
            Toast.makeText(itemLayout.context, "TODO: показать опции для работы с сообщением", Toast.LENGTH_LONG).show()
        }
    }

    override fun getItemCount() = messages.size
}