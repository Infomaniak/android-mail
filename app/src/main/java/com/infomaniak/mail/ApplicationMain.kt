/*
 * Infomaniak kMail - Android
 * Copyright (C) 2022 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.mail

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.StrictMode
import androidx.core.app.NotificationManagerCompat
import com.facebook.stetho.Stetho
import com.infomaniak.lib.core.InfomaniakCore
import com.infomaniak.lib.core.auth.TokenInterceptorListener
import com.infomaniak.lib.core.models.user.User
import com.infomaniak.lib.core.networking.HttpClient
import com.infomaniak.lib.core.utils.ApiController
import com.infomaniak.lib.core.utils.clearStack
import com.infomaniak.lib.login.ApiToken
import com.infomaniak.mail.data.models.Attachment
import com.infomaniak.mail.data.models.Folder
import com.infomaniak.mail.data.models.Recipient
import com.infomaniak.mail.data.models.message.Message
import com.infomaniak.mail.ui.LaunchActivity
import com.infomaniak.mail.utils.*
import com.infomaniak.mail.utils.NotificationUtils.initNotificationChannel
import com.infomaniak.mail.utils.NotificationUtils.showGeneralNotification
import io.realm.RealmInstant
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ApplicationMain : Application() {

    override fun onCreate() {
        super.onCreate()
        configureApiController()
        configureInfomaniakCore()
        configureHttpClient()
    }

    private fun configureApiController() {
        ApiController.init(
            arrayListOf(
                RealmInstant::class.java to RealmInstantConverter(),
                typeAdapterOfRealmListOf<Folder>(FolderRealmListConverter()),
                typeAdapterOfRealmListOf<Recipient>(RecipientRealmListConverter()),
                typeAdapterOfRealmListOf<Message>(MessageRealmListConverter()),
                typeAdapterOfRealmListOf<Attachment>(AttachmentRealmListConverter()),
                typeAdapterOfRealmListOf<String>(StringRealmListConverter()),
            )
        )
    }

    private fun configureInfomaniakCore() {
        InfomaniakCore.init(
            appVersionName = BuildConfig.VERSION_NAME,
            clientId = BuildConfig.CLIENT_ID,
            credentialManager = null,
            isDebug = BuildConfig.DEBUG,
        )
    }

    private fun configureHttpClient() {
        KMailHttpClient.onRefreshTokenError = refreshTokenError
        HttpClient.init(tokenInterceptorListener())
    }

    private val refreshTokenError: (User) -> Unit = { user ->
        val openAppIntent = Intent(this, LaunchActivity::class.java).clearStack()
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificationManagerCompat = NotificationManagerCompat.from(this)
        showGeneralNotification(getString(R.string.refreshTokenError)).apply {
            setContentIntent(pendingIntent)
            notificationManagerCompat.notify(UUID.randomUUID().hashCode(), build())
        }

        CoroutineScope(Dispatchers.IO).launch { AccountUtils.removeUser(this@ApplicationMain, user) }
    }

    private fun tokenInterceptorListener() = object : TokenInterceptorListener {
        override suspend fun onRefreshTokenSuccess(apiToken: ApiToken) {
            AccountUtils.setUserToken(AccountUtils.currentUser!!, apiToken)
        }

        override suspend fun onRefreshTokenError() {
            refreshTokenError(AccountUtils.currentUser!!)
        }

        override suspend fun getApiToken(): ApiToken = AccountUtils.currentUser!!.apiToken
    }
}
