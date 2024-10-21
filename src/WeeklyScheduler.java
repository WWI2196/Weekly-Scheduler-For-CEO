import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;

public class WeeklyScheduler {
    // Main method to launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}

// All other classes are now inner classes, marked as static to avoid requiring an instance of CEOScheduler
class Event implements Serializable {
    private String name;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Color color;

    public Event(String name, String location, LocalDateTime startTime, 
                 LocalDateTime endTime, Color color) {
        if (name.length() > 32 || location.length() > 32) {
            throw new IllegalArgumentException("Name and location must be 32 characters or less");
        }
        this.name = name;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getLocation() { return location; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Color getColor() { return color; }
}

class MainFrame extends JFrame {
    private WeekPanel weekPanel;
    private ArrayList<Event> events;
    private LocalDate currentMonday;
    private JMenuBar menuBar;

    public MainFrame() {
        events = new ArrayList<>();
        
        // Try to load existing schedule
        if (!loadSchedule()) {
            // If no schedule exists, prompt for Monday's date
            String input = JOptionPane.showInputDialog("Enter Monday's date (YYYY-MM-DD):");
            if (input == null || input.trim().isEmpty()) {
                System.exit(0);
            }
            try {
                currentMonday = LocalDate.parse(input);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid date format");
                System.exit(0);
            }
        }

        setupUI();
    }

    private void setupUI() {
        setTitle("CEO Weekly Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        // Setup menu
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

        // Create week panel
        weekPanel = new WeekPanel(currentMonday, events);
        add(weekPanel);
    }

    private void showNewEventDialog() {
        NewEventDialog dialog = new NewEventDialog(this);
        dialog.setVisible(true);
        if (dialog.getEvent() != null) {
            if (isValidEvent(dialog.getEvent())) {
                events.add(dialog.getEvent());
                weekPanel.repaint();
            }
        }
    }

    private boolean isValidEvent(Event newEvent) {
        // Check duration (30 min to 3 hours)
        long durationMinutes = java.time.Duration.between(
            newEvent.getStartTime(), newEvent.getEndTime()).toMinutes();
        if (durationMinutes < 30 || durationMinutes > 180) {
            JOptionPane.showMessageDialog(this, 
                "Event duration must be between 30 minutes and 3 hours");
            return false;
        }

        // Check business hours and weekend restrictions
        if (newEvent.getStartTime().getDayOfWeek().getValue() == 6) { // Saturday
            if (newEvent.getStartTime().getHour() < 8 || 
                newEvent.getEndTime().getHour() > 15) {
                JOptionPane.showMessageDialog(this, 
                    "Saturday events must be between 8 AM and 3 PM");
                return false;
            }
        } else if (newEvent.getStartTime().getDayOfWeek().getValue() == 7) { // Sunday
            JOptionPane.showMessageDialog(this, "No events allowed on Sunday");
            return false;
        } else { // Weekdays
            if (newEvent.getStartTime().getHour() < 8 || 
                newEvent.getEndTime().getHour() > 20) {
                JOptionPane.showMessageDialog(this, 
                    "Weekday events must be between 8 AM and 8 PM");
                return false;
            }
        }

        // Check overlap limit (30 minutes)
        for (Event existing : events) {
            if (eventsOverlap(existing, newEvent)) {
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

    private boolean eventsOverlap(Event e1, Event e2) {
        return !e1.getEndTime().isBefore(e2.getStartTime()) && 
               !e2.getEndTime().isBefore(e1.getStartTime());
    }

    private long calculateOverlap(Event e1, Event e2) {
        LocalDateTime overlapStart = e1.getStartTime().isBefore(e2.getStartTime()) ? 
            e2.getStartTime() : e1.getStartTime();
        LocalDateTime overlapEnd = e1.getEndTime().isBefore(e2.getEndTime()) ? 
            e1.getEndTime() : e2.getEndTime();
        return java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    private boolean saveSchedule() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("schedule.dat"))) {
            oos.writeObject(events);
            oos.writeObject(currentMonday);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving schedule");
            return false;
        }
    }

    private boolean loadSchedule() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("schedule.dat"))) {
            events = (ArrayList<Event>) ois.readObject();
            currentMonday = (LocalDate) ois.readObject();
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }
}

class WeekPanel extends JPanel {
    private LocalDate monday;
    private ArrayList<Event> events;
    private static final int HOUR_HEIGHT = 60;
    private static final int DAY_WIDTH = 150;
    
    public WeekPanel(LocalDate monday, ArrayList<Event> events) {
        this.monday = monday;
        this.events = events;
        setPreferredSize(new Dimension(DAY_WIDTH * 7, HOUR_HEIGHT * 12));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGrid(g);
        drawEvents(g);
    }

    private void drawGrid(Graphics g) {
        // Draw vertical lines for days
        for (int i = 0; i <= 7; i++) {
            g.drawLine(i * DAY_WIDTH, 0, i * DAY_WIDTH, getHeight());
            if (i < 7) {
                // Draw day headers
                LocalDate date = monday.plusDays(i);
                g.drawString(date.toString(), i * DAY_WIDTH + 5, 20);
            }
        }

        // Draw horizontal lines for hours
        for (int i = 0; i <= 12; i++) {
            g.drawLine(0, i * HOUR_HEIGHT, getWidth(), i * HOUR_HEIGHT);
            g.drawString((i + 8) + ":00", 5, i * HOUR_HEIGHT + 15);
        }
    }

    private void drawEvents(Graphics g) {
        for (Event event : events) {
            // Calculate position and size
            int day = event.getStartTime().getDayOfWeek().getValue() - 1;
            int startHour = event.getStartTime().getHour() - 8;
            int endHour = event.getEndTime().getHour() - 8;
            
            int x = day * DAY_WIDTH;
            int y = startHour * HOUR_HEIGHT;
            int height = (endHour - startHour) * HOUR_HEIGHT;

            // Draw event rectangle
            g.setColor(event.getColor());
            g.fillRect(x + 5, y + 5, DAY_WIDTH - 10, height - 10);
            g.setColor(Color.BLACK);
            g.drawString(event.getName(), x + 10, y + 20);
        }
    }

    private void handleClick(int x, int y) {
        // Find clicked event and show edit dialog
        for (Event event : events) {
            // Calculate event bounds and check if clicked
            // Show edit dialog if clicked
        }
    }
}

class NewEventDialog extends JDialog {
    private Event result;
    private JTextField nameField;
    private JTextField locationField;
    private JComboBox<String> colorBox;
    // ... other components

    public NewEventDialog(Frame owner) {
        super(owner, "New Event", true);
        setupUI();
    }

    private void setupUI() {
        setLayout(new GridLayout(0, 2, 5, 5));
        
        nameField = new JTextField(32);
        locationField = new JTextField(32);
        
        String[] colors = {"Red", "Green", "Yellow", "Blue", "Orange", "Gray"};
        colorBox = new JComboBox<>(colors);

        // Add components
        add(new JLabel("Name:"));
        add(nameField);
        add(new JLabel("Location:"));
        add(locationField);
        add(new JLabel("Color:"));
        add(colorBox);

        // Add date and time pickers
        // Add OK and Cancel buttons
        
        pack();
        setLocationRelativeTo(getOwner());
    }

    public Event getEvent() {
        return result;
    }
}