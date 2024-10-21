import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.io.*;

public class WeeklyScheduler {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}

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

    // Getters and setters with validation
    public void setName(String name) {
        if (name.length() > 32) throw new IllegalArgumentException("Name too long");
        this.name = name;
    }

    public void setLocation(String location) {
        if (location.length() > 32) throw new IllegalArgumentException("Location too long");
        this.location = location;
    }

    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setColor(Color color) { this.color = color; }

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

        weekPanel = new WeekPanel(currentMonday, events, this);
        add(weekPanel);
    }

    void showNewEventDialog() {
        EventDialog dialog = new EventDialog(this, null);
        dialog.setVisible(true);
        Event newEvent = dialog.getEvent();
        if (newEvent != null && isValidEvent(newEvent)) {
            events.add(newEvent);
            weekPanel.repaint();
            saveSchedule();
        }
    }

    private boolean isValidEvent(Event newEvent) {
        // Duration check (30 minutes to 3 hours)
        long durationMinutes = Duration.between(
            newEvent.getStartTime(), newEvent.getEndTime()).toMinutes();
        if (durationMinutes < 30 || durationMinutes > 180) {
            JOptionPane.showMessageDialog(this, 
                "Event duration must be between 30 minutes and 3 hours");
            return false;
        }

        // Business hours check
        int dayOfWeek = newEvent.getStartTime().getDayOfWeek().getValue();
        if (dayOfWeek == 7) { // Sunday
            JOptionPane.showMessageDialog(this, "No events allowed on Sunday");
            return false;
        } else if (dayOfWeek == 6) { // Saturday
            if (newEvent.getStartTime().getHour() < 8 || 
                newEvent.getEndTime().getHour() > 15) {
                JOptionPane.showMessageDialog(this, 
                    "Saturday events must be between 8 AM and 3 PM");
                return false;
            }
        } else { // Weekdays
            if (newEvent.getStartTime().getHour() < 8 || 
                newEvent.getEndTime().getHour() > 20) {
                JOptionPane.showMessageDialog(this, 
                    "Weekday events must be between 8 AM and 8 PM");
                return false;
            }
        }

        // Overlap check (30 minutes maximum)
        for (Event existing : events) {
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

    private boolean saveSchedule() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("schedule.dat"))) {
            oos.writeObject(events);
            oos.writeObject(currentMonday);
            JOptionPane.showMessageDialog(this, "Schedule saved successfully");
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

    void deleteEvent(Event event) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this event?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            events.remove(event);
            weekPanel.repaint();
            saveSchedule();
        }
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
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }
}

class WeekPanel extends JPanel {
    private LocalDate monday;
    private ArrayList<Event> events;
    private MainFrame mainFrame;
    private static final int HOUR_HEIGHT = 60;
    private static final int DAY_WIDTH = 150;
    private static final int HEADER_HEIGHT = 30;
    
    public WeekPanel(LocalDate monday, ArrayList<Event> events, MainFrame mainFrame) {
        this.monday = monday;
        this.events = events;
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(DAY_WIDTH * 7, HOUR_HEIGHT * 12 + HEADER_HEIGHT));
        
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
        // Draw day headers and vertical lines
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            String header = date.format(DateTimeFormatter.ofPattern("MM/dd"));
            g.drawString(header, i * DAY_WIDTH + 5, 20);
            g.drawLine(i * DAY_WIDTH, 0, i * DAY_WIDTH, getHeight());
        }
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        // Draw hour lines and labels
        for (int i = 0; i <= 12; i++) {
            int y = i * HOUR_HEIGHT + HEADER_HEIGHT;
            g.drawLine(0, y, getWidth(), y);
            g.drawString((i + 8) + ":00", 5, y - 5);
        }
    }

    private void drawEvents(Graphics g) {
        for (Event event : events) {
            LocalDateTime startTime = event.getStartTime();
            LocalDateTime endTime = event.getEndTime();
            
            int day = startTime.getDayOfWeek().getValue() - 1;
            int startHour = startTime.getHour() - 8;
            int endHour = endTime.getHour() - 8;
            int startMinute = startTime.getMinute();
            int endMinute = endTime.getMinute();
            
            int x = day * DAY_WIDTH + 5;
            int y = startHour * HOUR_HEIGHT + (startMinute * HOUR_HEIGHT / 60) + HEADER_HEIGHT;
            int height = (endHour - startHour) * HOUR_HEIGHT + 
                        ((endMinute - startMinute) * HOUR_HEIGHT / 60);

            // Draw event rectangle
            g.setColor(event.getColor());
            g.fillRect(x, y, DAY_WIDTH - 10, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, DAY_WIDTH - 10, height);
            g.drawString(event.getName(), x + 5, y + 15);
        }
    }

    private void handleClick(int x, int y) {
        // Check if clicked on date header (for daily view)
        if (y < HEADER_HEIGHT) {
            int day = x / DAY_WIDTH;
            if (day >= 0 && day < 7) {
                showDailyView(monday.plusDays(day));
                return;
            }
        }

        // Check if clicked on an event
        Event clickedEvent = findEventAt(x, y);
        if (clickedEvent != null) {
            EventDialog dialog = new EventDialog(mainFrame, clickedEvent);
            dialog.setVisible(true);
            repaint();
        }
    }

    private Event findEventAt(int x, int y) {
        int day = x / DAY_WIDTH;
        int hour = (y - HEADER_HEIGHT) / HOUR_HEIGHT;
        int minute = ((y - HEADER_HEIGHT) % HOUR_HEIGHT) * 60 / HOUR_HEIGHT;
        
        LocalDateTime clickTime = monday.plusDays(day)
            .atTime(hour + 8, minute);

        for (Event event : events) {
            if (isTimeInEvent(clickTime, event)) {
                return event;
            }
        }
        return null;
    }

    private boolean isTimeInEvent(LocalDateTime time, Event event) {
        return !time.isBefore(event.getStartTime()) && 
               !time.isAfter(event.getEndTime());
    }

    private void showDailyView(LocalDate date) {
        JDialog dailyView = new JDialog(mainFrame, 
            "Daily Schedule - " + date.format(DateTimeFormatter.ISO_DATE), 
            true);
        dailyView.setLayout(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<>();
        for (Event event : events) {
            if (event.getStartTime().toLocalDate().equals(date)) {
                String timeStr = event.getStartTime().format(
                    DateTimeFormatter.ofPattern("HH:mm")) + 
                    " - " + event.getName();
                model.addElement(timeStr);
            }
        }

        JList<String> eventList = new JList<>(model);
        dailyView.add(new JScrollPane(eventList), BorderLayout.CENTER);
        
        dailyView.setSize(300, 400);
        dailyView.setLocationRelativeTo(mainFrame);
        dailyView.setVisible(true);
    }
}

