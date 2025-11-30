package com.remy.gmymembership.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.remy.gmymembership.MainActivity
import com.remy.gmymembership.R
import com.remy.gmymembership.model.Member
import com.remy.gmymembership.ui.settings.SettingsFragment
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MembershipCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Comprobar la preferencia del usuario
        val sharedPreferences = context.getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPreferences.getBoolean(SettingsFragment.NOTIFICATIONS_ENABLED_KEY, true)

        // Si el usuario ha desactivado las notificaciones, nos detenemos aquí.
        if (!notificationsEnabled) {
            return Result.success()
        }

        // 2. Si están activadas, continuamos con la lógica de siempre
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.failure()

        try {
            val snapshots = db.collection("users").document(userId).collection("members").get().await()
            if (snapshots.isEmpty) return Result.success()

            var porVencer = 0
            var vencidos = 0

            val hoy = Calendar.getInstance()
            limpiarHora(hoy)
            val diasDesdeEpochHoy = TimeUnit.DAYS.convert(hoy.timeInMillis, TimeUnit.MILLISECONDS)

            for (doc in snapshots) {
                val member = doc.toObject(Member::class.java)
                if (member.registrationDate != null) {
                    val fechaVenc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    fechaVenc.time = member.registrationDate!!.toDate()
                    fechaVenc.add(Calendar.DAY_OF_YEAR, member.planDurationDays)
                    limpiarHora(fechaVenc)

                    val diasDesdeEpochVenc = TimeUnit.DAYS.convert(fechaVenc.timeInMillis, TimeUnit.MILLISECONDS)
                    val diasRestantes = diasDesdeEpochVenc - diasDesdeEpochHoy

                    when {
                        diasRestantes in 0..5 -> porVencer++
                        diasRestantes < 0 -> vencidos++
                    }
                }
            }

            if (porVencer > 0 || vencidos > 0) {
                sendNotification(porVencer, vencidos)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun sendNotification(porVencer: Int, vencidos: Int) {
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Resumen Diario de Membresías",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones diarias sobre membresías vencidas y por vencer."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationText = "Hoy tienes $porVencer membresías por vencer y $vencidos vencidas."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_home_24)
            .setContentTitle("Resumen de Membresías")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun limpiarHora(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "GMY_MEMBERSHIP_CHANNEL"
        const val UNIQUE_WORK_NAME = "MembershipCheckDailyWork"
    }
}
