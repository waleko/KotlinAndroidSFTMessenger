package ru.sft.kotlin.messenger.client.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Users")
data class User (
    @PrimaryKey
    val userId: String,
    val displayName: String
)