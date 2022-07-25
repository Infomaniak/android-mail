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
package com.infomaniak.mail.ui.main.thread

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.text.SpannedString
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.infomaniak.lib.core.utils.FormatterFileSize
import com.infomaniak.lib.core.utils.firstOrEmpty
import com.infomaniak.lib.core.utils.format
import com.infomaniak.lib.core.utils.loadAvatar
import com.infomaniak.mail.R
import com.infomaniak.mail.data.models.Attachment
import com.infomaniak.mail.data.models.Recipient
import com.infomaniak.mail.data.models.message.Body
import com.infomaniak.mail.data.models.message.Message
import com.infomaniak.mail.databinding.ItemMessageBinding
import com.infomaniak.mail.utils.*
import java.util.*
import com.infomaniak.lib.core.R as RCore

class ThreadAdapter(
    private var messageList: MutableList<Message> = mutableListOf(),
) : RecyclerView.Adapter<BindingViewHolder<ItemMessageBinding>>() {

    var onContactClicked: ((contact: Recipient, isExpanded: Boolean) -> Unit)? = null
    var onDeleteDraftClicked: ((message: Message) -> Unit)? = null
    var onDraftClicked: ((message: Message) -> Unit)? = null

    override fun getItemCount() = messageList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<ItemMessageBinding> {
        return BindingViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BindingViewHolder<ItemMessageBinding>, position: Int): Unit = with(holder.binding) {
        val message = messageList[position]
        if ((position == lastIndex() || !message.seen) && !message.isDraft) message.isExpanded = true

        loadBody(message.body)
        initHeader(message)
        initAttachment(message.attachments)

        handleHeaderClick(message)

        displayExpandedCollapsedMessage(message)
    }

    private fun ItemMessageBinding.displayExpandedCollapsedMessage(message: Message) {
        setHeaderState(message)
        if (message.isExpanded) displayAttachments(message.attachments) else hideAttachments()
        webViewFrameLayout.isVisible = message.isExpanded
    }

    private fun ItemMessageBinding.initAttachment(attachments: List<Attachment>) {
        val fileSize = formatAttachmentFileSize(attachments)
        attachmentsSizeText.text = context.resources.getQuantityString(
            R.plurals.attachmentQuantity,
            attachments.size,
            attachments.size,
        ) + " ($fileSize)"
        attachmentsRecyclerView.adapter = AttachmentAdapter(attachments)
        attachmentsDownloadAllButton.setOnClickListener {
            // TODO: AttachmentsList Fragment
        }
    }

    private fun ItemMessageBinding.setHeaderState(message: Message) = with(message) {
        deleteDraftButton.apply {
            isVisible = isDraft
            setOnClickListener { onDeleteDraftClicked?.invoke(this@with) }
        }
        replyButton.apply {
            isVisible = isExpanded
            setOnClickListener { /*TODO*/ }
        }
        menuButton.apply {
            isVisible = isExpanded
            setOnClickListener { /*TODO*/ }
        }

        recipient.text = if (isExpanded) formatRecipientsName(this@with) else subject
        recipientChevron.isVisible = isExpanded
        recipientOverlayedButton.isVisible = isExpanded
    }

    private fun ItemMessageBinding.initHeader(message: Message) {
        if (message.isDraft) {
            userAvatarImage.loadAvatar(AccountUtils.currentUser!!)
            expeditorName.apply {
                text = context.getString(R.string.messageIsDraftOption)
                setTextColor(context.getColor(R.color.draftTextColor))
            }
            messageDate.text = ""
        } else {
            val firstSender = message.from.first()
            userAvatarImage.loadAvatar(firstSender.email.hashCode(), null, firstSender.getNameOrEmail().firstOrEmpty().uppercase())
            expeditorName.apply {
                text = firstSender.displayedName(context)
                setTextColor(context.getColor(R.color.primaryTextColor))
            }
            messageDate.text = message.date?.let { mailFormattedDate(it.toDate()) } ?: ""
        }

        recipientOverlayedButton.setOnClickListener {
            recipientChevron.toggleChevron(false)
            Log.e("gibran", "displayExpandedHeader: Displaying details")
        }
    }

    private fun mailFormattedDate(date: Date): CharSequence = with(date) {
        return format(
            when {
                isToday() -> FORMAT_EMAIL_DATE_TODAY
                isYesterday() -> FORMAT_EMAIL_DATE_YESTERDAY
                isThisYear() -> FORMAT_EMAIL_DATE_THIS_YEAR
                else -> FORMAT_EMAIL_DATE_PAST_YEAR
            }
        )
    }

    private fun ItemMessageBinding.handleHeaderClick(message: Message) = with(message) {
        messageHeader.setOnClickListener {
            if (isExpanded) {
                isExpanded = false
                displayExpandedCollapsedMessage(this@with)
            } else {
                if (isDraft) {
                    onDraftClicked?.invoke(this@with)
                } else {
                    isExpanded = true
                    displayExpandedCollapsedMessage(this@with)
                }
            }
        }
    }

    fun removeMessage(message: Message) {
        val position = messageList.indexOf(message)
        messageList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun notifyAdapter(newList: MutableList<Message>) {
        DiffUtil.calculateDiff(MessageListDiffCallback(messageList, newList)).dispatchUpdatesTo(this)
        messageList = newList
    }

    fun lastIndex() = messageList.lastIndex


    private fun ItemMessageBinding.displayMessage(message: Message) = with(message) {
        displayHeader(message)
        hideAttachments()
        if (isExpanded) {
            displayAttachments(attachments)
            loadBody(body)
        }
    }

    private fun ItemMessageBinding.displayHeader(message: Message) = with(message) {
        deleteDraftButton.apply {
            isVisible = isDraft
            setOnClickListener { onDeleteDraftClicked?.invoke(message) }
        }

        messageDate.text = if (isDraft) "" else date?.toDate()?.format(FORMAT_EMAIL_DATE_SIMPLIFIED)

        expeditorName.apply {
            setTextColor(context.getColor(if (isDraft) R.color.draftTextColor else R.color.primaryTextColor))
            text = if (isDraft) {
                context.getString(R.string.messageIsDraftOption)
            } else {
                from.first().displayedName(context)
            }
        }

        webViewFrameLayout.isVisible = isExpanded
        recipient.text = if (isExpanded) formatRecipientsName(message) else Html.fromHtml(preview, Html.FROM_HTML_MODE_LEGACY)

        if (isExpanded) messageHeader.addExpandHeaderListener(this@displayHeader, message)
    }

    private fun ItemMessageBinding.formatRecipientsName(message: Message): SpannedString = with(message) {
        val to = recipientsToSpannedString(context, to)
        val cc = recipientsToSpannedString(context, cc)

        return buildSpannedString {
            if (isExpandedHeaderMode) scale(RECIPIENT_TEXT_SCALE_FACTOR) { append("${context.getString(R.string.toTitle)} ") }
            append(to)
            if (cc.isNotBlank()) append(cc)
        }.dropLast(2) as SpannedString
    }

    private fun Message.recipientsToSpannedString(context: Context, recipientsList: List<Recipient>) = buildSpannedString {
        recipientsList.forEach {
            if (isExpandedHeaderMode) {
                color(context.getColor(RCore.color.accent)) { append(it.displayedName(context)) }
                    .scale(RECIPIENT_TEXT_SCALE_FACTOR) { if (it.name?.isNotBlank() == true) append(" (${it.email})") }
                    .append(",\n")
            } else {
                append("${it.displayedName(context)}, ")
            }
        }
    }

    private fun View.addExpandHeaderListener(binding: ItemMessageBinding, message: Message) = with(message) {
        setOnClickListener {
            isExpandedHeaderMode = !isExpandedHeaderMode
            binding.expandHeader(message)
        }
    }

    private fun ItemMessageBinding.expandHeader(message: Message) = with(message) {
        recipient.maxLines = if (isExpandedHeaderMode) Int.MAX_VALUE else 1
        recipient.changeSize(if (isExpandedHeaderMode) R.dimen.textSmallSize else R.dimen.textHintSize)

        recipient.text = formatRecipientsName(message)
        // TODO: Add listener to name and email of all recipient ?
        userAvatar.setOnClickListener { onContactClicked?.invoke(from.first(), isExpandedHeaderMode) }
    }

    private fun TextView.changeSize(@DimenRes dimension: Int) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(dimension))
    }

    @SuppressLint("SetTextI18n")
    private fun ItemMessageBinding.displayAttachments(attachments: List<Attachment>) {
        if (attachments.isEmpty()) hideAttachments() else showAttachments()
    }

    private fun ItemMessageBinding.hideAttachments() {
        attachmentsGroup.isGone = true
        attachmentsRecyclerView.isGone = true
    }

    private fun ItemMessageBinding.showAttachments() {
        attachmentsGroup.isVisible = true
        attachmentsRecyclerView.isVisible = true
    }

    private fun ItemMessageBinding.formatAttachmentFileSize(attachments: List<Attachment>): String {
        if (attachments.isEmpty()) return ""

        val totalAttachmentsFileSizeInBytes: Long = attachments.map { attachment ->
            attachment.size.toLong()
        }.reduce { accumulator: Long, size: Long -> accumulator + size }

        return FormatterFileSize.formatShortFileSize(context, totalAttachmentsFileSizeInBytes)
    }

