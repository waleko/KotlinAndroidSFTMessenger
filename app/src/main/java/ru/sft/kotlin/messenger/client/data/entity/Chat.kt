package ru.sft.kotlin.messenger.client.data.entity

import androidx.room.*

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
    val members: List<Member>,

    @ColumnInfo(name = "text")
    val lastMessageText: String?,
    @ColumnInfo(name = "memberId")
    val lastMessageMemberId: Int?,
    @ColumnInfo(name = "createdOn")
    val lastMessageCreatedOn: Long?,
    @ColumnInfo(name = "memberDisplayName")
    val lastMessageMemberDisplayName: String?,
    @ColumnInfo(name = "userId")
    val lastMessageUserId: String?
)