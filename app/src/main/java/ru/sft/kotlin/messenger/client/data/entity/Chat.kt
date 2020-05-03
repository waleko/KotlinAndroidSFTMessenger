package ru.sft.kotlin.messenger.client.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "Chats"
)
class Chat(
    @PrimaryKey
    val id: Int,
    val isSystem: Boolean,
    val name: String
)

class ChatWithMembers (
    val id: Int,
    val isSystem: Boolean,
    val name: String,
    @Relation(parentColumn = "id", entityColumn = "chatId")
    val members: List<Member>
)