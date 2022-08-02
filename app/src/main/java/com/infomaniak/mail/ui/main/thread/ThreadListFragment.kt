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

import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.imageLoader
import com.infomaniak.lib.core.utils.Utils
import com.infomaniak.lib.core.utils.loadAvatar
import com.infomaniak.lib.core.utils.safeNavigate
import com.infomaniak.lib.core.utils.setPagination
import com.infomaniak.mail.R
import com.infomaniak.mail.data.api.ApiRepository.OFFSET_FIRST_PAGE
import com.infomaniak.mail.data.api.ApiRepository.PER_PAGE
import com.infomaniak.mail.data.models.Folder
import com.infomaniak.mail.data.models.thread.Thread
import com.infomaniak.mail.databinding.FragmentThreadListBinding
import com.infomaniak.mail.ui.main.MainActivity
import com.infomaniak.mail.ui.main.MainViewModel
import com.infomaniak.mail.ui.main.MainViewModel.Companion.currentOffset
import com.infomaniak.mail.utils.AccountUtils
import com.infomaniak.mail.utils.context
import com.infomaniak.mail.utils.observeNotNull
import kotlinx.coroutines.*
import java.util.*

class ThreadListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: ThreadListViewModel by viewModels()

    private lateinit var binding: FragmentThreadListBinding

    private var updatedAtRefreshJob: Job? = null

    private var threadListAdapter = ThreadListAdapter()

    private val showLoadingTimer: CountDownTimer by lazy {
        Utils.createRefreshTimer(
            // TODO: Remove & fix this. It's been put there because currently, it seems that when the refresh API call is shorter
            // TODO: than 600ms, the spinning thingy never disappears (because it appears AFTER that everything is finished).
            milliseconds = 0L,
        ) { binding.swipeRefreshLayout.isRefreshing = true }
    }

    private var isDownloadingChanges = false
    private var lastUpdatedAt = Date() // TODO: Remove when implementing "Last updated at" feature

    private companion object {
        const val OFFSET_TRIGGER = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentThreadListBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lastUpdatedAt = Date()
        startPeriodicRefreshJob()

        setupOnRefresh()
        setupAdapter()
        setupListeners()
        setupUserAvatar()
        setupUnreadCountChip()

        listenToCurrentFolder()
        listenToThreads()
    }

    private fun setupUnreadCountChip() {
        binding.unreadCountChip.apply {
            isCloseIconVisible = false
            setOnCheckedChangeListener { _, isChecked ->
                isCloseIconVisible = isChecked
            }
        }
    }

    private fun setupOnRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this)
    }

    override fun onRefresh() {
        currentOffset = OFFSET_FIRST_PAGE
        mainViewModel.forceRefreshThreads()
    }

    private fun startPeriodicRefreshJob() {
        updatedAtRefreshJob?.cancel()
        updatedAtRefreshJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) { updateUpdatedAt() }
                delay(DateUtils.MINUTE_IN_MILLIS)
            }
        }
    }

    private fun updateUpdatedAt() = with(binding) {
        // TODO : Replace lastUpdatedAt.time with currentFolder.lastUpdatedAt ?
        val ago = if (Date().time - lastUpdatedAt.time < DateUtils.MINUTE_IN_MILLIS) {
            getString(R.string.threadListHeaderLastUpdateNow)
        } else {
            DateUtils.getRelativeTimeSpanString(lastUpdatedAt.time).toString().replaceFirstChar { it.lowercaseChar() }
        }
        updatedAt.text = getString(R.string.threadListHeaderLastUpdate, ago)
    }

    private fun updateUnreadCount() = with(binding.unreadCountChip) {
        // TODO: Fetch folder again to update it.
        val unreadCount = MainViewModel.currentFolderFlow.value?.unreadCount ?: 0
        text = resources.getQuantityString(R.plurals.threadListHeaderUnreadCount, unreadCount, unreadCount)
        isVisible = unreadCount > 0
    }

    private fun setupAdapter() {
        binding.threadsList.adapter = threadListAdapter

        mainViewModel.isInternetAvailable.observe(viewLifecycleOwner) { isInternetAvailable ->
            // TODO: Manage no Internet screen
            // threadAdapter.toggleOfflineMode(requireContext(), !isInternetAvailable)
            // binding.noNetwork.isGone = isInternetAvailable
        }

        threadListAdapter.apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            // onEmptyList = { checkIfNoFiles() }

            onThreadClicked = {
                safeNavigate(
                    ThreadListFragmentDirections.actionThreadListFragmentToThreadFragment(
                        threadUid = it.uid,
                        threadSubject = it.subject,
                        threadIsFavorite = it.flagged
                    )
                )
            }
        }
    }

    private fun setupListeners() = with(binding) {
        // TODO: Multiselect
        // openMultiselectButton.setOnClickListener {}

        toolbar.setNavigationOnClickListener { (activity as? MainActivity)?.binding?.drawerLayout?.open() }

        searchButton.setOnClickListener {
            safeNavigate(ThreadListFragmentDirections.actionThreadListFragmentToSearchFragment())
        }

        userAvatar.setOnClickListener {
            safeNavigate(ThreadListFragmentDirections.actionThreadListFragmentToSwitchUserFragment())
        }

        newMessageFab.setOnClickListener {
            safeNavigate(ThreadListFragmentDirections.actionHomeFragmentToNewMessageActivity())
        }

        threadsList.setPagination(
            whenLoadMoreIsPossible = { if (!isDownloadingChanges) downloadThreads() },
            triggerOffset = OFFSET_TRIGGER,
        )

        threadsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0 || dy <= 0) {
                    newMessageFab.extend()
                } else {
                    newMessageFab.shrink()
                }

                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }

    private fun setupUserAvatar() {
        AccountUtils.currentUser?.let { binding.userAvatarImage.loadAvatar(it, requireContext().imageLoader) }
    }

    private fun listenToCurrentFolder() {
        viewModel.currentFolder.observeNotNull(this, ::displayFolderName)
        viewModel.listenToCurrentFolder()
    }

    private fun displayFolderName(folder: Folder) = with(binding) {
        val folderName = folder.getLocalizedName(context)
        Log.i("UI", "Received folder name (${folderName})")
        toolbar.title = folderName
    }

    private fun listenToThreads() {
        viewModel.threads.observeNotNull(this, ::displayThreads)
        viewModel.listenToThreads()
    }

    private fun displayThreads(threads: List<Thread>) = with(binding) {
        Log.i("UI", "Received threads (${threads.size})")
        isDownloadingChanges = false
        swipeRefreshLayout.isRefreshing = false
        if (threads.size < PER_PAGE) mainViewModel.canContinueToPaginate = false

        updateUnreadCount()

        if (threads.isEmpty()) displayNoEmailView() else displayThreadList()

        with(threadListAdapter) {
            notifyAdapter(formatList(threads, context))
        }
        startPeriodicRefreshJob()
    }

    private fun displayNoEmailView() = with(binding) {
        threadsList.isGone = true
        noMailLayoutGroup.isVisible = true
    }

    private fun displayThreadList() = with(binding) {
        threadsList.isVisible = true
        noMailLayoutGroup.isGone = true
    }

    private fun downloadThreads() {

        val folder = MainViewModel.currentFolderFlow.value ?: return
        val mailbox = MainViewModel.currentMailboxFlow.value ?: return

        if (mainViewModel.canContinueToPaginate) {
            isDownloadingChanges = true
            currentOffset += PER_PAGE
            showLoadingTimer.start()
            mainViewModel.loadMoreThreads(mailbox, folder, currentOffset)
        }
    }
}
