package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDatabase() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_getReminderById_verifyCorrectData() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO("Reminder title", "description", "location", 30.0, 25.0)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val loadedReminder = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(loadedReminder as ReminderDTO, notNullValue())
        assertThat(loadedReminder.id, `is`(reminder.id))
        assertThat(loadedReminder.title, `is`(reminder.title))
        assertThat(loadedReminder.description, `is`(reminder.description))
        assertThat(loadedReminder.location, `is`(reminder.location))
        assertThat(loadedReminder.latitude, `is`(reminder.latitude))
        assertThat(loadedReminder.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminders_verifyLoadingReminders() = runBlockingTest {
        // GIVEN - Insert 2 different reminders.
        val reminder1 = ReminderDTO("title 1", "description 1", "location 1", 10.55, 10.552)
        val reminder2 = ReminderDTO("title 2", "description 2", "location 2", 37.3333, 4.4)
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        // WHEN - Load all reminders from the database.
        val loadedReminders = database.reminderDao().getReminders()

        // THEN - size is correct, id is correct
        assertThat(loadedReminders.size, `is`(3))
        assertThat(loadedReminders[0].id, `is`(reminder1.id))
        assertThat(loadedReminders[1].id, `is`(reminder2.id))
    }

    @Test
    fun deleteAllReminders_verifyEmpty() = runBlockingTest {
        // GIVEN - Insert 2 different reminders.
        val reminder1 = ReminderDTO("title 1", "description 1", "location 1", 10.55, 10.552)
        val reminder2 = ReminderDTO("title 2", "description 2", "location 2", 37.3333, 4.4)
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        // WHEN - delete all reminders
        database.reminderDao().deleteAllReminders()

        // THEN - size is empty
        val loadedReminders = database.reminderDao().getReminders()
        assertThat(loadedReminders.size, `is`(0))
    }

}