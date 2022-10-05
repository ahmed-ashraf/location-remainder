package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.SomeReminderData
import com.udacity.project4.locationreminders.CoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = CoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource

    private lateinit var viewModel: SaveReminderViewModel

    private lateinit var app: Application

    @Before
    fun setUp() {
        stopKoin()
        fakeDataSource = FakeDataSource()

        app = ApplicationProvider.getApplicationContext()

        viewModel = SaveReminderViewModel(app, fakeDataSource)
    }

    @Test
    fun saveReminder_ShowLoading() = runBlockingTest {
        mainCoroutineRule.pauseDispatcher()


        viewModel.saveReminder(SomeReminderData.reminderDataItem)


        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))


        mainCoroutineRule.resumeDispatcher()


        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun saveReminder_Success() {
        viewModel.saveReminder(SomeReminderData.reminderDataItem)

        MatcherAssert.assertThat(
            viewModel.showToast.getOrAwaitValue(),
            CoreMatchers.`is`(app.getString(R.string.reminder_saved))
        )
        Assert.assertEquals(viewModel.navigationCommand.getOrAwaitValue(), NavigationCommand.Back)
    }

    @Test
    fun validateEnteredData_TitleEmpty_ReturnFalse(){
        val reminderData = SomeReminderData.reminderDataItem.copy()
        reminderData.title = ""

        val res = viewModel.validateEnteredData(reminderData)

        MatcherAssert.assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )
        MatcherAssert.assertThat(res, CoreMatchers.`is`(false))
    }

    @Test
    fun validateEnteredData_TitleNull_ReturnFalse(){
        val reminderData = SomeReminderData.reminderDataItem.copy()
        reminderData.title = null

        val res = viewModel.validateEnteredData(reminderData)

        MatcherAssert.assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )
        MatcherAssert.assertThat(res, CoreMatchers.`is`(false))
    }

    @Test
    fun validateEnteredData_LocationNull_ReturnFalse(){
        val reminderData = SomeReminderData.reminderDataItem.copy()
        reminderData.location = null

        val res = viewModel.validateEnteredData(reminderData)

        MatcherAssert.assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            Matchers.`is`(R.string.err_select_location)
        )
        MatcherAssert.assertThat(res, CoreMatchers.`is`(false))
    }

    @Test
    fun validateEnteredData_LocationEmpty_ReturnFalse(){
        val reminderData = SomeReminderData.reminderDataItem.copy()
        reminderData.location = ""

        val res = viewModel.validateEnteredData(reminderData)

        MatcherAssert.assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            Matchers.`is`(R.string.err_select_location)
        )
        MatcherAssert.assertThat(res, CoreMatchers.`is`(false))
    }

    @Test
    fun validateEnteredData_ReturnTrue() {
        val res = viewModel.validateEnteredData(SomeReminderData.reminderDataItem)

        MatcherAssert.assertThat(res, CoreMatchers.`is`(true))
    }
}