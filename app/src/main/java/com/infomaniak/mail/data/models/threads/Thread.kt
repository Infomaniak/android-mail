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
package com.infomaniak.mail.data.models.threads

import com.google.gson.annotations.SerializedName
import com.infomaniak.mail.data.models.Recipient
import com.infomaniak.mail.data.models.message.Message
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.realmListOf

class Thread : RealmObject {
    @PrimaryKey
    var uid: String = ""

    @SerializedName("messages_count")
    var messagesCount: Int = 0

    @SerializedName("unique_messages_count")
    var uniqueMessagesCount: Int = 0

    @SerializedName("deleted_messages_count")
    var deletedMessagesCount: Int = 0
    var messages: RealmList<Message> = realmListOf()

    @SerializedName("unseen_messages")
    var unseenMessages: Int = 0
    var from: RealmList<Recipient> = realmListOf()
    var cc: RealmList<Recipient> = realmListOf()
    var bcc: RealmList<Recipient> = realmListOf()
    var to: RealmList<Recipient> = realmListOf()
    var subject: String = ""
    var date: String = ""

    @SerializedName("has_attachments")
    var hasAttachments: Boolean = false

    @SerializedName("has_st_attachments")
    var hasStAttachments: Boolean = false

    @SerializedName("has_drafts")
    var hasDrafts: Boolean = false
    var flagged: Boolean = false
    var answered: Boolean = false
    var forwarded: Boolean = false
    var size: Int = 0

    enum class ThreadFilter(title: String) {
        ALL("All"),
        SEEN("Seen"),
        UNSEEN("Unseen"),
        STARRED("Starred"),
        UNSTARRED("Unstarred"),
    }
}