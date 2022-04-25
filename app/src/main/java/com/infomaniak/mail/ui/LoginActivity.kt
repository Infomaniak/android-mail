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
package com.infomaniak.mail.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.infomaniak.lib.core.InfomaniakCore
import com.infomaniak.lib.core.models.ApiResponse
import com.infomaniak.lib.core.models.user.User
import com.infomaniak.lib.core.networking.HttpClient
import com.infomaniak.lib.core.utils.clearStack
import com.infomaniak.lib.login.ApiToken
import com.infomaniak.lib.login.InfomaniakLogin
import com.infomaniak.mail.BuildConfig
import com.infomaniak.mail.R
import com.infomaniak.mail.data.api.ApiRepository
import com.infomaniak.mail.databinding.ActivityLoginBinding
import com.infomaniak.mail.ui.main.MainActivity
import com.infomaniak.mail.utils.AccountUtils
import com.infomaniak.mail.utils.showSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var infomaniakLogin: InfomaniakLogin

    private val webViewLoginResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        with(result) {
            if (resultCode == RESULT_OK) {
                val authCode = data?.extras?.getString(InfomaniakLogin.CODE_TAG)
                val translatedError = data?.extras?.getString(InfomaniakLogin.ERROR_TRANSLATED_TAG)
                when {
                    translatedError?.isNotBlank() == true -> showError(translatedError)
                    authCode?.isNotBlank() == true -> authenticateUser(authCode)
                    else -> showError(getString(R.string.anErrorHasOccurred))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        infomaniakLogin = InfomaniakLogin(context = this, appUID = BuildConfig.APPLICATION_ID, clientID = BuildConfig.CLIENT_ID)
        infomaniakLogin.startWebViewLogin(webViewLoginResultLauncher)
    }

    private fun authenticateUser(authCode: String) {
        lifecycleScope.launch {
            infomaniakLogin.getToken(
                okHttpClient = HttpClient.okHttpClientNoInterceptor,
                code = authCode,
                onSuccess = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        when (val user = authenticateUser(this@LoginActivity, it)) {
                            is User -> {
                                // application.trackCurrentUserId() // TODO: Matomo
                                // trackAccountEvent("loggedIn") // TODO: Matomo
                                launchMainActivity()
                            }
                            is ApiResponse<*> -> withContext(Dispatchers.Main) {
                                showError(getString(user.translatedError))
                            }
                            else -> withContext(Dispatchers.Main) { showError(getString(R.string.anErrorHasOccurred)) }
                        }
                    }
                },
                onError = {
                    val error = when (it) {
                        InfomaniakLogin.ErrorStatus.SERVER -> R.string.serverError
                        InfomaniakLogin.ErrorStatus.CONNECTION -> R.string.connectionError
                        else -> R.string.anErrorHasOccurred
                    }
                    showError(getString(error))
                },
            )
        }
    }

    private fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).clearStack())
    }

    private fun showError(error: String) {
        showSnackbar(error)
    }

    companion object {
        suspend fun authenticateUser(context: Context, apiToken: ApiToken): Any {

            AccountUtils.getUserById(apiToken.userId)?.let {
                return getErrorResponse(R.string.errorUserAlreadyPresent)
            } ?: run {
                InfomaniakCore.bearerToken = apiToken.accessToken
                val userProfileResponse = ApiRepository.getUserProfile(HttpClient.okHttpClientNoInterceptor)
                if (userProfileResponse.result == ApiResponse.Status.ERROR) {
                    return userProfileResponse
                } else {
                    val user: User? = userProfileResponse.data?.apply {
                        this.apiToken = apiToken
                        this.organizations = ArrayList()
                    }

                    return user?.let {
                        // DriveInfosController.storeDriveInfos(user.id, driveInfo) // TODO?
                        // CloudStorageProvider.notifyRootsChanged(context) // TODO?
                        AccountUtils.addUser(user)
                        user
                    } ?: run {
                        getErrorResponse(R.string.anErrorHasOccurred)
                    }
                }
            }
        }

        private fun getErrorResponse(@StringRes text: Int): ApiResponse<Any> {
            return ApiResponse(result = ApiResponse.Status.ERROR, translatedError = text)
        }
    }
}
