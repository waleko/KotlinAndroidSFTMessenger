package ru.sft.kotlin.messenger.client.data.entity

import android.content.Context
import android.text.Spannable
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.sft.kotlin.messenger.client.api.UserInfo
import ru.sft.kotlin.messenger.client.util.getAutoColoredString

@Entity(tableName = "Users")
data class User (
    @PrimaryKey
    val userId: String,
    val displayName: String
) {
    constructor(userInfo: UserInfo) : this(
        userInfo.userId,
        userInfo.displayName
    )

    fun getColored(context: Context, isActive: Boolean = true) : Spannable =
        displayName.getAutoColoredString(context, userId, isActive)
}