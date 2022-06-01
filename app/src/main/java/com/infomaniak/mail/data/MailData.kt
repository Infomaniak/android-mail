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
package com.infomaniak.mail.data

import android.util.Log
import com.infomaniak.mail.data.api.MailApi
import com.infomaniak.mail.data.cache.MailRealm
import com.infomaniak.mail.data.cache.MailboxContentController
import com.infomaniak.mail.data.cache.MailboxInfoController
import com.infomaniak.mail.data.models.AppSettings
import com.infomaniak.mail.data.models.Folder
import com.infomaniak.mail.data.models.Folder.FolderRole
import com.infomaniak.mail.data.models.Mailbox
import com.infomaniak.mail.data.models.message.Message
import com.infomaniak.mail.data.models.thread.Thread
import com.infomaniak.mail.utils.AccountUtils
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.toRealmList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

object MailData {

    private val DEFAULT_FOLDER_ROLE = FolderRole.INBOX

    private val mutableMailboxesFlow: MutableStateFlow<List<Mailbox>?> = MutableStateFlow(null)
    val mailboxesFlow = mutableMailboxesFlow.asStateFlow().filterNotNull()

    private val mutableFoldersFlow: MutableStateFlow<List<Folder>?> = MutableStateFlow(null)
    val foldersFlow = mutableFoldersFlow.asStateFlow().filterNotNull()

    private val mutableThreadsFlow: MutableStateFlow<List<Thread>?> = MutableStateFlow(null)
    val threadsFlow = mutableThreadsFlow.asStateFlow().filterNotNull()

    private val mutableMessagesFlow: MutableStateFlow<List<Message>?> = MutableStateFlow(null)
    val messagesFlow = mutableMessagesFlow.asStateFlow().filterNotNull()

    var currentMailbox: Mailbox? = null
    var currentFolder: Folder? = null
    var currentThread: Thread? = null

    ///////////////
    // LOAD MAIL //
    ///////////////

    fun loadMailData() {
        readRealmData()
    }

    fun loadMessages(thread: Thread) {
        readMessages(thread)
    }

    private fun selectMailbox(mailbox: Mailbox) {
        if (MailRealm.currentMailboxObjectIdFlow.value != mailbox.objectId) {
            AccountUtils.currentMailboxId = mailbox.mailboxId
            MailRealm.mutableCurrentMailboxObjectIdFlow.value = mailbox.objectId
            currentMailbox = mailbox
        }
    }

    private fun selectFolder(folder: Folder) {
        MailRealm.mutableCurrentFolderIdFlow.value = folder.id
        currentFolder = folder
    }

    fun selectThread(thread: Thread) {
        MailRealm.mutableCurrentThreadUidFlow.value = thread.uid
        currentThread = thread
    }

    private fun handleMailboxes(mailboxes: List<Mailbox>, loadFolders: (mailbox: Mailbox) -> Unit) {
        val mailbox = with(mailboxes) {
            find { it.mailboxId == AccountUtils.currentMailboxId }
            // ?: find { it.email == "kevin.boulongne@ik.me" } // TODO: Remove this, it's for dev only
                ?: find { it.email == "kevin.boulongne@infomaniak.com" } // TODO: Remove this, it's for dev only
                ?: firstOrNull()
                ?: return
        }

        selectMailbox(mailbox)
        loadFolders(mailbox)
    }

    private fun handleFolders(folders: List<Folder>, loadThreads: (folder: Folder) -> Unit) {
        val folder = with(folders) {
            find { it.role == DEFAULT_FOLDER_ROLE }
                ?: firstOrNull()
                ?: return
        }

        selectFolder(folder)
        loadThreads(folder)
    }

    ////////////////
    // READ REALM //
    ////////////////

    private fun readRealmData() {
        readMailboxes()
    }

    private fun readMailboxes() {
        var job: Job? = null
        job = CoroutineScope(Dispatchers.IO).launch {
            MailRealm.readMailboxes().collect {

                val realmMailboxes = it.list.toList()
                mutableMailboxesFlow.value = realmMailboxes

                if (realmMailboxes.isEmpty()) {
                    fetchApiData()
                } else {
                    handleMailboxes(realmMailboxes) {
                        readFolders()
                    }
                }

                job?.cancel()
            }
        }
    }

