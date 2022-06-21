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
package com.infomaniak.mail.ui.main.menu

import android.content.Context
import androidx.lifecycle.ViewModel
import com.infomaniak.mail.data.MailData
import com.infomaniak.mail.data.models.Folder
import com.infomaniak.mail.data.models.Mailbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MenuDrawerViewModel : ViewModel() {

    private var listenToMailboxesJob: Job? = null
    private var listenToFoldersJob: Job? = null

    private val mutableUiMailboxesFlow: MutableStateFlow<List<Mailbox>?> = MutableStateFlow(null)
    val uiMailboxesFlow = mutableUiMailboxesFlow.asStateFlow()

    private val mutableUiFoldersFlow: MutableStateFlow<List<Folder>?> = MutableStateFlow(null)
    val uiFoldersFlow = mutableUiFoldersFlow.asStateFlow()

    fun setup() {
        listenToMailboxes()
        listenToFolders()
    }

    private fun listenToMailboxes() {
        if (listenToMailboxesJob != null) listenToMailboxesJob?.cancel()

        listenToMailboxesJob = CoroutineScope(Dispatchers.IO).launch {
            MailData.mailboxesFlow.filterNotNull().collect { mailboxes ->
                mutableUiMailboxesFlow.value = mailboxes
            }
        }
    }

    private fun listenToFolders() {
        if (listenToFoldersJob != null) listenToFoldersJob?.cancel()

        listenToFoldersJob = CoroutineScope(Dispatchers.IO).launch {
            MailData.foldersFlow.filterNotNull().collect { folders ->
                mutableUiFoldersFlow.value = folders
            }
        }
    }

    fun openFolder(folderName: String, context: Context) {
        var job: Job? = null
        job = CoroutineScope(Dispatchers.IO).launch {
            MailData.foldersFlow.filterNotNull().collect { folders ->
                MailData.currentMailboxFlow.value?.let { mailbox ->
                    folders.find { it.getLocalizedName(context) == folderName }?.let { folder ->
                        MailData.selectFolder(folder)
                        MailData.loadThreads(folder, mailbox)
                    }
                }
                job?.cancel()
            }
        }
    }

    override fun onCleared() {
        listenToMailboxesJob?.cancel()
        listenToFoldersJob?.cancel()

        listenToMailboxesJob = null
        listenToFoldersJob = null

        super.onCleared()
    }
}
