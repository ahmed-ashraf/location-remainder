package com.udacity.project4

import android.app.Application
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)
    private lateinit var decorView: View

    @get:Rule
    var locationPermission = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @get:Rule
    var backgroundLocationPermission = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        activityScenarioRule.scenario.onActivity { activity ->
            decorView = activity.window.decorView
        }
        viewModel = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    @Test
    fun addReminder_verifyAddedItemInTheList() {
        val typingTitle = "Title"
        val typingDescription = "Description"
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Type data
        onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText(typingTitle))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText(typingDescription))
        closeSoftKeyboard()

        // Select location
        onView(withId(R.id.selectLocation)).perform(ViewActions.click())

        // Click any position in the map
        onView(withId(R.id.map_fragment)).perform(ViewActions.click())
        runBlocking {
            delay(1000)
        }
        // Save location
        onView(withId(R.id.btnSave)).perform(ViewActions.click())

        // Get selected location
        val selectedLocation = viewModel.selectedPOI.value?.name

        // Save
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // Verify: One item is created
        onView(withText(typingTitle)).check(matches(isDisplayed()))
        onView(withText(typingDescription)).check(matches(isDisplayed()))
        onView(withText(selectedLocation)).check(matches(isDisplayed()))

        // Click on that item
        onView(withText(typingTitle)).perform(ViewActions.click())

        // Verify detail screen is correct!
        onView(withText(typingTitle)).check(matches(isDisplayed()))
        onView(withText(typingDescription)).check(matches(isDisplayed()))
        onView(withText(selectedLocation)).check(matches(isDisplayed()))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }


}
