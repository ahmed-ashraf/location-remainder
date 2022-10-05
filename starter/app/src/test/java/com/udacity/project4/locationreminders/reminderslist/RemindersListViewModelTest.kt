package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.SomeReminderData
import com.udacity.project4.locationreminders.CoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = CoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource

    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun setUp() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }



    @Test
    fun loadReminders_Success() = runBlockingTest {
        SomeReminderData.items.forEach { reminderDTO ->
            fakeDataSource.saveReminder(reminderDTO)
        }

        viewModel.loadReminders()

        val loadedItems = viewModel.remindersList.getOrAwaitValue()
        MatcherAssert.assertThat(loadedItems.size, `is`(SomeReminderData.items.size))
        for (i in loadedItems.indices) {
            MatcherAssert.assertThat(loadedItems[i].title, `is`(SomeReminderData.items[i].title))
        }
        MatcherAssert.assertThat(viewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun loadReminders_ShowLoading() = runBlockingTest {
        mainCoroutineRule.pauseDispatcher()

        viewModel.loadReminders()

        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))

        mainCoroutineRule.resumeDispatcher()

        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun loadReminders_resultSuccess_noReminders() = runBlockingTest {
        fakeDataSource.deleteAllReminders()

        viewModel.loadReminders()


        val loadedItems = viewModel.remindersList.getOrAwaitValue()
        MatcherAssert.assertThat(loadedItems.size, CoreMatchers.`is`(0))

        MatcherAssert.assertThat(viewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(true))
    }
}