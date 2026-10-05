package com.jjas.labpomodoro

import android.app.Application
import com.jjas.labpomodoro.service.FocusSoundController
import com.jjas.labpomodoro.service.RewardSync
import com.jjas.labpomodoro.service.TimerEffects
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LabPomodoroApp : Application() {

    @Inject lateinit var timerEffects: TimerEffects

    @Inject lateinit var rewardSync: RewardSync

    @Inject lateinit var focusSound: FocusSoundController

    override fun onCreate() {
        super.onCreate()
        timerEffects.start()
        rewardSync.start()
        focusSound.start()
    }
}
