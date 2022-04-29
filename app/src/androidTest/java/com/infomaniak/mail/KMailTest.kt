/*
 * Infomaniak kDrive - Android
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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.permission.PermissionRequester
import com.infomaniak.lib.core.InfomaniakCore
import com.infomaniak.lib.core.models.user.User
import com.infomaniak.lib.core.networking.HttpClient
import com.infomaniak.lib.login.ApiToken
import com.infomaniak.mail.data.api.ApiRepository
import com.infomaniak.mail.utils.AccountUtils
import com.infomaniak.mail.utils.AccountUtils.addUser
import com.infomaniak.mail.utils.AccountUtils.getUserById
import com.infomaniak.mail.utils.ApiTestUtils.assertApiResponseData
import com.infomaniak.mail.utils.Env
import com.infomaniak.mail.utils.KMailHttpClient
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

open class KMailTest {

    companion object {

        internal const val APP_PACKAGE = BuildConfig.APPLICATION_ID
        internal val context = ApplicationProvider.getApplicationContext<Context>()
        internal lateinit var okHttpClient: OkHttpClient
        internal lateinit var uiRealm: Realm
        internal lateinit var user: User
        // internal lateinit var userDrive: UserDrive

        // @BeforeAll
        // @JvmStatic
        fun beforeAll() {
            if (Env.USE_CURRENT_USER) {
                user = runBlocking(Dispatchers.IO) { AccountUtils.requestCurrentUser() }!!
                InfomaniakCore.bearerToken = user.apiToken.accessToken
            } else {
                InfomaniakCore.bearerToken = Env.TOKEN

                val apiResponse = ApiRepository.getUserProfile(HttpClient.okHttpClientNoInterceptor)
                assertApiResponseData(apiResponse)
                user = apiResponse.data!!
                user.apiToken = ApiToken(Env.TOKEN, "", "Bearer", userId = user.id, expiresAt = null)

                runBlocking {
                    if (getUserById(user.id) == null) {
                        user.organizations = arrayListOf()
                        addUser(user)
                    } else {
                        AccountUtils.currentUser = user
                    }
                }
            }

            // userDrive = UserDrive(user.id, Env.DRIVE_ID)
            okHttpClient = runBlocking { KMailHttpClient.getHttpClient(user.id) }

            // setUpRealm()
            // grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // @AfterAll
        // @JvmStatic
        // fun afterAll() {
        //     ApiRepository.emptyTrash(userDrive.driveId)
        //     if (!uiRealm.isClosed) uiRealm.close()
        //     Realm.deleteRealm(FileController.getRealmConfiguration(userDrive))
        //     if (!Env.USE_CURRENT_USER) {
        //         runBlocking { AccountUtils.removeUser(context, user) }
        //     }
        // }

        // internal fun getRealmConfigurationTest() = RealmConfiguration.Builder().inMemory()
        //     .name("KDrive-test.realm")
        //     .deleteRealmIfMigrationNeeded()
        //     .modules(RealmModules.LocalFilesModule())
        //     .build()

        // private fun setUpRealm() {
        //     try {
        //         uiRealm = FileController.getRealmInstance(userDrive)
        //     } catch (realmFileException: RealmFileException) {
        //         java.io.File(FileController.getRealmConfiguration(userDrive).path).apply {
        //             if (exists()) {
        //                 delete()
        //                 uiRealm = FileController.getRealmInstance(userDrive)
        //             } else {
        //                 realmFileException.printStackTrace()
        //                 assert(false) { "realmFileException thrown" }
        //                 throw realmFileException
        //             }
        //         }
        //     }
        // }

        private fun grantPermissions(vararg permissions: String) {
            PermissionRequester().apply {
                addPermissions(*permissions)
                requestPermissions()
            }
        }
    }
}
