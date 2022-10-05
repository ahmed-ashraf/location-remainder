package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.util.LinkedHashMap

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    private val fakeData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }

        return Result.Success(fakeData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        fakeData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("An unknown error occurred!")
        }

        val reminder = fakeData[id]
        if (reminder != null) {
            return Result.Success(reminder)
        }
        return Result.Error("Reminder not found!")
    }

    override suspend fun deleteAllReminders() {
        fakeData.clear()
    }


}