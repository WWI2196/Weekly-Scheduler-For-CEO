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
                "Event duration must be between 30 minutes minimum and 3 hours maximum.");
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
    private static final int HEADER_HEIGHT = 50;  // Increased header height
    private static final int TIME_COLUMN_WIDTH = 50;  // Width for time indicators
    
    public WeekPanel(LocalDate monday, ArrayList<Event> events, MainFrame mainFrame) {
        this.monday = monday;
        this.events = events;
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(TIME_COLUMN_WIDTH + DAY_WIDTH * 7, 
            HOUR_HEIGHT * 12 + HEADER_HEIGHT));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });

        // Add tooltip
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Event event = findEventAt(e.getX(), e.getY());
        if (event != null) {
            return String.format("<html>%s<br>Location: %s<br>Time: %s - %s</html>",
                event.getName(),
                event.getLocation(),
                event.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                event.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawGrid(g2d);
        drawTimeIndicators(g2d);
        drawEvents(g2d);
        drawCurrentTimeLine(g2d);
    }

    private void drawGrid(Graphics2D g) {
        // Draw header background
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, getWidth(), HEADER_HEIGHT);
        g.setColor(Color.BLACK);

        // Draw day headers
        Font headerFont = new Font("Arial", Font.BOLD, 12);
        g.setFont(headerFont);
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            int x = TIME_COLUMN_WIDTH + (i * DAY_WIDTH);
            
            // Draw day name
            String dayName = date.getDayOfWeek().getDisplayName(
                TextStyle.SHORT, Locale.getDefault());
            g.drawString(dayName, x + 5, 20);
            
            // Draw date
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM/dd"));
            g.drawString(dateStr, x + 5, 40);

            // Draw vertical lines
            g.drawLine(x, 0, x, getHeight());

            // Highlight weekend
            if (i >= 5) {  // Saturday and Sunday
                g.setColor(new Color(255, 240, 240, 100));
                g.fillRect(x, HEADER_HEIGHT, DAY_WIDTH, getHeight() - HEADER_HEIGHT);
                g.setColor(Color.BLACK);
            }
        }

        // Draw right border
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        // Draw horizontal hour lines
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i <= 12; i++) {
            int y = i * HOUR_HEIGHT + HEADER_HEIGHT;
            g.drawLine(TIME_COLUMN_WIDTH, y, getWidth(), y);
        }
    }

    private void drawTimeIndicators(Graphics2D g) {
        g.setColor(Color.BLACK);
        Font timeFont = new Font("Arial", Font.PLAIN, 10);
        g.setFont(timeFont);
        
        for (int i = 0; i <= 12; i++) {
            int hour = i + 8;  // Starting from 8 AM
            int y = i * HOUR_HEIGHT + HEADER_HEIGHT;
            
            // Draw hour
            g.drawString(String.format("%02d:00", hour), 5, y - 5);
            
            // Draw half-hour marker
            g.setColor(new Color(200, 200, 200));
            int halfHourY = y + (HOUR_HEIGHT / 2);
            g.drawLine(TIME_COLUMN_WIDTH, halfHourY, getWidth(), halfHourY);
            g.setColor(Color.BLACK);
        }
    }

    private void drawEvents(Graphics2D g) {
        for (Event event : events) {
            LocalDateTime startTime = event.getStartTime();
            LocalDateTime endTime = event.getEndTime();
            
            // Skip if event is not in current week
            if (!isEventInWeek(event)) continue;
            
            int day = startTime.getDayOfWeek().getValue() - 1;
            int startHour = startTime.getHour() - 8;
            int endHour = endTime.getHour() - 8;
            int startMinute = startTime.getMinute();
            int endMinute = endTime.getMinute();
            
            int x = TIME_COLUMN_WIDTH + (day * DAY_WIDTH) + 5;
            int y = startHour * HOUR_HEIGHT + 
                    (startMinute * HOUR_HEIGHT / 60) + 
                    HEADER_HEIGHT;
            int height = (endHour - startHour) * HOUR_HEIGHT + 
                        ((endMinute - startMinute) * HOUR_HEIGHT / 60);

            // Draw event background
            g.setColor(event.getColor());
            g.fillRoundRect(x, y, DAY_WIDTH - 10, height, 10, 10);
            
            // Draw event border
            g.setColor(event.getColor().darker());
            g.drawRoundRect(x, y, DAY_WIDTH - 10, height, 10, 10);

            // Draw event details
            g.setColor(Color.BLACK);
            Font eventFont = new Font("Arial", Font.BOLD, 11);
            g.setFont(eventFont);
            
            // Draw event time
            String timeStr = String.format("%02d:%02d-%02d:%02d", 
                startTime.getHour(), startTime.getMinute(),
                endTime.getHour(), endTime.getMinute());
            g.drawString(timeStr, x + 5, y + 15);
            
            // Draw event name
            g.drawString(event.getName(), x + 5, y + 30);
        }
    }

    private void drawCurrentTimeLine(Graphics2D g) {
        LocalDateTime now = LocalDateTime.now();
        if (isTimeInWorkHours(now)) {
            int day = now.getDayOfWeek().getValue() - 1;
            int hour = now.getHour() - 8;
            int minute = now.getMinute();
            
            int y = hour * HOUR_HEIGHT + 
                    (minute * HOUR_HEIGHT / 60) + 
                    HEADER_HEIGHT;

            // Draw current time line
            g.setColor(Color.RED);
            Stroke oldStroke = g.getStroke();
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g.drawLine(TIME_COLUMN_WIDTH, y, getWidth(), y);
            g.setStroke(oldStroke);
        }
    }

    private boolean isTimeInWorkHours(LocalDateTime time) {
        int dayOfWeek = time.getDayOfWeek().getValue();
        int hour = time.getHour();
        
        if (dayOfWeek <= 5) {  // Weekdays
            return hour >= 8 && hour < 20;
        } else if (dayOfWeek == 6) {  // Saturday
            return hour >= 8 && hour < 15;
        }
        return false;  // Sunday
    }

    private boolean isEventInWeek(Event event) {
        LocalDate eventDate = event.getStartTime().toLocalDate();
        LocalDate weekEnd = monday.plusDays(6);
        return !eventDate.isBefore(monday) && !eventDate.isAfter(weekEnd);
    }

    private void handleClick(int x, int y) {
        // Check if clicked in header area (for daily view)
        if (y < HEADER_HEIGHT && x > TIME_COLUMN_WIDTH) {
            int day = (x - TIME_COLUMN_WIDTH) / DAY_WIDTH;
            if (day >= 0 && day < 7) {
                showDailyView(monday.plusDays(day));
                return;
            }
        }

        // Check if clicked on an event
        Event clickedEvent = findEventAt(x, y);
        if (clickedEvent != null) {
            showEventDialog(clickedEvent);
        }
    }

    private void showEventDialog(Event event) {
        EventDialog dialog = new EventDialog(mainFrame, event);
        dialog.setVisible(true);
        if (dialog.getEvent() != null) {
            // If event was modified
            events.remove(event);  // Remove old event
            events.add(dialog.getEvent());  // Add modified event
            repaint();
        }
    }

    private Event findEventAt(int x, int y) {
        if (x < TIME_COLUMN_WIDTH || y < HEADER_HEIGHT) return null;
        
        int day = (x - TIME_COLUMN_WIDTH) / DAY_WIDTH;
        double hour = (double)(y - HEADER_HEIGHT) / HOUR_HEIGHT + 8;
        
        LocalDateTime clickTime = monday.plusDays(day)
            .atTime((int)hour, (int)((hour % 1) * 60));

        for (Event event : events) {
            if (isTimeInEvent(clickTime, event)) {
                return event;
            }
        }
        return null;
    }

    private void showDailyView(LocalDate date) {
        JDialog dailyView = new JDialog(mainFrame, 
            "Daily Schedule - " + date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), 
            true);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create event list with custom renderer
        DefaultListModel<Event> model = new DefaultListModel<>();
        for (Event event : events) {
            if (event.getStartTime().toLocalDate().equals(date)) {
                model.addElement(event);
            }
        }

        JList<Event> eventList = new JList<>(model);
        eventList.setCellRenderer(new EventListRenderer());
        eventList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Event selected = eventList.getSelectedValue();
                    if (selected != null) {
                        dailyView.dispose();
                        showEventDialog(selected);
                    }
                }
            }
        });

        contentPanel.add(new JScrollPane(eventList), BorderLayout.CENTER);

        // Add a label showing work hours
        String workHours = date.getDayOfWeek() == DayOfWeek.SATURDAY ? 
            "Work Hours: 8:00 AM - 3:00 PM" :
            date.getDayOfWeek() == DayOfWeek.SUNDAY ?
            "No Events Allowed" : "Work Hours: 8:00 AM - 8:00 PM";
        
        JLabel hoursLabel = new JLabel(workHours);
        hoursLabel.setHorizontalAlignment(JLabel.CENTER);
        contentPanel.add(hoursLabel, BorderLayout.NORTH);

        dailyView.add(contentPanel);
        dailyView.setSize(400, 500);
        dailyView.setLocationRelativeTo(mainFrame);
        dailyView.setVisible(true);
    }

    private boolean isTimeInEvent(LocalDateTime time, Event event) {
        return !time.isBefore(event.getStartTime()) && 
               !time.isAfter(event.getEndTime());
    }
}

class EventListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof Event) {
            Event event = (Event) value;
            String timeStr = String.format("%02d:%02d-%02d:%02d", 
                event.getStartTime().getHour(),
                event.getStartTime().getMinute(),
                event.getEndTime().getHour(),
                event.getEndTime().getMinute());
            
            setText(String.format("%s: %s (%s)", 
                timeStr, event.getName(), event.getLocation()));
            
            setBackground(isSelected ? list.getSelectionBackground() : event.getColor());
            setForeground(isSelected ? list.getSelectionForeground() : Color.BLACK);
        }
        
        return this;
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