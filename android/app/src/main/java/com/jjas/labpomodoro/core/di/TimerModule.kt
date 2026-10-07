package com.jjas.labpomodoro.core.di

import android.content.Context
import android.os.SystemClock
import com.jjas.labpomodoro.service.AlarmDeadlineScheduler
import com.jjas.labpomodoro.service.FileTimerStore
import com.jjas.labpomodoro.service.TimerService
import com.jjas.labpomodoro.timer.DeadlineScheduler
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerServiceLauncher
import com.jjas.labpomodoro.timer.TimerStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import java.time.Instant
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope que vive lo mismo que el proceso, para trabajo que no debe morir con una pantalla. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    abstract fun bindDeadlineScheduler(impl: AlarmDeadlineScheduler): DeadlineScheduler

    @Binds
    abstract fun bindTimerStore(impl: FileTimerStore): TimerStore

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        fun provideTimeSource(clock: Clock): TimeSource = object : TimeSource {
            override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
            override fun now(): Instant = clock.instant()
        }

        @Provides
        fun provideServiceLauncher(@ApplicationContext context: Context) =
            TimerServiceLauncher { TimerService.start(context) }
    }
}
