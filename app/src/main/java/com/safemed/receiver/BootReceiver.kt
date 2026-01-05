package com.safemed.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safemed.alarm.ReminderAlarmManager
import com.safemed.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver để reschedule tất cả alarms sau khi device boot
 * Android hủy tất cả alarms khi device reboot, cần phải schedule lại
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var reminderAlarmManager: ReminderAlarmManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d(TAG, "Device booted or app updated, rescheduling reminders...")

            scope.launch {
                try {
                    val result = reminderRepository.getActiveReminders()
                    result.onSuccess { reminders ->
                        Log.d(TAG, "Found ${reminders.size} active reminders to reschedule")
                        reminderAlarmManager.rescheduleAllReminders(reminders)
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to get active reminders", error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminders after boot", e)
                }
            }
        }
    }
}
