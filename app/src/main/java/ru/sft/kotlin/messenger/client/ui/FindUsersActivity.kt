package ru.sft.kotlin.messenger.client.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_find_users.*
import kotlinx.android.synthetic.main.users_item.view.*
import ru.sft.kotlin.messenger.client.R
import ru.sft.kotlin.messenger.client.data.entity.User
import ru.sft.kotlin.messenger.client.util.afterTextChanged


class FindUsersActivity : AppCompatActivity() {

    private lateinit var adapter: UsersAdapter
    lateinit var model: FindUsersViewModel
    var chatId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_users)

        chatId = intent.getIntExtra("chatId", -1)

        usersRecyclerView.layoutManager = LinearLayoutManager(this)

        model = ViewModelProvider(this, FindUsersViewModelFactory(this.application, chatId))
            .get(FindUsersViewModel::class.java)

        adapter = UsersAdapter(this)   //TODO it doesn't seem good
        usersRecyclerView.adapter = adapter

        searchText.apply {
            afterTextChanged {
                model.updateUsersByPartOfId(it)
            }
        }

        model.users.observe(this, Observer {
            adapter.setUsers(it)
        })
    }
}

class UsersAdapter(private val parent: FindUsersActivity) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {
    class UsersViewHolder(val itemLayout: View) : RecyclerView.ViewHolder(itemLayout)

    private val users = mutableListOf<User>()

    fun setUsers(users: List<User>) {
        this.users.clear()
        this.users.addAll(users)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val itemLayout = LayoutInflater.from(parent.context).inflate(R.layout.users_item, parent, false)
        return UsersViewHolder(
            itemLayout
        )
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = users[position]
        val itemLayout = holder.itemLayout
        itemLayout.displayNameTextView.text = user.getColored(itemLayout.context)
        itemLayout.userIdTextView.text = user.userId
        itemLayout.setOnClickListener {
            Log.i("Adapter", "inviting to chat.")
            parent.model.sendInvite(parent.chatId, user.userId).invokeOnCompletion {
                Toast.makeText(
                    holder.itemLayout.context,
                    "Successfully invited!",
                    Toast.LENGTH_SHORT
                ).show()
                parent.finish()
            }
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}