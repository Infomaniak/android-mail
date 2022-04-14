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
package com.infomaniak.mail.data.models

import com.google.gson.annotations.SerializedName

data class AddressBooksResult(
    @SerializedName("addressbooks")
    val addressBooks: ArrayList<AddressBook>,
    val default: AddressBook?,
) {
    data class AddressBook(
        val id: Int,
        @SerializedName("user_id")
        val userId: Int,
        @SerializedName("principal_uri")
        val principalUri: String,
        val name: String,
        val color: String,
        val uuid: String,
        val description: String,
        @SerializedName("is_shared")
        val isShared: Boolean,
        val rights: String,
        @SerializedName("is_activated")
        val isActivated: Boolean,
        @SerializedName("is_hidden")
        val isHidden: Boolean,
        @SerializedName("is_pending")
        val isPending: Boolean,
        // val categories: ArrayList<Category>,
    )
}