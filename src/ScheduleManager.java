import java.io.*;
import java.time.*;
import java.util.*;
import javax.swing.*;

class ScheduleManager extends JFrame {
    private WeeklyCalendarView weekPanel;
    private ArrayList<ScheduleEvent> events;
    private LocalDate currentMonday;
    private JMenuBar menuBar;

    public ScheduleManager() {
        events = new ArrayList<>();
        
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

        setupUI();
    }

    private void setupUI() {
        setTitle("CEO Weekly Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newEventItem = new JMenuItem("New Event");
        JMenuItem saveItem = new JMenuItem("Save Schedule");

        newEventItem.addActionListener(e -> showNewEventDialog());
        saveItem.addActionListener(e -> saveSchedule());

        fileMenu.add(newEventItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        weekPanel = new WeeklyCalendarView(currentMonday, events, this);
        add(weekPanel);
    }

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

    private boolean isValidEvent(ScheduleEvent newEvent) {
        // Duration check (30 minutes to 3 hours)
        long durationMinutes = Duration.between(
            newEvent.getStartTime(), newEvent.getEndTime()).toMinutes();
        if (durationMinutes < 30 || durationMinutes > 180) {
            JOptionPane.showMessageDialog(this, 
                "Event duration must be between 30 minutes minimum and 3 hours maximum.");
            return false;
        }

        // Business hours check
        int dayOfWeek = newEvent.getStartTime().getDayOfWeek().getValue();
        switch (dayOfWeek) {
            case 7 -> {
                // Sunday
                JOptionPane.showMessageDialog(this, "No events allowed on Sunday");
                return false;
            }
            case 6 -> {
                // Saturday
                if (newEvent.getStartTime().getHour() < 8 ||
                        newEvent.getEndTime().getHour() > 15) {
                    JOptionPane.showMessageDialog(this,
                            "Saturday events must be between 8 AM and 3 PM");
                    return false;
                }
            }
            default -> {
                // Weekdays
                if (newEvent.getStartTime().getHour() < 8 ||
                        newEvent.getEndTime().getHour() > 20) {
                    JOptionPane.showMessageDialog(this,
                            "Weekday events must be between 8 AM and 8 PM");
                    return false;
                }
            }
        }

        // Overlap check (30 minutes maximum)
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
    
    public void addNewEvent(ScheduleEvent newEvent) {
        if (isValidEvent(newEvent)) {
            events.add(newEvent);
            weekPanel.repaint();
            saveSchedule();
        }
    }

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

    private boolean eventsOverlap(ScheduleEvent e1, ScheduleEvent e2) {
        return !e1.getEndTime().isBefore(e2.getStartTime()) && 
               !e2.getEndTime().isBefore(e1.getStartTime());
    }

    private long calculateOverlap(ScheduleEvent e1, ScheduleEvent e2) {
        LocalDateTime overlapStart = e1.getStartTime().isBefore(e2.getStartTime()) ? 
            e2.getStartTime() : e1.getStartTime();
        LocalDateTime overlapEnd = e1.getEndTime().isBefore(e2.getEndTime()) ? 
            e1.getEndTime() : e2.getEndTime();
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }
}