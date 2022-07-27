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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.infomaniak.lib.core.utils.Utils
import com.infomaniak.lib.core.utils.loadAvatar
import com.infomaniak.lib.core.utils.safeNavigate
import com.infomaniak.lib.core.utils.setPagination
import com.infomaniak.mail.R
import com.infomaniak.mail.data.MailData
import com.infomaniak.mail.data.api.ApiRepository.OFFSET_FIRST_PAGE
import com.infomaniak.mail.data.api.ApiRepository.PER_PAGE
import com.infomaniak.mail.data.models.Folder
import com.infomaniak.mail.data.models.Mailbox
import com.infomaniak.mail.data.models.thread.Thread
import com.infomaniak.mail.data.models.thread.Thread.ThreadFilter
import com.infomaniak.mail.databinding.FragmentThreadListBinding
import com.infomaniak.mail.ui.main.MainActivity
import com.infomaniak.mail.ui.main.MainViewModel
import com.infomaniak.mail.ui.main.menu.MenuDrawerFragment
import com.infomaniak.mail.utils.AccountUtils
import com.infomaniak.mail.utils.context
import com.infomaniak.mail.utils.observeNotNull
import com.infomaniak.mail.utils.toDate
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
        Utils.createRefreshTimer { binding.swipeRefreshLayout.isRefreshing = true }
    }

    private var menuDrawerFragment: MenuDrawerFragment? = null
    private var menuDrawerNavigation: NavigationView? = null
    private var drawerLayout: DrawerLayout? = null
    private val drawerListener = object : DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            // No-op
        }

        override fun onDrawerOpened(drawerView: View) {
            // No-op
        }

        override fun onDrawerClosed(drawerView: View) {
            menuDrawerFragment?.closeDropdowns()
        }

        override fun onDrawerStateChanged(newState: Int) {
            // No-op
        }
    }

    private companion object {
        const val OFFSET_TRIGGER = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentThreadListBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        startPeriodicRefreshJob()

        setupOnRefresh()
        setupAdapter()
        setupMenuDrawer()
        setupListeners()
        setupUserAvatar()
        setupUnreadCountChip()

        listenToCurrentMailbox()
        listenToCurrentFolder()
        listenToThreads()
    }

    private fun setupUnreadCountChip() {
            binding.unreadCountChip.apply {
            setOnClickListener {
                isCloseIconVisible = isChecked
                viewModel.filter = if (isChecked) ThreadFilter.UNSEEN else null
                binding.swipeRefreshLayout.isRefreshing = true
                viewModel.refreshThreads()
                scrollToTop()
            }
        }
        }

    private fun setupOnRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this)
    }

    override fun onRefresh() {
        viewModel.refreshThreads()
        scrollToTop()
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
        val lastUpdatedAt = MailData.currentFolderFlow.value?.lastUpdatedAt?.toDate() ?: Date(0)
        val ago = when {
            Date(0).time == lastUpdatedAt.time -> ""
            Date().time - lastUpdatedAt.time < DateUtils.MINUTE_IN_MILLIS -> getString(R.string.threadListHeaderLastUpdateNow)
            else -> DateUtils.getRelativeTimeSpanString(lastUpdatedAt.time).toString().replaceFirstChar { it.lowercaseChar() }
        }

        updatedAt.text = if (ago.isEmpty()) {
            getString(R.string.noNetworkDescription)
        } else {
            getString(R.string.threadListHeaderLastUpdate, ago)
        }
    }

    private fun updateUnreadCount(unreadCount: Int) = with(binding) {
        if (unreadCount == 0 && viewModel.lastUnreadCount > 0 && viewModel.filter != null) {
            swipeRefreshLayout.isRefreshing = true
            clearFilter()
            onRefresh()
        }
        viewModel.lastUnreadCount = unreadCount
        unreadCountChip.text = resources.getQuantityString(R.plurals.threadListHeaderUnreadCount, unreadCount, unreadCount)
        unreadCountChip.isVisible = unreadCount > 0
    }

    private fun setupMenuDrawer() {
        (activity as? MainActivity)?.binding?.let { activityBinding ->

            drawerLayout = activityBinding.drawerLayout.also {
                it.addDrawerListener(drawerListener)
            }

            menuDrawerNavigation = activityBinding.menuDrawerNavigation
        }
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

        toolbar.setNavigationOnClickListener { drawerLayout?.open() }

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
            whenLoadMoreIsPossible = { if (!viewModel.isDownloadingChanges) downloadThreads() },
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
        AccountUtils.currentUser?.let(binding.userAvatarImage::loadAvatar)
    }

    override fun onDestroyView() {
        drawerLayout?.removeDrawerListener(drawerListener)

        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        setupMenuDrawerCallbacks()

        viewModel.currentOffset = OFFSET_FIRST_PAGE
        binding.unreadCountChip.apply { isCloseIconVisible = isChecked }
        viewModel.loadMailData()
    }

        with(viewModel) {
            currentFolder.value?.threads?.toList()?.let(::displayThreads)

            currentOffset = OFFSET_FIRST_PAGE
            binding.unreadCountChip.apply { isCloseIconVisible = isChecked }
            loadMailData()
        }
    }

