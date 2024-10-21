# Weekly Scheduler For CEO

The CEO Weekly Scheduler is a Java-based application designed to help a company's secretary manage the weekly schedule of the CEO. The application allows the secretary to create, edit, and delete events, as well as save and load the schedule to a file.

## Features

- Displays a weekly calendar view with 7 days, including weekends
- Allows scheduling of events between 8 AM to 8 PM on weekdays and 8 AM to 3 PM on Saturdays
- Events can be created, edited, and deleted
- Event details include name, location, start time, end time, and color
- Event duration must be between 30 minutes and 3 hours
- Overlapping events are limited to 30 minutes
- Saves and loads the schedule to/from a file named "schedule.dat"
- Prompts the secretary to enter the current Monday's date if the schedule file is not found

## Getting Started

1. Clone the repository.
2. Open the project in your preferred Java IDE.
3. Compile and run the `WeeklyScheduler` class to launch the application.

## Usage

1. When the application starts, if the "schedule.dat" file is not found, the secretary will be prompted to enter the current Monday's date.
2. The weekly calendar view will be displayed, showing the events for the current week.
3. To create a new event, click on the "New Event" menu item.
4. Fill out the event details in the form, including the name, location, start time, end time, and color.
5. Click "OK" to save the event or "Cancel" to discard it.
6. To edit an existing event, double-click on the event in the calendar view or the daily view.
7. To delete an event, select the event and click the "Delete" button in the event details form.
8. To save the current schedule, click the "Save Schedule" menu item.

## Dependencies

The CEO Weekly Scheduler application uses the following libraries:

- Java Swing for the user interface
- Java Serialization for saving and loading the schedule

## Contributing

If you find any issues or have suggestions for improvements, feel free to create a new issue or submit a pull request.

## License

This project is licensed under the MIT License.
