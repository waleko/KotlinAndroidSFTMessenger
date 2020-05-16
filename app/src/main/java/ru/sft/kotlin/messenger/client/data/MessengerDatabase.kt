package ru.sft.kotlin.messenger.client.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.sft.kotlin.messenger.client.data.entity.*
import ru.sft.kotlin.messenger.client.util.SingletonHolder

@Database(
    entities = [
        User::class,
        Chat::class,
        Member::class,
        Message::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MessengerDatabase : RoomDatabase() {

    companion object : SingletonHolder<MessengerDatabase, Context>({
        Room.databaseBuilder(
            it.applicationContext,
            MessengerDatabase::class.java,
            "messenger.db"
        ).build()
    })

    abstract fun dao(): MessengerDao
}