private fun setupMenuDrawerCallbacks() {
        val fragmentContainer = (activity as? MainActivity)?.binding?.menuDrawerFragment ?: return
        (fragmentContainer.getFragment() as? MenuDrawerFragment)
            ?.apply {
                closeDrawer = { closeDrawer() }
                isDrawerOpen = { drawerLayout?.isOpen ?: false }
            }?.also {
                menuDrawerFragment = it
            }
    }

    private fun listenToCurrentMailbox() {
        viewModel.currentMailbox.observeNotNull(this, ::onMailboxChange)
        viewModel.listenToCurrentMailbox()
    }

    private fun listenToCurrentFolder() {
        viewModel.currentFolder.observeNotNull(this, ::updateFolderInfo)
        viewModel.listenToCurrentFolder()
    }

    private fun resetList() {
        clearFilter()
        scrollToTop()
    }

    private fun clearFilter() = with(binding.unreadCountChip) {
        viewModel.filter = null
        isChecked = false
        isCloseIconVisible = false
    }

    private fun onMailboxChange(mailbox: Mailbox) = with(viewModel) {
        if (mailbox.objectId != lastMailboxId) resetList()
        lastMailboxId = mailbox.objectId
    }

    private fun updateFolderInfo(folder: Folder) = with(viewModel) {
        if (lastFolderRole != folder.role) {
            lastUnreadCount = folder.unreadCount
            resetList()
        }
        lastFolderRole = folder.role

        val folderName = folder.getLocalizedName(binding.context)
        Log.i("UI", "Received folder name (${folderName})")
        binding.toolbar.title = folderName
        updateUpdatedAt()
        updateUnreadCount(folder.unreadCount)
    }

    private fun listenToThreads() {
        viewModel.threads.observeNotNull(this, ::displayThreads)
        viewModel.listenToThreads()
    }

    private fun displayThreads(threads: List<Thread>) = with(binding) {
        Log.i("UI", "Received threads (${threads.size})")
        viewModel.isDownloadingChanges = false
        swipeRefreshLayout.isRefreshing = false

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

    private fun closeDrawer() {
        drawerLayout?.let { drawer -> menuDrawerNavigation?.let(drawer::closeDrawer) }
    }


    private fun scrollToTop() {
        binding.threadsList.layoutManager?.scrollToPosition(0)
    }

    private fun downloadThreads() = with(viewModel) {

        val folder = MailData.currentFolderFlow.value ?: return
        val mailbox = MailData.currentMailboxFlow.value ?: return

        if (folder.totalCount > currentOffset + PER_PAGE) {
            isDownloadingChanges = true
            currentOffset += PER_PAGE
            showLoadingTimer.start()
            viewModel.loadThreads(folder, mailbox, currentOffset)
        }
    }
}
