import java.awt.*;
import java.time.*;
import java.util.*;
import javax.swing.*;

// Class for the event details form to create and edit events
class EventDetailsForm extends JDialog {
    private ScheduleEvent result;
    private ScheduleEvent originalEvent;
    private JTextField nameField;
    private JTextField locationField;
    private JComboBox<String> colorBox;
    private JSpinner dateSpinner;
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    
    // Colors for the events
    private final Color[] EVENT_COLORS = {
        Color.RED, Color.GREEN, Color.YELLOW, 
        Color.BLUE, Color.ORANGE, Color.GRAY
    };
    
    // Colors for the sheduler form window
    private final Color[] WINDOW_COLORS = {
        new Color(255, 200, 200), // Light Red
        new Color(200, 255, 200), // Light Green
        new Color(255, 255, 200), // Light Yellow
        new Color(200, 200, 255), // Light Blue
        new Color(255, 225, 200), // Light Orange
        new Color(225, 225, 225)  // Light Gray
    };
    private ScheduleManager scheduleManager;
    
    public EventDetailsForm(ScheduleManager owner, ScheduleEvent event) {
        super(owner, event == null ? "New Event" : "Edit Event", true);
        this.originalEvent = event;
        this.scheduleManager = owner;
        setUserInterface();
        if (event != null) {
            populateFields(event);
        }
    }

     // Set the user interface for the form
    private void setUserInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc_ = new GridBagConstraints();
        gbc_.insets = new Insets(5, 5, 5, 5);
        gbc_.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(32);
        locationField = new JTextField(32);
        
        String[] colorNames = {"Red", "Green", "Yellow", "Blue", "Orange", "Gray"};
        colorBox = new JComboBox<>(colorNames);
        colorBox.addActionListener(e -> updateWindowColor());

        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));

        startTimeSpinner = new JSpinner(new SpinnerDateModel());
        startTimeSpinner.setEditor(new JSpinner.DateEditor(startTimeSpinner, "HH:mm"));

        endTimeSpinner = new JSpinner(new SpinnerDateModel());
        endTimeSpinner.setEditor(new JSpinner.DateEditor(endTimeSpinner, "HH:mm"));

        gbc_.gridx = 0; gbc_.gridy = 0;
        add(new JLabel("Event Name:"), gbc_);
        
        gbc_.gridx = 1;
        add(nameField, gbc_);

        gbc_.gridx = 0; gbc_.gridy = 1;
        add(new JLabel("Location:"), gbc_);
        
        gbc_.gridx = 1;
        add(locationField, gbc_);

        gbc_.gridx = 0; gbc_.gridy = 2;
        add(new JLabel("Date:"), gbc_);
        
        gbc_.gridx = 1;
        add(dateSpinner, gbc_);

        gbc_.gridx = 0; gbc_.gridy = 3;
        add(new JLabel("Start Time:"), gbc_);
        
        gbc_.gridx = 1;
        add(startTimeSpinner, gbc_);

        gbc_.gridx = 0; gbc_.gridy = 4;
        add(new JLabel("End Time:"), gbc_);
        
        gbc_.gridx = 1;
        add(endTimeSpinner, gbc_);

        gbc_.gridx = 0; gbc_.gridy = 5;
        add(new JLabel("Color:"), gbc_);
        
        gbc_.gridx = 1;
        add(colorBox, gbc_);

        // To Add buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton deleteButton = new JButton("Delete");

        okButton.addActionListener(e -> {
            if (saveEventDetails()) {
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        deleteButton.addActionListener(e -> {
            if (originalEvent != null) {
                scheduleManager.deleteEvent(originalEvent);
                dispose();
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        if (originalEvent != null) {
            buttonPanel.add(deleteButton);
        }

        gbc_.gridx = 0; gbc_.gridy = 6;
        gbc_.gridwidth = 2;
        add(buttonPanel, gbc_);

        pack();
        setLocationRelativeTo(getOwner());
        updateWindowColor();
    }
    
    // Update the window color based on the selected event color
    private void updateWindowColor() {
        Color selectedColor = WINDOW_COLORS[colorBox.getSelectedIndex()];
        this.getContentPane().setBackground(selectedColor);
        this.repaint();
    }

    // Populate form fields with existing event form
    private void populateFields(ScheduleEvent event) {
        nameField.setText(event.getName());
        locationField.setText(event.getLocation());
        
        // Set date
        dateSpinner.setValue(java.sql.Timestamp.valueOf(
            event.getStartTime().toLocalDate().atStartOfDay()));
        
        // Set times
        startTimeSpinner.setValue(java.sql.Timestamp.valueOf(event.getStartTime()));
        endTimeSpinner.setValue(java.sql.Timestamp.valueOf(event.getEndTime()));
        
        // Set color
        for (int i = 0; i < EVENT_COLORS.length; i++) {
            if (EVENT_COLORS[i].equals(event.getColor())) {
                colorBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private boolean saveEventDetails() {
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
            LocalDate date = dateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            // Get times from time spinners
            Date startValue = (Date) startTimeSpinner.getValue();
            Date endValue = (Date) endTimeSpinner.getValue();
            
            LocalTime startTime = startValue.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            LocalTime endTime = endValue.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

            // Create LocalDateTime objects
            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);

            // Get selected color
            Color color = EVENT_COLORS[colorBox.getSelectedIndex()];

            // Create new event
            if (originalEvent != null) {
                originalEvent.setName(name);
                originalEvent.setLocation(location);
                originalEvent.setStartTime(startDateTime);
                originalEvent.setEndTime(endDateTime);
                originalEvent.setColor(color);
                result = originalEvent;
                scheduleManager.updateEvent(result);
            } else {
                result = new ScheduleEvent(name, location, startDateTime, endDateTime, color);
                scheduleManager.addNewEvent(result);
            }
            
            return true;
            
        } catch (HeadlessException e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid input: " + e.getMessage());
            return false;
        }
    }

    public ScheduleEvent getEvent() {
        return result;
    }
}

