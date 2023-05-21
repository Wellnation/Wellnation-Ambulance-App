package com.wellnation.ambulanceportal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle the received message
        if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            Log.d("notification",title.toString())
            // Customize this to handle the notification based on your requirements
            showNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
//        sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE)
//        val id = sharedPreferences.getString("ambulanceid", " ")
//        Log.d("id from Sharepref", id.toString())
//        id?.let { FirebaseFirestore.getInstance().collection("ambulance").document(it) }
//            ?.update("fmcToken", token)
        // Update the token in your server if needed
        // Implement your logic to send the new token to your server
    }

    private fun showNotification(title: String?, body: String?) {
        val channelId = "MyChannelId"
        val channelName = "MyChannelName"
        val notificationId = 123 // Choose any unique notification ID

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification) // Customize this with your app's notification icon
            .setAutoCancel(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
