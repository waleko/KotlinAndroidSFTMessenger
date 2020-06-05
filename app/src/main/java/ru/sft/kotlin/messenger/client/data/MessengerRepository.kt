package ru.sft.kotlin.messenger.client.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.sft.kotlin.messenger.client.*
import ru.sft.kotlin.messenger.client.api.*
import ru.sft.kotlin.messenger.client.data.entity.*
import ru.sft.kotlin.messenger.client.util.CallNotExecutedException
import ru.sft.kotlin.messenger.client.util.Result
import ru.sft.kotlin.messenger.client.util.SingletonHolder
import ru.sft.kotlin.messenger.client.util.invokeAsync

class MessengerRepository private constructor(private val context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener, Authenticator {

    companion object : SingletonHolder<MessengerRepository, Context>({
        MessengerRepository(it.applicationContext)
    })


    // объект для работы с базой данных
    private val dao = MessengerDatabase.getInstance(context).dao()

    private var _currentUser = MutableLiveData<User?>(null)

    var currentUser: LiveData<User?> = _currentUser

    val currentUserChats: LiveData<List<ChatWithMembers>> = dao.allChatsWithMembers()

    val isSignedIn: Boolean
        get() = _currentUser.value != null


    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)
        preferences.getString(PREF_USER_ID, null)?.let {
            runBlocking {
                _currentUser.value = dao.getUser(it)
                    ?: throw IllegalStateException("Cannot load current user from database")
            }
        }
    }

    // создаём объект для конвертации в JSON и обратно
    private val objectMapper = jacksonObjectMapper().apply {
        configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
    }

    private val okHttpClient = with(OkHttpClient.Builder()) {
        // Добавляем специальный обработчик на случай ответа 401 Unauthorized
        // В этом случае надо будет обновить access token используя refresh token
        // См. метод authenticate ниже
        authenticator(this@MessengerRepository)
        build()
    }

    // объект для сетевых вызовов API мессенджера
    private var refreshTokenApi: RefreshTokenApi = buildRefreshTokenApi()

    private fun buildRefreshTokenApi(): RefreshTokenApi {
        // читаем URL сервера из настроек
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val serverUrl = preferences.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL)
            ?: throw IllegalStateException("null instead of DEFAULT_SERVER_URL")

        // создаём объект для работы с API
        return Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build().create(RefreshTokenApi::class.java)
    }

    // объект для сетевых вызовов API мессенджера
    private var api: MessengerApi = buildApi()

    private fun buildApi(): MessengerApi {
        // читаем URL сервера из настроек
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val serverUrl = preferences.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL)
            ?: throw IllegalStateException("null instead of DEFAULT_SERVER_URL")

        // создаём объект для работы с API
        return Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build().create(MessengerApi::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!isRequestWithAccessToken(response)) {
            return null
        }
        val token = updateAccessToken() ?: return null
        return response.request.newBuilder().header("Authorization", token).build()
    }

    private fun isRequestWithAccessToken(response: Response) =
        response.request.header("Authorization")?.startsWith("Bearer ") ?: false

    @Synchronized
    private fun updateAccessToken(): String? {
        val accessToken =
            getAccessToken() ?: throw IllegalStateException("No access token to refresh")
        val refreshToken =
            getRefreshToken() ?: throw IllegalStateException("No refresh token for refresh")
        try {
            val response = refreshTokenApi.refreshAccessToken(
                accessToken.toBearer(),
                RefreshTokenInfo(refreshToken)
            ).execute()
            if (response.isSuccessful) {
                val authInfo =
                    response.body() ?: throw IllegalStateException("Empty response during refresh")
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit()
                    .putString(PREF_ACCESS_TOKEN, authInfo.accessToken)
                    .putString(PREF_REFRESH_TOKEN, authInfo.refreshToken)
                    .apply()
                return authInfo.accessToken
            }
            Log.w(logTag, "Error during refresh token update: ${response.errorBody()}")
            return null
        } catch (t: Throwable) {
            Log.w(logTag, "Cannot update refresh token", t)
            return null
        }
    }

    suspend fun createMessage(message: Message) {
        dao.insertMessages(message)
    }

    suspend fun deleteMessage(message: Message) {
        dao.deleteMessage(message)
    }

    fun allChatMessages(chatId: Int): LiveData<List<Message>> {
        return dao.allChatMessages(chatId)
    }

    fun allChatMessagesWithMembers(chatId: Int): LiveData<List<MessageWithMember>> {
        return dao.allChatMessagesWithMembers(chatId)
    }

    fun chatById(chatId: Int): LiveData<ChatWithMembers?> {
        return dao.chatById(chatId)
    }

    fun getUsersByPartOfName(part: String): List<User> {
        return dao.findUsers(part)
    }

    suspend fun joinChat(chatId: Int, joinChatInfo: JoinChatInfo) {
        try {
            if (chatById(chatId).value != null) // Уже существует такой чат
                return
            val accessToken =
                getAccessToken() ?: throw CallNotExecutedException("Unable to get access token")
            api.joinToChat(chatId, joinChatInfo, accessToken.toBearer()).invokeAsync()
            updateChatsList()
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
        }
    }

    suspend fun leaveChat(chatId: Int) {
        try {
            val accessToken =
                getAccessToken() ?: throw CallNotExecutedException("Unable to get access token")
            val response = api.leaveChat(chatId, accessToken.toBearer()).invokeAsync()
            if (response["status"]?.equals("OK") != true)
                throw CallNotExecutedException("Non-ok status (${response["status"] ?: "<no status>"})")
            Log.i(logTag, "Left the chat #$chatId")
            dao.deleteMessagesByChatId(chatId)
            dao.deleteMembersByChatId(chatId)
            dao.deleteChatById(chatId)
            updateChatsList()
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
        }
    }

    suspend fun sendMessage(chatId: Int, newMessageInfo: NewMessageInfo) {
        try {
            val accessToken =
                getAccessToken() ?: throw CallNotExecutedException("Unable to get access token")
            // запрашиваем свежие сообщения с сервера
            val messageInfo =
                api.sendMessage(chatId, newMessageInfo, accessToken.toBearer()).invokeAsync()
            dao.insertMessages(Message(messageInfo, chatId))
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
        }
    }

    suspend fun sendInvite(chatId: Int, userId: String): Result<Boolean> {
        try {
            Log.i("MessengerRepository", "invite to chat $chatId pending to user $userId")
            val accessToken = getAccessToken() ?: throw CallNotExecutedException("Unable to get access token")
            val invite = InviteChatInfo(userId)
            api.inviteToChat(chatId, invite, accessToken.toBearer()).invokeAsync()
            Log.i("MessengerRepository", "invited successful!")
            return Result.Success(true)
        }
        catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
            return Result.Error(e)
        }
        catch (e: Exception) {
            Log.e(logTag, e.message, e)
            return Result.Error(e)
        }
    }

    suspend fun updateMessages(chatId: Int) {
        try {
            val lastMessageId = dao.lastChatMessageId(chatId)
            val accessToken = getAccessToken() ?: return
            // запрашиваем свежие сообщения с сервера
            val messagesInfo =
                api.listMessages(chatId, accessToken.toBearer(), lastMessageId).invokeAsync()
            val messages = messagesInfo
                .map {
                    Message(
                        it.messageId,
                        it.memberId,
                        chatId,
                        it.text,
                        it.createdOn
                    )
                }
                .toTypedArray()
            // сохраняем в базу данных
            dao.insertMessages(*messages)
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
        }
    }

    suspend fun updateUsers(part: String = "") {
        try {
            val accessToken = getAccessToken() ?: return
            // запрашиваем свежие сообщения с сервера
            val usersInfo =
                api.findUsersByPartOfName(accessToken.toBearer(), part).invokeAsync()
            val users = usersInfo.map { User(it) }.toTypedArray()
            // сохраняем в базу данных
            dao.insertUsers(*users)
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
        }
    }

    private val logTag = "Repository"

    suspend fun register(userId: String, password: String, displayName: String): Result<UserInfo> {
        val previousUser = _currentUser.value
        if (previousUser != null) {
            // регистрируем нового пользователя -> надо разлогиниться
            signOut()
        }
        try {
            val userInfo =
                api.registerUser(NewUserInfo(userId, displayName, password)).invokeAsync()
            return Result.Success(userInfo)
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
            return Result.Error(e)
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    suspend fun createChat(chatName: String): Result<ChatInfo> {
        val accessToken = getAccessToken()
        return if (accessToken != null) {
            try {
                val chatInfo =
                    api.createChat(NewChatInfo(chatName), accessToken.toBearer()).invokeAsync()
                Result.Success(chatInfo)
            } catch (e: CallNotExecutedException) {
                Log.w(logTag, "Request error: ${e.message}", e)
                Result.Error(e)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            Result.Error(Exception("No access token"))
        }
    }

    suspend fun updateChatsList(): Result<Boolean> {
        try {
            val accessToken =
                getAccessToken() ?: return Result.Error(Exception("AccessTokenIsNull"))
            val accessTokenHeader = accessToken.toBearer()
            // запрашиваем список чатов пользователя
            val chats = api.listChats(accessTokenHeader).invokeAsync()
            // NB! В качестве имени чата лучше использовать то имя, которое ему дал пользователь
            // Оно хранится в поле member.chatDisplayName участника чата с member.userId равным currentUser.userId
            val chatNames = mutableMapOf<Int, String>()
            val allMembers = mutableListOf<Member>()

            // NB! Сиситемный чат отличается тем, что из него нельзя выйти и в него нельзя приглашать.
            // Кроме того для него требуется специальная обработка сообщений с приглашениями в чаты
            // других пользователей. Системным является чат, в котором есть системный пользователь.
            var systemChatId: Int = -1

            // запрашиваем данные о системном пользователе и сохраняем в базе
            val systemUserInfo = api.getSystemUser(accessTokenHeader).invokeAsync()
            val systemUser = User(systemUserInfo.userId, systemUserInfo.displayName)
            dao.insertUsers(systemUser)

            // запрашиваем участников чата для каждого чата
            chats.forEach { chat ->
                val members =
                    api.listChatMembers(chat.chatId, accessTokenHeader).invokeAsync()
                val membersArray = members
                    .map {
                        val name = api.getUserByUserId(it.userId, accessTokenHeader)
                            .invokeAsync()?.displayName ?: "[ ${it.userId} ]"
                        dao.insertUsers(User(it.userId, name))
                        // Исползуем то имя чата, которое выбрал currentUser при создании свойго чата или вступлении в чужой чат
                        if (it.userId == currentUser.value!!.userId) {
                            chatNames[it.chatId] = it.chatDisplayName
                        }
                        // находим и запоминаем системный чат
                        if (it.userId == systemUserInfo.userId) {
                            systemChatId = it.chatId
                        }
                        Member(
                            it.memberId,
                            it.chatId,
                            it.chatDisplayName,
                            it.memberDisplayName,
                            it.userId,
                            it.isActive
                        )
                    }.toTypedArray()
                allMembers.addAll(membersArray)
            }
            val chatsArray = chats.map {
                Chat(
                    it.chatId,
                    it.chatId == systemChatId,
                    chatNames[it.chatId] ?: it.defaultName
                )
            }.toTypedArray()
            // сохраняем чаты и участников в базе данных
            dao.insertChats(*chatsArray)
            dao.insertMembers(*(allMembers.toTypedArray()))
            Log.v(logTag, "Chats update completed")
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
            return Result.Error(e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
            return Result.Error(e)
        }
        return Result.Success(true)
    }

    suspend fun signIn(userId: String, password: String): Result<User> {
        val previousUser = _currentUser.value
        if (previousUser != null && previousUser.userId != userId) {
            // пользователь поменялся - принудительно разлогиниваем предыдущего пользователя
            signOut()
        }
        try {
            // отправляем запрос Sign In на сервер
            val authInfo = api.signIn(userId, PasswordInfo(password)).invokeAsync()
            // запрашиваем данные пользователя
            val userInfo = api.getUserByUserId(userId, authInfo.accessTokenHeader).invokeAsync()
                ?: return Result.Error(IllegalStateException("User not found"))
            // обновляем даныне в настройках и локальной базе данных
            val user = User(userInfo.userId, userInfo.displayName)
            dao.insertUsers(user)

            // запрашиваем данные о системном пользователе и сохраняем в базе
            val systemUserInfo = api.getSystemUser(authInfo.accessTokenHeader).invokeAsync()
            val systemUser = User(systemUserInfo.userId, systemUserInfo.displayName)
            dao.insertUsers(systemUser)

            // сохраняем данные пользователя в настройках
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_USER_ID, userId)
                .putString(PREF_ACCESS_TOKEN, authInfo.accessToken)
                .putString(PREF_REFRESH_TOKEN, authInfo.refreshToken)
                .apply()
            _currentUser.value = user
            updateChatsList()
            return Result.Success(user)
        } catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
            return Result.Error(e)
        } catch (e: Exception) {
            Log.e(logTag, e.message, e)
            return Result.Error(e)
        }
    }

    suspend fun signOut() {
        if (!isSignedIn) {
            return
        }
        // выполняем Sign Out на сервере
        val accessToken = getAccessToken()
        if (accessToken != null) {
            try {
                api.signOut(accessToken.toBearer()).invokeAsync()
            } catch (e: CallNotExecutedException) {
                Log.w(logTag, "Request error: ${e.message}", e)
            } catch (e: Exception) {
                Log.w(logTag, e.message, e)
            }
        }

        // удаляем данные пользователя в локальной базе данных
        dao.deleteAllMessages()
        dao.deleteAllMembers()
        dao.deleteAllChats()
        dao.deleteAllUsers()

        // удаляем данные пользователя в настройках
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .remove(PREF_ACCESS_TOKEN)
            .remove(PREF_REFRESH_TOKEN)
            .remove(PREF_USER_ID)
            .apply()
        _currentUser.value = null
        Log.d(logTag, "SignedOut")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PREF_SERVER_URL -> {
                api = buildApi()
            }
        }
    }

    private fun getAccessToken() =
        PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_ACCESS_TOKEN, null)

    private fun getRefreshToken() =
        PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_REFRESH_TOKEN, null)

    private fun String.toBearer() = "Bearer $this"
}