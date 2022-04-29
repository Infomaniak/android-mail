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

import com.infomaniak.mail.data.api.ApiRepository
import com.infomaniak.mail.utils.ApiTestUtils.assertApiResponseData
import org.junit.Test

class ApiRepositoryTest : KMailTest() {

    private val getMailbox = run {
        beforeAll()
        ApiRepository.getMailboxes(okHttpClient)
    }
    private val mailbox = getMailbox.data?.firstOrNull()!!

    @Test
    fun getAddressBooks() {
        assertApiResponseData(ApiRepository.getAddressBooks())
    }

    @Test
    fun getContacts() {
        assertApiResponseData(ApiRepository.getContacts())
    }

    @Test
    fun getUser() {
        assertApiResponseData(ApiRepository.getUser())
    }

//    fun getContactImage(path: String): ApiResponse<Data> =
//        callApi(ApiRoutes.resource(path), GET)

    @Test
    fun getSignatures() {
        assertApiResponseData(ApiRepository.getSignatures(mailbox))
    }

    @Test
    fun getMailboxes() {
        assertApiResponseData(getMailbox)
    }

    @Test
    fun getFolders() {
        assertApiResponseData(ApiRepository.getFolders(mailbox))
    }

//    fun createFolder(mailbox: Mailbox, name: String, path: String?): ApiResponse<Folder> =
//        callApi(ApiRoutes.folders(mailbox.uuid), POST, mutableMapOf("name" to name).apply { path?.let { "path" to it } })

//    fun renameFolder(mailbox: Mailbox, folder: Folder, newName: String): ApiResponse<Folder> =
//        callApi(ApiRoutes.renameFolder(mailbox.uuid, folder.id), POST, mapOf("name" to newName))

//    fun favoriteFolder(mailbox: Mailbox, folder: Folder, favorite: Boolean): ApiResponse<Boolean> =
//        callApi(ApiRoutes.favoriteFolder(mailbox.uuid, folder.id, favorite), POST)

//    fun readFolder(mailbox: Mailbox, folder: Folder): ApiResponse<Boolean> =
//        callApi(ApiRoutes.readFolder(mailbox.uuid, folder.id), POST)

//    fun flushFolder(mailbox: Mailbox, folder: Folder): ApiResponse<Boolean> =
//        callApi(ApiRoutes.flushFolder(mailbox.uuid, folder.id), POST)

//    fun deleteFolder(mailbox: Mailbox, folder: Folder): ApiResponse<Boolean> =
//        callApi(ApiRoutes.folder(mailbox.uuid, folder.id), DELETE)

    @Test
    fun getThreads() {
//        assertApiResponseData(ApiRepository.getThreads(mailbox))
    }

    // fun getThreads(mailbox: Mailbox, folder: Folder, filter: Thread.ThreadFilter? = null)

    @Test
    fun getMessage() {
//        assertApiResponseData(ApiRepository.getMessage(mailbox))
    }

    @Test
    fun getQuotas() {
        assertApiResponseData(ApiRepository.getQuotas(mailbox))
    }

//    fun markAsSeen(mailbox: Mailbox, messages: ArrayList<Message>): ApiResponse<ArrayList<Seen>> =
//        callApi(ApiRoutes.messageSeen(mailbox.uuid), POST, mapOf("uids" to messages.map { it.uid }))

//    fun markAsUnseen(mailbox: Mailbox, messages: ArrayList<Message>): ApiResponse<ArrayList<Seen>> =
//        callApi(ApiRoutes.messageUnseen(mailbox.uuid), POST, mapOf("uids" to messages.map { it.uid }))

//    fun markAsSafe(mailbox: Mailbox, messages: ArrayList<Message>): ApiResponse<ArrayList<Seen>> =
//        callApi(ApiRoutes.messageSafe(mailbox.uuid), POST, mapOf("uids" to messages.map { it.uid }))

//    fun trustSender(mailbox: Mailbox, message: Message): ApiResponse<EmptyResponse> =
//        callApi(ApiRoutes.resource("${message.resource}/trustForm"), POST)

    @Test
    fun saveDraft() {
//        assertApiResponseData(ApiRepository.saveDraft(mailbox))
    }

    @Test
    fun send() {
//        assertApiResponseData(ApiRepository.send(mailbox))
    }

    @Test
    fun getDraft2() { // todo rename
//        assertApiResponseData(ApiRepository.getDraft(mailbox))
    }

    @Test
    fun deleteDraft() {
//        assertApiResponseData(ApiRepository.deleteDraft(mailbox))
    }

    @Test
    fun moveMessage() {
//        assertApiResponseData(ApiRepository.moveMessage(mailbox))
    }

//    fun createAttachment(
//        mailbox: Mailbox,
//        attachmentData: Data,
//        disposition: AttachmentDisposition,
//        attachmentName: String,
//        mimeType: String,
//    ): ApiResponse<Attachment> {
//        // TODO: Add headers
//        // let headers = ["x-ws-attachment-filename": attachmentName, "x-ws-attachment-mime-type": mimeType, "x-ws-attachment-disposition": disposition.rawValue]
//        return callApi(ApiRoutes.createAttachment(mailbox.uuid), POST, attachmentData)
//    }

    @Test
    fun getDraft() {
//        assertApiResponseData(ApiRepository.getDraft(mailbox))
    }

    @Test
    fun starMessage() {
//        assertApiResponseData(ApiRepository.starMessage(mailbox))
    }

//    fun search(mailbox: Mailbox, folder: Folder, searchText: String): ApiResponse<Thread> =
//        callApi(ApiRoutes.search(mailbox.uuid, folder.id, searchText), GET)
}
