package ru.sft.kotlin.messenger.client.util

open class SingletonHolder<out T: Any, in A>(private val creator: (A) -> T) {
    @Volatile
    private var instance: T? = null
    private val lock = Any()

    fun getInstance(arg: A): T =
        instance ?: synchronized(lock) {
            instance ?: creator(arg).also {
                instance = it
            }
        }
}