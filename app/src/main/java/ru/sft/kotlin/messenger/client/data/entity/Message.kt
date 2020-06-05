package ru.sft.kotlin.messenger.client.data.entity

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.sft.kotlin.messenger.client.api.MessageInfo

@Entity(
    tableName = "Messages",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = [ "id" ],
            childColumns =  [ "memberId" ],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Chat::class,
            parentColumns = [ "id" ],
            childColumns =  [ "chatId" ],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index( value = [ "memberId" ] ),
        Index( value = [ "chatId" ] )
    ]
)
open class Message (
    @PrimaryKey
    var id: Int,
    val memberId: Int,
    val chatId: Int,
    val text: String,
    val createdOn: Long
) {
    // FIXME Fix this redundancy
    constructor(message: MessageInfo, chatId: Int) : this(
        message.messageId,
        message.memberId,
        chatId,
        message.text,
        message.createdOn
    )
}

class MessageWithMember (
    id: Int,
    memberId: Int,
    text: String,
    createdOn: Long,
    chatId: Int,

    @Relation(parentColumn = "memberId", entityColumn = "id")
    val member: Member
) : Message(
    id,
    memberId,
    chatId,
    text,
    createdOn
) {
    val memberDisplayName: String
        get() = member.memberDisplayName
    val userId: String
        get() = member.userId
    val isMemberActive: Boolean
        get() = member.isActive
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX index_Messages_chatId ON Messages (chatId)")
    }
}
