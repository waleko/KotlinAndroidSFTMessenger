package ru.sft.kotlin.messenger.client.data

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.sft.kotlin.messenger.client.data.entity.*

@Dao
interface MessengerDao {

    @Query("SELECT * FROM Chats ORDER BY name ASC")
    fun allChats(): LiveData<List<Chat>>

    @Transaction
    @Query("SELECT C.*, M.memberId, M.text, M.createdOn, E.memberDisplayName, E.userId FROM Chats C LEFT JOIN Messages M ON M.id = (SELECT MAX(M.id) FROM Messages M WHERE M.chatId = C.id) LEFT JOIN Members E ON E.id = M.memberId ORDER BY createdOn DESC")
    fun allChatsWithMembers(): LiveData<List<ChatWithMembers>>

    @Query("SELECT Messages.* FROM Messages JOIN Members ON Members.id = Messages.memberId WHERE Members.chatId = :chatId ORDER BY createdOn ASC")
    fun allChatMessages(chatId: Int): LiveData<List<Message>>

    @Query("SELECT M.*, E.chatId, E.memberDisplayName, E.userId FROM Messages M JOIN Members E ON E.id = M.memberId WHERE E.chatId = :chatId ORDER BY createdOn ASC")
    fun allChatMessagesWithMembers(chatId: Int): LiveData<List<MessageWithMember>>

    @Transaction
    @Query("SELECT C.*, M.memberId, M.text, M.createdOn, E.memberDisplayName, E.userId FROM Chats C LEFT JOIN Messages M ON M.id = (SELECT MAX(M.id) FROM Messages M WHERE M.chatId = :chatId) LEFT JOIN Members E ON E.id = M.memberId WHERE C.id = :chatId")
    fun chatById(chatId: Int): LiveData<ChatWithMembers?>

    @Query("SELECT MAX(Messages.id) FROM Messages JOIN Members ON Members.id = Messages.memberId WHERE Members.chatId = :chatId")
    fun lastChatMessageId(chatId: Int): Int

    @Query("SELECT M.*, E.chatId, E.memberDisplayName, E.userId FROM Messages M JOIN Members E ON E.id = M.memberId WHERE M.id = (SELECT MAX(M.id) FROM Messages M JOIN Members E ON E.id = M.memberId WHERE E.chatId = :chatId)")
    fun lastChatMessage(chatId: Int): MessageWithMember?

    @Query("SELECT count(*) FROM Messages")
    fun countMessages(): Int

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM Messages")
    suspend fun deleteAllMessages()

    @Update
    fun updateMessages(vararg messages: Message)

    @Query("SELECT * FROM Users")
    fun allUsers(): LiveData<List<User>>

    @Query("SELECT * FROM Members")
    fun allMembers(): LiveData<List<Member>>

    @Query("SELECT count(*) FROM Users")
    fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(vararg users: User)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChats(vararg chats: Chat)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembers(vararg members: Member)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(vararg messages: Message)

    @Delete
    suspend fun deleteUser(users: User)

    @Query("DELETE FROM Users")
    suspend fun deleteAllUsers()

    @Update
    fun updateUser(vararg users: User)

    @Query("DELETE FROM Members")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM Chats")
    suspend fun deleteAllChats()

    @Query("SELECT * FROM Users WHERE userId = :userId")
    suspend fun getUser(userId: String): User?
}