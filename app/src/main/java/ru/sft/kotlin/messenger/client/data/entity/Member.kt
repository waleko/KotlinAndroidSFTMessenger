package ru.sft.kotlin.messenger.client.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Members",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = [ "id" ],
            childColumns = [ "chatId" ],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = [ "userId" ],
            childColumns = [ "userId" ],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = [ "chatId" ]),
        Index(value = [ "userId" ])
    ]
)
class Member(
    @PrimaryKey
    val id: Int,
    val chatId: Int,
    val chatDisplayName: String,
    val memberDisplayName: String,
    val userId: String)
