package com.jjas.labpomodoro.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Solo debug: publica en el propio reloj el estado que mandaría el teléfono, para ver la app,
 * la vibración y la actividad en curso sin emparejar. Por adb:
 * `am start -n com.jjas.labpomodoro/com.jjas.labpomodoro.wear.FakePhoneActivity --es type WORK --ei left 754 --ez pro true --es shape ERLENMEYER --ei pomodoro 2`
 */
class FakePhoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val left = intent.getIntExtra("left", 754) * 1000L
        val request = PutDataMapRequest.create(WearContract.TIMER_PATH).apply {
            dataMap.putString(WearContract.KEY_STATE, intent.getStringExtra("state") ?: "ACTIVE")
            dataMap.putString(WearContract.KEY_TYPE, intent.getStringExtra("type") ?: "WORK")
            dataMap.putLong(WearContract.KEY_ENDS_AT, System.currentTimeMillis() + left)
            dataMap.putBoolean(WearContract.KEY_PAUSED, intent.getBooleanExtra("paused", false))
            dataMap.putLong(WearContract.KEY_REMAINING, left)
            dataMap.putLong(WearContract.KEY_DURATION, 25 * 60_000L)
            dataMap.putInt(WearContract.KEY_POMODORO, intent.getIntExtra("pomodoro", 1))
            dataMap.putInt(WearContract.KEY_POMODOROS, 5)
            dataMap.putInt(WearContract.KEY_COLOR, intent.getIntExtra("color", 0xFFE6197E.toInt()))
            dataMap.putString(WearContract.KEY_SHAPE, intent.getStringExtra("shape") ?: "BEAKER")
            dataMap.putBoolean(WearContract.KEY_PRO, intent.getBooleanExtra("pro", false))
        }
        lifecycleScope.launch {
            Wearable.getDataClient(this@FakePhoneActivity).putDataItem(request.asPutDataRequest().setUrgent()).await()
            finish()
        }
    }
}
