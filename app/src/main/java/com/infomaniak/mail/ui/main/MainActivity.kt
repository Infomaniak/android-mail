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
package com.infomaniak.mail.ui.main

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.infomaniak.mail.R
import com.infomaniak.mail.data.api.ApiRepository
import com.infomaniak.mail.data.cache.MailboxController
import com.infomaniak.mail.data.cache.MailboxInfosController
import com.infomaniak.mail.data.models.Folder
import com.infomaniak.mail.utils.AccountUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.startCalls).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {

                val getMailboxes = ApiRepository.getMailboxes()
                Log.i("API", "getMailboxes: $getMailboxes")
                getMailboxes.data!!.forEach { MailboxInfosController.upsertMailboxInfos(it.initLocalValues()) }

                val mailbox = getMailboxes.data!!.first()
                AccountUtils.currentMailboxId = mailbox.mailboxId
                val getFolders = ApiRepository.getFolders(mailbox)
                Log.i("API", "getFolders: $getFolders")
                getFolders.data!!.forEach { MailboxController.upsertFolder(it) }

                val inbox = getFolders.data?.find { it.getRole() == Folder.FolderRole.INBOX }!!
                val getInboxThreads = ApiRepository.getThreads(mailbox, inbox, null)
                Log.i("API", "getInboxThreads: $getInboxThreads")
                getInboxThreads.data!!.threads.forEach { MailboxController.upsertThread(it) }

                val message = getInboxThreads.data!!.threads.first().messages.first()
                val getMessage = ApiRepository.getMessage(message)
                Log.i("API", "getMessage: $getMessage")
                MailboxController.upsertMessage(getMessage.data!!)

                // val moveMessageResponse = ApiRepository.moveMessage(mailbox, arrayListOf(message), inbox.id)
                // Log.i("API", "moveMessageResponse: $moveMessageResponse")

                // val starMessageResponse = ApiRepository.starMessage(true, mailbox, arrayListOf(message.uid))
                // Log.i("API", "starMessageResponse: $starMessageResponse")

                val getQuotas = ApiRepository.getQuotas(mailbox)
                Log.i("API", "getQuotas: $getQuotas")

                val getAddressBooks = ApiRepository.getAddressBooks()
                Log.i("API", "getAddressBooks: $getAddressBooks")

                val getContacts = ApiRepository.getContacts()
                Log.i("API", "getContacts: $getContacts")
                // getContacts.data?.forEach { Log.e("Contacts", "${it.getContactedTimes()}") }

                val getUser = ApiRepository.getUser()
                Log.i("API", "getUser: $getUser")

                val getSignature = ApiRepository.getSignatures(mailbox)
                Log.i("API", "getSignature: $getSignature")

                val drafts = getFolders.data?.find { it.getRole() == Folder.FolderRole.DRAFT }!!
                val getDraftsThreads = ApiRepository.getThreads(mailbox, drafts, null)
                Log.i("API", "getDraftsThreads: $getDraftsThreads")

                val draftMessage = getDraftsThreads.data?.threads?.find { it.hasDrafts }?.messages?.find { it -> it.isDraft }
                draftMessage?.let {
                    val draftUuid = draftMessage.draftResource.substringAfterLast('/')
                    val getDraftFromUuid = ApiRepository.getDraft(mailbox, draftUuid)
                    Log.i("API", "getDraftFromUuid: $getDraftFromUuid")
                    val getDraftFromMessage = ApiRepository.getDraft(draftMessage)
                    Log.i("API", "getDraftFromMessage: $getDraftFromMessage")

                    // val deleteDraft = ApiRepository.deleteDraft(mailbox, draftUuid)
                    // Log.i("API", "deleteDraft: $deleteDraft")
                }
            }
        }
    }
}