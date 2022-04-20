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

open class UserInfos(
    var name: String = "",
    @SerializedName("firstname")
    var firstName: String = "",
    var login: String = "",
    var email: String = "",
    @SerializedName("avatar_url")
    var avatarUrl: String = "",
    @SerializedName("hosting_url")
    var hostingUrl: String = "",
    @SerializedName("manager_url")
    var managerUrl: String = "",
    @SerializedName("workspace_url")
    var workspaceUrl: String = "",
    @SerializedName("drive_url")
    var driveUrl: String = "",
    @SerializedName("workspace_only")
    var workspaceOnly: Boolean = false,
    @SerializedName("double_auth")
    var doubleAuth: Boolean = false,
    @SerializedName("old_user")
    var oldUser: Boolean = false,
    var locale: String = "",
    var country: String = "",
    @SerializedName("is_restricted")
    var isRestricted: Boolean = false,
    @SerializedName("from_webmail1")
    var fromWebmail1: Boolean = false,
) : RealmObject()