//    private fun ItemMessageBinding.createChip(attachmentName: String): Chip {
//        val layoutInflater = LayoutInflater.from(context)
//        val chip = layoutInflater.inflate(R.layout.chip_attachment, attachmentsChipGroup, false) as Chip
//
//        return chip.apply { text = attachmentName }
//    }

    private fun ItemMessageBinding.loadBody(body: Body?) {
        // TODO: Make prettier webview, Add button to hide / display the conversation inside message body like webapp ?
        body?.let { messageBody.loadDataWithBaseURL("", it.value, it.type, "utf-8", "") }
    }

    private fun Recipient.displayedName(context: Context): String {
        return if (AccountUtils.currentUser?.email == email) context.getString(R.string.contactMe) else getNameOrEmail()
    }

    private fun Recipient.getNameOrEmail() = name?.ifBlank { email } ?: email

    private class MessageListDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>,
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean = oldList[oldIndex].uid == newList[newIndex].uid

        override fun areContentsTheSame(oldIndex: Int, newIndex: Int): Boolean {
            val oldItem = oldList[oldIndex]
            val newItem = newList[newIndex]
            return oldItem.uid == newItem.uid
                    && oldItem.from == newItem.from
                    && oldItem.date == newItem.date
                    && oldItem.attachments == newItem.attachments
                    && oldItem.subject == newItem.subject
                    && oldItem.body == newItem.body
        }
    }

    private companion object {
        const val FORMAT_EMAIL_DATE_DETAILED = "d MMM yyyy, HH:mm"
        const val FORMAT_EMAIL_DATE_SIMPLIFIED = "d MMM HH:mm"

        const val FORMAT_EMAIL_DATE_TODAY = "HH:mm"
        const val FORMAT_EMAIL_DATE_YESTERDAY = "yesterday, $FORMAT_EMAIL_DATE_TODAY"
        const val FORMAT_EMAIL_DATE_THIS_YEAR = "d MMM HH:mm"
        const val FORMAT_EMAIL_DATE_PAST_YEAR = "d MMM yyyy, HH:mm"

        const val RECIPIENT_TEXT_SCALE_FACTOR = 0.9f
    }
}
