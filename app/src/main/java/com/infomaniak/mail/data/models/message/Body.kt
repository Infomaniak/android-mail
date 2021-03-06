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
package com.infomaniak.mail.data.models.message

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.serialization.Serializable

// @RealmClass(embedded = true) // TODO: https://github.com/realm/realm-kotlin/issues/551
@Serializable
class Body : RealmObject {
    var value: String = ""
    var type: String = ""
    var subBody: String? = null

    /**
     * Local
     */
    @PrimaryKey
    var objectId: String = "" // TODO: Remove this when we have EmbeddedObjects

    // TODO: Remove this when we have EmbeddedObjects
    fun initLocalValues(completedMessageUid: String) {
        objectId = "body_${completedMessageUid}"
    }

    override fun equals(other: Any?): Boolean = other is Body && other.objectId == objectId

    override fun hashCode(): Int = objectId.hashCode()
}
