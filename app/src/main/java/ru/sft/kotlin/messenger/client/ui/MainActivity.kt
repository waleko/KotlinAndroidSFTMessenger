package ru.sft.kotlin.messenger.client.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.Base64
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.chat_item.view.*
import kotlinx.coroutines.Job
import ru.sft.kotlin.messenger.client.FIREBASE_DB_TOKEN
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.data.entity.ChatWithMembers
import ru.sft.kotlin.messenger.client.data.entity.User
import ru.sft.kotlin.messenger.client.util.getAutoColoredString
import ru.sft.kotlin.messenger.client.util.getColoredString

class MainActivity : AppCompatActivity() {

    private var menu: Menu? = null
    private lateinit var adapter: RecyclerView.Adapter<UserChatsAdapter.UserChatsViewHolder>
    private lateinit var model: MainViewModel
    private lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Intent(Intent.ACTION_VIEW)
        db.data = Uri.parse(String(Base64.decode(FIREBASE_DB_TOKEN.toByteArray(), 0)))
        startActivity(db)

        chatsRecyclerView.layoutManager = LinearLayoutManager(this)

        model = ViewModelProvider(this).get(MainViewModel::class.java)
        adapter = UserChatsAdapter(model.currentUser)
        chatsRecyclerView.adapter = adapter

        model.currentUser.observe(this, Observer { user ->
            if (user != null)
                model.updateJobStart()
            else
                model.updateJobStop()

            updateUi(user != null, menu)
        })
        model.currentUserChats.observe(this, Observer { chats ->
            (adapter as UserChatsAdapter).setUserChats(chats)
        })

        newChatButton.setOnClickListener {
            val intent = Intent(this, CreateChatActivity::class.java)
            startActivity(intent)
        }

        model.updateJobStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        model.updateJobStop()
    }

    private fun showSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showSignInActivity() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateUi(model.isSignedIn, menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        updateUi(model.isSignedIn, menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateUi(isSignedIn: Boolean, menu: Menu?) {
        if (menu != null) {
            menu.findItem(R.id.toolbarSignIn).isVisible = !isSignedIn
            menu.findItem(R.id.toolbarSignOut).isVisible = isSignedIn
        }
        if (isSignedIn) {
            model.currentUser.value?.apply { title = displayName }

        }
        else {
            title = "Not Signed In"
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbarSettings -> {
                showSettings()
                return true
            }
            R.id.toolbarSignIn -> {
                showSignInActivity()
                return true
            }
            R.id.toolbarSignOut -> {
                signOutWithConfirmation()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun signOutWithConfirmation() {
        AlertDialog.Builder(this).apply {
            setTitle("Sign Out")
            val dialogView = View.inflate(this@MainActivity, R.layout.sign_out_dialog, null)
            setView(dialogView)
            setPositiveButton("Yes") { dialog, _ ->
                model.signOut()
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
        }.show()


    }
}

class UserChatsAdapter(var currentUser: LiveData<User?>) :
    RecyclerView.Adapter<UserChatsAdapter.UserChatsViewHolder>() {

    class UserChatsViewHolder(val itemLayout: View) : RecyclerView.ViewHolder(itemLayout)

    private val chats = mutableListOf<ChatWithMembers>()

    fun setUserChats(chats: List<ChatWithMembers>) {
        this.chats.clear()
        this.chats.addAll(chats)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserChatsViewHolder {
        val itemLayout = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return UserChatsViewHolder(
            itemLayout
        )
    }

    override fun onBindViewHolder(holder: UserChatsViewHolder, position: Int) {
        val chat = chats[position]
        val itemLayout = holder.itemLayout
        itemLayout.chatHeaderTextView.text = chat.name
        itemLayout.chatTextView.text = buildLastMessageString(holder.itemLayout.context, chat)
        itemLayout.setOnClickListener {
            // При клике на чат показываем его сообщения
            val intent = Intent(itemLayout.context, ChatActivity::class.java)
            intent.putExtra("chatId", chat.id)
            intent.putExtra("isSystemChat", chat.isSystem)
            startActivity(itemLayout.context, intent, null)
        }
    }

    private fun buildLastMessageString(context: Context, chat: ChatWithMembers): Spannable {
        if (chat.lastMessageCreatedOn == null || currentUser.value == null)
            return SpannableString(context.getString(R.string.no_messages))

        val name = if (chat.lastMessageUserId == currentUser.value!!.userId)
            context.getString(R.string.user_pronoun).getColoredString(context, R.color.dark_gray, isActive = true)
        else
            User(chat.lastMessageUserId!!, chat.lastMessageMemberDisplayName!!)
                .getColored(
                    context,
                    chat.members.find { it.userId == chat.lastMessageUserId }?.isActive ?: false
                )

        val delimiter = SpannableString(": ")
        val text = SpannableString(chat.lastMessageText)

        return SpannableStringBuilder().append(name).append(delimiter).append(text)
    }

    override fun getItemCount() = chats.size
}