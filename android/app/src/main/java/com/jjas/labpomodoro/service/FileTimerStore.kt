package com.jjas.labpomodoro.service

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.jjas.labpomodoro.domain.model.PlannedSession
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerSnapshot
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.timer.TimerStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El plan en curso en un archivo JSON. AtomicFile evita dejarlo a medias si el proceso muere
 * escribiendo. Está fuera del respaldo de Android (un plan no tiene sentido en otro teléfono).
 */
@Singleton
class FileTimerStore @Inject constructor(
    @ApplicationContext context: Context,
) : TimerStore {

    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))

    override suspend fun save(snapshot: TimerSnapshot?) = withContext(Dispatchers.IO) {
        if (snapshot == null) {
            file.delete()
            return@withContext
        }
        val out = file.startWrite()
        try {
            out.write(encode(snapshot).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.w(TAG, "No se pudo guardar el plan", e)
        }
    }

    override suspend fun load(): TimerSnapshot? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        // Un archivo dañado o de otra versión no debe impedir que la app abra
        runCatching { decode(JSONObject(String(file.readFully(), Charsets.UTF_8))) }
            .onFailure { Log.w(TAG, "Plan guardado ilegible; se descarta", it) }
            .getOrNull()
    }

    private fun encode(snapshot: TimerSnapshot): JSONObject {
        val s = snapshot.state
        return JSONObject()
            .put("version", VERSION)
            .put("savedAtWall", snapshot.savedAtWallMillis)
            .put("savedAtElapsed", snapshot.savedAtElapsed)
            .put("plan", JSONArray().apply {
                s.plan.forEach { put(JSONObject().put("type", it.type.name).put("seconds", it.durationSeconds)) }
            })
            .put("index", s.index)
            .put("paused", s.isPaused)
            .put("endsAtElapsed", s.endsAtElapsed)
            .put("remainingWhenPaused", s.remainingWhenPausedMillis)
            .put("sessionStartedAt", s.sessionStartedAt.toEpochMilli())
            .put("completedWork", s.completedWorkSessions)
            .put("completedWorkSeconds", s.completedWorkSeconds)
            .put("seed", s.planSeed)
            .put("skipped", JSONArray(s.skippedIndices.sorted()))
    }

    private fun decode(json: JSONObject): TimerSnapshot? {
        if (json.getInt("version") != VERSION) return null
        val planJson = json.getJSONArray("plan")
        val plan = List(planJson.length()) { i ->
            val item = planJson.getJSONObject(i)
            PlannedSession(SessionType.valueOf(item.getString("type")), item.getInt("seconds"))
        }
        val index = json.getInt("index")
        if (index !in plan.indices) return null
        val skipped = json.getJSONArray("skipped").let { a -> List(a.length()) { a.getInt(it) }.toSet() }
        return TimerSnapshot(
            state = TimerState.Active(
                plan = plan,
                index = index,
                isPaused = json.getBoolean("paused"),
                endsAtElapsed = json.getLong("endsAtElapsed"),
                remainingWhenPausedMillis = json.getLong("remainingWhenPaused"),
                sessionStartedAt = Instant.ofEpochMilli(json.getLong("sessionStartedAt")),
                completedWorkSessions = json.getInt("completedWork"),
                completedWorkSeconds = json.getLong("completedWorkSeconds"),
                planSeed = json.getLong("seed"),
                skippedIndices = skipped,
            ),
            savedAtWallMillis = json.getLong("savedAtWall"),
            savedAtElapsed = json.getLong("savedAtElapsed"),
        )
    }

    private companion object {
        const val TAG = "FileTimerStore"
        const val FILE_NAME = "timer_plan.json"
        const val VERSION = 1
    }
}
