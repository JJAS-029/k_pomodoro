package com.jjas.labpomodoro.data.repository

import com.jjas.labpomodoro.data.local.dao.TagDao
import com.jjas.labpomodoro.data.local.entity.TagEntity
import com.jjas.labpomodoro.data.local.entity.TagTotal
import com.jjas.labpomodoro.domain.model.TagColors
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val clock: Clock,
) {
    val tags: Flow<List<TagEntity>> = tagDao.observeAll()

    suspend fun create(name: String, color: Int): Long =
        tagDao.insert(TagEntity(name = name.trim().take(TagColors.MAX_NAME), color = color, createdAtMillis = clock.millis()))

    suspend fun update(id: Long, name: String, color: Int) = tagDao.update(id, name.trim().take(TagColors.MAX_NAME), color)

    suspend fun delete(id: Long) = tagDao.delete(id)

    suspend fun tagSession(startedAtMillis: Long, tagId: Long) = tagDao.tagSession(startedAtMillis, tagId)

    /** Enfoque por etiqueta en los últimos [days] días (null = todo). */
    fun totals(days: Long?): Flow<List<TagTotal>> {
        val from = days?.let { LocalDate.now(clock).minusDays(it - 1).toEpochDay() } ?: 0L
        return tagDao.observeTotals(from)
    }
}