    private fun readFolders() {
        var job: Job? = null
        job = CoroutineScope(Dispatchers.IO).launch {
            MailRealm.readFolders().collectLatest {

                val realmFolders = it.list.toList()
                mutableFoldersFlow.value = realmFolders

                if (realmFolders.isEmpty()) {
                    fetchApiData()
                } else {
                    handleFolders(realmFolders) { folder ->
                        readThreads(folder)
                    }
                }

                job?.cancel()
            }
        }
    }

    private fun readThreads(folder: Folder) {
        val realmThreads = MailRealm.readThreads(folder)
        mutableThreadsFlow.value = realmThreads

        fetchApiData()
    }

    private fun readMessages(thread: Thread) {
        val realmMessages = MailRealm.readMessages(thread)
        mutableMessagesFlow.value = realmMessages

        fetchMessages(thread)
    }

    ///////////////
    // FETCH API //
    ///////////////

    private fun fetchApiData() {
        fetchMailboxes()
    }

    private fun fetchMailboxes() {
        val realmMailboxes = mutableMailboxesFlow.value
        val apiMailboxes = MailApi.fetchMailboxes()
        val mergedMailboxes = mergeMailboxes(realmMailboxes, apiMailboxes)

        mutableMailboxesFlow.value = mergedMailboxes

        handleMailboxes(mergedMailboxes) { mailbox ->
            fetchFolders(mailbox)
        }
    }

    private fun fetchFolders(mailbox: Mailbox) {
        val realmFolders = mutableFoldersFlow.value
        val apiFolders = MailApi.fetchFolders(mailbox)
        val mergedFolders = mergeFolders(realmFolders, apiFolders)

        mutableFoldersFlow.value = mergedFolders

        handleFolders(mergedFolders) { folder ->
            fetchThreads(folder, mailbox)
        }
    }

    fun fetchThreads(folder: Folder, mailbox: Mailbox) {
        CoroutineScope(Dispatchers.IO).launch {
            val realmThreads = mutableThreadsFlow.value
            val apiThreads = MailApi.fetchThreads(folder, mailbox.uuid)
            val mergedThreads = mergeThreads(realmThreads, apiThreads, folder)

            mutableThreadsFlow.value = mergedThreads
        }
    }

    private fun fetchMessages(thread: Thread) {
        CoroutineScope(Dispatchers.IO).launch {
            val realmMessages = mutableMessagesFlow.value
            val apiMessages = MailApi.fetchMessages(thread)
            val mergedMessages = mergeMessages(realmMessages, apiMessages)

            mutableMessagesFlow.value = mergedMessages
        }
    }

    private fun mergeMailboxes(realmMailboxes: List<Mailbox>?, apiMailboxes: List<Mailbox>?): List<Mailbox> {
        if (apiMailboxes == null) return realmMailboxes ?: emptyList()

        // Get outdated data
        Log.d("API", "Mailboxes: Get outdated data")
        // val deletableMailboxes = MailboxInfoController.getDeletableMailboxes(apiMailboxes)
        val deletableMailboxes = realmMailboxes?.filter { realmMailbox ->
            !apiMailboxes.any { apiMailbox -> apiMailbox.mailboxId == realmMailbox.mailboxId }
        } ?: emptyList()

        // Save new data
        Log.d("API", "Mailboxes: Save new data")
        MailboxInfoController.upsertMailboxes(apiMailboxes)

        // Delete outdated data
        Log.d("API", "Mailboxes: Delete outdated data")
        val isCurrentMailboxDeleted = deletableMailboxes.any { it.mailboxId == AccountUtils.currentMailboxId }
        if (isCurrentMailboxDeleted) {
            MailRealm.closeMailboxContent()
            AccountUtils.currentMailboxId = AppSettings.DEFAULT_ID
        }
        MailboxInfoController.deleteMailboxes(deletableMailboxes)
        deletableMailboxes.forEach { Realm.deleteRealm(MailRealm.getMailboxConfiguration(it.mailboxId)) }

        return if (isCurrentMailboxDeleted) {
            AccountUtils.reloadApp()
            emptyList()
        } else {
            apiMailboxes
        }
    }