class EventDialog extends JDialog {
    private Event result;
    private Event originalEvent;
    private JTextField nameField;
    private JTextField locationField;
    private JComboBox<String> colorBox;
    private JSpinner dateSpinner;
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    private final Color[] AVAILABLE_COLORS = {
        Color.RED, Color.GREEN, Color.YELLOW, 
        Color.BLUE, Color.ORANGE, Color.GRAY
    };

    public EventDialog(Frame owner, Event event) {
        super(owner, event == null ? "New Event" : "Edit Event", true);
        this.originalEvent = event;
        setupUI();
        if (event != null) {
            populateFields(event);
        }
    }

    private void setupUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create components
        nameField = new JTextField(32);
        locationField = new JTextField(32);
        
        String[] colorNames = {"Red", "Green", "Yellow", "Blue", "Orange", "Gray"};
        colorBox = new JComboBox<>(colorNames);

        // Setup date spinner
        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));

        // Setup time spinners
        startTimeSpinner = new JSpinner(new SpinnerDateModel());
        startTimeSpinner.setEditor(new JSpinner.DateEditor(startTimeSpinner, "HH:mm"));

        endTimeSpinner = new JSpinner(new SpinnerDateModel());
        endTimeSpinner.setEditor(new JSpinner.DateEditor(endTimeSpinner, "HH:mm"));

        // Add components to dialog
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Event Name:"), gbc);
        
        gbc.gridx = 1;
        add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Location:"), gbc);
        
        gbc.gridx = 1;
        add(locationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Date:"), gbc);
        
        gbc.gridx = 1;
        add(dateSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Start Time:"), gbc);
        
        gbc.gridx = 1;
        add(startTimeSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("End Time:"), gbc);
        
        gbc.gridx = 1;
        add(endTimeSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        add(new JLabel("Color:"), gbc);
        
        gbc.gridx = 1;
        add(colorBox, gbc);

        // Add buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            if (createEvent()) {
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void populateFields(Event event) {
        nameField.setText(event.getName());
        locationField.setText(event.getLocation());
        
        // Set date
        dateSpinner.setValue(java.sql.Timestamp.valueOf(
            event.getStartTime().toLocalDate().atStartOfDay()));
        
        // Set times
        startTimeSpinner.setValue(java.sql.Timestamp.valueOf(event.getStartTime()));
        endTimeSpinner.setValue(java.sql.Timestamp.valueOf(event.getEndTime()));
        
        // Set color
        for (int i = 0; i < AVAILABLE_COLORS.length; i++) {
            if (AVAILABLE_COLORS[i].equals(event.getColor())) {
                colorBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private boolean createEvent() {
        try {
            String name = nameField.getText().trim();
            String location = locationField.getText().trim();

            if (name.isEmpty() || location.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Name and location are required");
                return false;
            }

            if (name.length() > 32 || location.length() > 32) {
                JOptionPane.showMessageDialog(this, 
                    "Name and location must be 32 characters or less");
                return false;
            }

            // Get date from date spinner
            Date dateValue = (Date) dateSpinner.getValue();
            LocalDate date = dateValue.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            // Get times from time spinners
            Date startValue = (Date) startTimeSpinner.getValue();
            Date endValue = (Date) endTimeSpinner.getValue();
            
            LocalTime startTime = startValue.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
            LocalTime endTime = endValue.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime();

            // Create LocalDateTime objects
            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

            // Get selected color
            Color color = AVAILABLE_COLORS[colorBox.getSelectedIndex()];

            // Create new event
            result = new Event(name, location, startDateTime, endDateTime, color);

            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid input: " + e.getMessage());
            return false;
        }
    }

    public Event getEvent() {
        return result;
    }
}