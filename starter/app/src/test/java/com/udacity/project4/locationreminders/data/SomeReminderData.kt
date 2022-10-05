package com.udacity.project4.locationreminders

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

object SomeReminderData {
    val items = arrayListOf(
        ReminderDTO(
            "Title 1",
            "Description 1",
            "Location 1",
            37.1,
            -122.2
        ),
        ReminderDTO(
            "Title 2",
            "Description 2",
            "Location 2",
            38.5,
            -110.3
        ),
        ReminderDTO(
            "title 3",
            "description 3",
            "location 3",
            37.5,
            -120.1
        )
    )

    val reminderDataItem = ReminderDataItem(
        "Title 100",
        "Description 100",
        "Location 100",
        37.8,
        -122.2
    )
}