    private fun mergeFolders(realmFolders: List<Folder>?, apiFolders: List<Folder>?): List<Folder> {
        if (apiFolders == null) return realmFolders ?: emptyList()

        // Get outdated data
        Log.d("API", "Folders: Get outdated data")
        // val deletableFolders = MailboxContentController.getDeletableFolders(foldersFromApi)
        val deletableFolders = realmFolders?.filter { realmFolder ->
            !apiFolders.any { apiFolder -> apiFolder.id == realmFolder.id }
        } ?: emptyList()
        val possiblyDeletableThreads = deletableFolders.flatMap { it.threads }
        val deletableMessages = possiblyDeletableThreads.flatMap { it.messages }.filter { message ->
            deletableFolders.any { folder -> folder.id == message.folderId }
        }
        val deletableThreads = possiblyDeletableThreads.filter { thread ->
            thread.messages.all { message -> deletableMessages.any { it.uid == message.uid } }
        }

        // Save new data
        Log.d("API", "Folders: Save new data")
        MailRealm.mailboxContent.writeBlocking {
            apiFolders.forEach { apiFolder ->
                realmFolders?.find { it.id == apiFolder.id }?.threads
                    ?.mapNotNull(::findLatest)
                    ?.let { apiFolder.threads = it.toRealmList() }
                copyToRealm(apiFolder, UpdatePolicy.ALL)
            }
        }

        // Delete outdated data
        Log.d("API", "Folders: Delete outdated data")
        MailboxContentController.deleteMessages(deletableMessages)
        MailboxContentController.deleteThreads(deletableThreads)
        MailboxContentController.deleteFolders(deletableFolders)

        return apiFolders
    }

    private fun mergeThreads(realmThreads: List<Thread>?, apiThreads: List<Thread>?, folder: Folder): List<Thread> {
        if (apiThreads == null) return realmThreads ?: emptyList()

        // Get outdated data
        Log.d("API", "Threads: Get outdated data")
        // val deletableThreads = MailboxContentController.getDeletableThreads(threadsFromApi)
        val deletableThreads = realmThreads?.filter { fromRealm ->
            !apiThreads.any { fromApi -> fromApi.uid == fromRealm.uid }
        } ?: emptyList()
        val deletableMessages = deletableThreads.flatMap { thread -> thread.messages.filter { it.folderId == folder.id } }

        // Save new data
        Log.d("API", "Threads: Save new data")
        folder.threads = apiThreads.toRealmList()
        // TODO: Put this back (and make it work) when we have EmbeddedObjects
        // folder.threads = apiThreads.map { apiThread ->
        //     realmThreads?.find { it.uid == apiThread.uid }?.also { realmThread ->
        //         apiThread.messages.forEach { apiMessage ->
        //             realmThread.messages.find { it.uid == apiMessage.uid }?.also { realmMessage ->
        //                 apiMessage.apply {
        //                     fullyDownloaded = realmMessage.fullyDownloaded
        //                     body = realmMessage.body
        //                     attachments = realmMessage.attachments
        //                 }
        //             }
        //         }
        //     } ?: apiThread
        // }.toRealmList()
        MailboxContentController.upsertFolder(folder)

        // Delete outdated data
        Log.d("API", "Threads: Delete outdated data")
        MailboxContentController.deleteMessages(deletableMessages)
        MailboxContentController.deleteThreads(deletableThreads)

        return apiThreads
    }

    private fun mergeMessages(realmMessages: List<Message>?, apiMessages: List<Message>): List<Message> {

        // Get outdated data
        Log.d("API", "Messages: Get outdated data")
        // val deletableMessages = MailboxContentController.getDeletableMessages(messagesFromApi)
        val deletableMessages = realmMessages?.filter { realmMessage ->
            !apiMessages.any { apiMessage -> apiMessage.uid == realmMessage.uid }
        } ?: emptyList()

        // Save new data
        Log.d("API", "Messages: Save new data")
        MailboxContentController.upsertMessages(apiMessages)

        // Delete outdated data
        Log.d("API", "Messages: Delete outdated data")
        MailboxContentController.deleteMessages(deletableMessages)

        return apiMessages
    }
}
