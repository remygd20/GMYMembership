package com.remy.gmymembership

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.remy.gmymembership.worker.MembershipCheckWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GMYMembershipApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleDailyMembershipCheck()
    }

    private fun scheduleDailyMembershipCheck() {
        val workManager = WorkManager.getInstance(this)

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<MembershipCheckWorker>(
            24, TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

        workManager.enqueueUniquePeriodicWork(
            MembershipCheckWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}
