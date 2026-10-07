package com.jjas.labpomodoro

import android.app.Application
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.billing.BillingRepository
import com.jjas.labpomodoro.service.AutoBackup
import com.jjas.labpomodoro.service.FocusSoundController
import com.jjas.labpomodoro.service.LeagueSync
import com.jjas.labpomodoro.service.PlaceTracker
import com.jjas.labpomodoro.service.RewardSync
import com.jjas.labpomodoro.service.StreakReminderScheduler
import com.jjas.labpomodoro.service.TimerEffects
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.widget.TimerWidgetSync
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LabPomodoroApp : Application() {

    @Inject lateinit var timerEffects: TimerEffects

    @Inject lateinit var rewardSync: RewardSync

    @Inject lateinit var focusSound: FocusSoundController

    @Inject lateinit var billing: BillingRepository

    @Inject lateinit var placeTracker: PlaceTracker

    @Inject lateinit var autoBackup: AutoBackup

    @Inject lateinit var streakReminder: StreakReminderScheduler

    @Inject lateinit var leagueSync: LeagueSync

    @Inject lateinit var widgetSync: TimerWidgetSync

    @Inject lateinit var engine: TimerEngine

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        timerEffects.start()
        rewardSync.start()
        focusSound.start()
        billing.start()
        placeTracker.start()
        autoBackup.start()
        streakReminder.start()
        leagueSync.start()
        widgetSync.start()
        // Si Android cerró la app con un plan en curso, se retoma donde iba
        scope.launch { engine.ensureRestored() }
    }
}
