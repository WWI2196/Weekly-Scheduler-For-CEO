import java.io.*;
import java.time.*;
import java.util.*;
import javax.swing.*;

//  Class to managing the scheduling tasks
class ScheduleManager extends JFrame {
    private WeeklyCalendarView weekPanel;
    private ArrayList<ScheduleEvent> events;
    private LocalDate currentMonday;
    private JMenuBar menuBar;

    public ScheduleManager() {
        events = new ArrayList<>();
        
        // Load existing schedule or ask for the initial date
        if (!loadSchedule()) {
            String input = JOptionPane.showInputDialog(this, 
                "Enter Monday's date (YYYY-MM-DD):", 
                "Initialize Schedule", 
                JOptionPane.QUESTION_MESSAGE);
            if (input == null || input.trim().isEmpty()) {
                System.exit(0);
            }
            try {
                currentMonday = LocalDate.parse(input);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid date format", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        setUserInterface();
    }

     // Set the main user interface
    private void setUserInterface() {
        setTitle("CEO Weekly Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

         // Create menu bar 
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Menu");
        JMenuItem newEventItem = new JMenuItem("Add New Event");
        JMenuItem saveItem = new JMenuItem("Save Schedule");

        newEventItem.addActionListener(e -> showNewEventDialog());
        saveItem.addActionListener(e -> saveSchedule());

        fileMenu.add(newEventItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

         // Create and add the calendar view
        weekPanel = new WeeklyCalendarView(currentMonday, events, this);
        add(weekPanel);
    }

    // Display form to create a new event
    void showNewEventDialog() {
        EventDetailsForm dialog = new EventDetailsForm(this, null);
        dialog.setVisible(true);
        ScheduleEvent newEvent = dialog.getEvent();
        if (newEvent != null && isValidEvent(newEvent)) {
            events.add(newEvent);
            weekPanel.repaint();
            saveSchedule();
        }
    }

    // Validate the event
    private boolean isValidEvent(ScheduleEvent newEvent) {
        
        // Duration check 
        long durationMinutes = Duration.between(
            newEvent.getStartTime(), newEvent.getEndTime()).toMinutes();
        if (durationMinutes < 30 || durationMinutes > 180) {
            JOptionPane.showMessageDialog(this, 
                "Event duration must be between 30 minutes minimum and 3 hours maximum.");
            return false;
        }

        // Check if event is within allowed times
        int dayOfWeek = newEvent.getStartTime().getDayOfWeek().getValue();
        switch (dayOfWeek) {
            case 7 -> {
                JOptionPane.showMessageDialog(this, "No events allowed on Sunday");
                return false;
            }
            case 6 -> {
                if (newEvent.getStartTime().getHour() < 8 ||
                        newEvent.getEndTime().getHour() > 15) {
                    JOptionPane.showMessageDialog(this,
                            "Saturday events must be between 8 AM and 3 PM");
                    return false;
                }
            }
            default -> {
                if (newEvent.getStartTime().getHour() < 8 ||
                        newEvent.getEndTime().getHour() > 20) {
                    JOptionPane.showMessageDialog(this,
                            "Weekday events must be between 8 AM and 8 PM");
                    return false;
                }
            }
        }

        // Event Overlaping check 
        for (ScheduleEvent existing : events) {
            if (existing != newEvent && eventsOverlap(existing, newEvent)) {
                long overlapMinutes = calculateOverlap(existing, newEvent);
                if (overlapMinutes > 30) {
                    JOptionPane.showMessageDialog(this, 
                        "Events cannot overlap by more than 30 minutes");
                    return false;
                }
            }
        }

        return true;
    }

     // Save the current schedule to a file
    public boolean saveSchedule() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("src/scheduleData/schedule.dat"))) {
            oos.writeObject(events);
            oos.writeObject(currentMonday);
            JOptionPane.showMessageDialog(this, "Schedule saved successfully.");
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving schedule: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

     // Load the current schedule from the file
    private boolean loadSchedule() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("src/scheduleData/schedule.dat"))) {
            events = (ArrayList<ScheduleEvent>) ois.readObject();
            currentMonday = (LocalDate) ois.readObject();
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Update an exsisting event
    public void updateEvent(ScheduleEvent updatedEvent) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).equals(updatedEvent)) {
                events.set(i, updatedEvent);
                break;
            }
        }
        weekPanel.repaint();
        saveSchedule();
    }
    
    // Add a new event
    public void addNewEvent(ScheduleEvent newEvent) {
        if (isValidEvent(newEvent)) {
            events.add(newEvent);
            weekPanel.repaint();
            saveSchedule();
        }
    }

    // Delete an exsisting event
    void deleteEvent(ScheduleEvent event) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Do you want to delete this event?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            events.remove(event);
            weekPanel.repaint();
            saveSchedule();
        }
    }

    // To check if events overlap
    private boolean eventsOverlap(ScheduleEvent e1, ScheduleEvent e2) {
        return !e1.getEndTime().isBefore(e2.getStartTime()) && 
               !e2.getEndTime().isBefore(e1.getStartTime());
    }

    // To calculate the overlaping time it events overlaps
    private long calculateOverlap(ScheduleEvent e1, ScheduleEvent e2) {
        LocalDateTime overlapStart = e1.getStartTime().isBefore(e2.getStartTime()) ? 
            e2.getStartTime() : e1.getStartTime();
        LocalDateTime overlapEnd = e1.getEndTime().isBefore(e2.getEndTime()) ? 
            e1.getEndTime() : e2.getEndTime();
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }
}