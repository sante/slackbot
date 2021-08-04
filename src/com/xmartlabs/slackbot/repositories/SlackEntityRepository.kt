package com.xmartlabs.slackbot.repositories

import com.xmartlabs.slackbot.data.sources.SlackRemoteSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class SlackEntityRepository<T> {
    protected var cachedEntities: List<T> = listOf()
        private set
    protected abstract val remoteSource: SlackRemoteSource<T>

    init {
        GlobalScope.launch(Dispatchers.IO) {
            reloadCache()
        }
    }

    suspend fun reloadCache() = withContext(Dispatchers.IO) {
        getAndCacheRemoteEntities()
    }

    fun getRemoteEntities() = getAndCacheRemoteEntities()

    protected fun getAndCacheRemoteEntities() =
        remoteSource.getRemoteEntities()
            .also { cachedEntities = it }

    protected fun getEntities() = cachedEntities.ifEmpty { getAndCacheRemoteEntities() }

    protected inline fun getEntity(conditionCheck: (T) -> Boolean) =
        cachedEntities.firstOrNull { conditionCheck(it) }
            ?: getAndCacheRemoteEntities().firstOrNull { conditionCheck(it) }
}
