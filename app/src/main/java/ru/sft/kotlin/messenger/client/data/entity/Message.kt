package ru.sft.kotlin.messenger.client.data.entity

import androidx.room.*
import ru.sft.kotlin.messenger.client.api.MessageInfo

@Entity(
    tableName = "Messages",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = [ "id" ],
            childColumns =  [ "memberId" ],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index( value = [ "memberId" ] )
    ]
)
data class Message (
    @PrimaryKey
    var id: Int,
    val memberId: Int,
    val text: String,
    val createdOn: Long
) {
    // FIXME Fix this redundancy
    constructor(message: MessageInfo) : this(
        message.messageId,
        message.memberId,
        message.text,
        message.createdOn
    )
}

class MessageWithMember (
    var id: Int,
    val memberId: Int,
    val text: String,
    val createdOn: Long,
    val chatId: Int,
    val memberDisplayName: String,
    val userId: String
)
