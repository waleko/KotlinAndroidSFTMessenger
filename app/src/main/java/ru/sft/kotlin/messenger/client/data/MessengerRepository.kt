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
import ru.sft.kotlin.messenger.client.api.MessengerApi
import ru.sft.kotlin.messenger.client.api.PasswordInfo
import ru.sft.kotlin.messenger.client.api.RefreshTokenApi
import ru.sft.kotlin.messenger.client.api.RefreshTokenInfo
import ru.sft.kotlin.messenger.client.data.entity.*
import ru.sft.kotlin.messenger.client.util.CallNotExecutedException
import ru.sft.kotlin.messenger.client.util.Result
import ru.sft.kotlin.messenger.client.util.SingletonHolder
import ru.sft.kotlin.messenger.client.util.invokeAsync

class MessengerRepository private constructor(private val context: Context):
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
                _currentUser.value =  dao.getUser(it) ?: throw IllegalStateException("Cannot load current user from database")
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
        val accessToken = getAccessToken() ?: throw IllegalStateException("No access token to refresh")
        val refreshToken = getRefreshToken() ?: throw IllegalStateException("No refresh token for refresh")
        try {
            val response = refreshTokenApi.refreshAccessToken(
                accessToken.toBearer(),
                RefreshTokenInfo(refreshToken)
            ).execute()
            if (response.isSuccessful) {
                val authInfo = response.body() ?: throw IllegalStateException("Empty response during refresh")
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
        }
        catch (t: Throwable) {
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

    suspend fun updateMessages(chatId: Int) {
        try {
            val lastMessageId = dao.lastChatMessage(chatId)
            val accessToken = getAccessToken() ?: return
            // запрашиваем свежие сообщения с сервера
            val messagesInfo =
                api.listMessages(chatId, accessToken.toBearer(), lastMessageId).invokeAsync()
            val messages = messagesInfo
                .map {
                    Message(
                        it.messageId,
                        it.memberId,
                        it.text,
                        it.createdOn
                    )
                }
                .toTypedArray()
            // сохраняем в базу данных
            dao.insertMessages(*messages)
        }
        catch (e: CallNotExecutedException) {
            Log.w(logTag, "Request error: ${e.message}", e)
        }
        catch (e: Exception) {
            Log.e(logTag, e.message, e)
        }
    }

    private val logTag = "Repository"

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
            val userInfo = api.getUserByUserId(userId, authInfo.accessTokenHeader).invokeAsync() ?: return Result.Error(IllegalStateException("User not found"))
            // запрашиваем список чатов пользователя
            val chats = api.listChats(authInfo.accessTokenHeader).invokeAsync()
            // обновляем даныне в настройках и локальной базе данных
            val user = User(userInfo.userId, userInfo.displayName)
            dao.insertUsers(user)

            // NB! Сиситемный чат отличается тем, что из него нельзя выйти и в него нельзя приглашать.
            // Кроме того для него требуется специальная обработка сообщений с приглашениями в чаты
            // других пользователей. Системным является чат, в котором есть системный пользователь.
            var systemChatId : Int = -1

            // запрашиваем данные о системном пользователе и сохраняем в базе
            val systemUserInfo = api.getSystemUser(authInfo.accessTokenHeader).invokeAsync()
            val systemUser = User(systemUserInfo.userId, systemUserInfo.displayName)
            dao.insertUsers(systemUser)

            // NB! В качестве имени чата лучше использовать то имя, которое ему дал пользователь
            // Оно хранится в поле member.chatDisplayName участника чата с member.userId равным currentUser.userId
            val chatNames = mutableMapOf<Int, String>()
            val allMembers = mutableListOf<Member>()

            // запрашиваем участников чата для каждого чата
            chats.forEach { chat ->
                val members = api.listChatMembers(chat.chatId, authInfo.accessTokenHeader).invokeAsync()
                val membersArray = members
                    .map {
                        val name = api.getUserByUserId(it.userId, authInfo.accessTokenHeader).invokeAsync()?.displayName ?: "[ ${it.userId} ]"
                        dao.insertUsers(User(it.userId, name))
                        // Исползуем то имя чата, которое выбрал currentUser при создании свойго чата или вступлении в чужой чат
                        if (it.userId == userId) {
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
            // сохраняем данные пользователя в настройках
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_USER_ID, userId)
                .putString(PREF_ACCESS_TOKEN, authInfo.accessToken)
                .putString(PREF_REFRESH_TOKEN, authInfo.refreshToken)
                .apply()
            _currentUser.value = user
            return Result.Success(user)
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

    private fun getAccessToken() = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_ACCESS_TOKEN, null)
    private fun getRefreshToken() = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_REFRESH_TOKEN, null)
    private fun String.toBearer() = "Bearer $this"
}