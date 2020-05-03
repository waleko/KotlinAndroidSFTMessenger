package ru.sft.kotlin.messenger.client.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.message_item.view.*
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.data.entity.MessageWithMember
import java.text.SimpleDateFormat
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

        adapter = ChatAdapter(isSystemChat)
        messagesRecyclerView.adapter = adapter

        model = ViewModelProvider(this, ChatViewModelFactory(this.application, chatId, isSystemChat))
            .get(ChatViewModel::class.java)

        title = model.chat.value?.name ?: "..."

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
            // TODO: отправить сообщение
            Toast.makeText(
                this,
                "TODO: отправить сообщение",
                Toast.LENGTH_LONG
            ).show()
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

class ChatAdapter(private val isSystemChat: Boolean) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    class ChatViewHolder(val itemLayout: View) : RecyclerView.ViewHolder(itemLayout)

    // Cached messages
    private val messages = mutableListOf<MessageWithMember>()

    fun setMessages(messages: List<MessageWithMember>) {
        this.messages.clear()
        this.messages.addAll(messages)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemLayout = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return ChatViewHolder(
            itemLayout
        )
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val fromUser = message.memberDisplayName
        val dateTime = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault()).format(Date(message.createdOn))
        val itemLayout = holder.itemLayout
        itemLayout.messageHeaderTextView.text = "$dateTime [ $fromUser ]"
        itemLayout.bodyTextView.text = message.text

        // TODO: Было бы лучше показывать сообщения текущего пользователя иначе, чем сообщения других пользователей.
        // Например, показывать их в "облачке" справа, а остальные - в "облачке" слева
        // Для начала можно не рисовать "облачка", а выделять сообщения разным цветом

        if (isSystemChat) {
            // TODO: в случае системного чата надо определять, является ли сообщение приглашением, и отображать кнопку "Join" под текстом сообщения
            // Для этого придётся проверять, что сообщение от системного пользователя и использовать регулярное выражение, т.к. другого способа API не предоставляет
            // Сделать кнопку видимой можно так: itemLayout.joinButton.visibility = View.VISIBLE
            // TODO: Также надо добавить обработчик нажатия на кнопку Join
        }
        itemLayout.setOnClickListener {
            Toast.makeText(itemLayout.context, "TODO: показать опции для работы с сообщением", Toast.LENGTH_LONG).show()
        }
    }

    override fun getItemCount() = messages.size
}