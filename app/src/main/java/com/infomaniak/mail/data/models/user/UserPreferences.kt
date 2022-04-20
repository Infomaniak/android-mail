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
package com.infomaniak.mail.data.models.user

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject

class UserPreferences : RealmObject {
    @SerializedName("thread_mode")
    private var _threadMode: String? = null

    @SerializedName("read_pos")
    var readPosition: String = ""
    var density: String = ""
    // TODO: Add other preferences.

    fun getThreadMode(): ThreadMode? = when (_threadMode) {
        ThreadMode.MESSAGES.name -> ThreadMode.MESSAGES
        ThreadMode.THREADS.name -> ThreadMode.THREADS
        else -> null
    }

    enum class ThreadMode {
        MESSAGES,
        THREADS,
    }
}