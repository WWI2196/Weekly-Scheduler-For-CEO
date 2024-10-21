import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.swing.*;

// Class to display the weekly calender view
class WeeklyCalendarView extends JPanel {
    private LocalDate monday;
    private ArrayList<ScheduleEvent> events;
    private ScheduleManager mainFrame;
    private static final int HOUR_HEIGHT = 60;
    private static final int DAY_WIDTH = 150;
    private static final int HEADER_HEIGHT = 50; 
    private static final int TIME_COLUMN_WIDTH = 50;
    
    public WeeklyCalendarView(LocalDate monday, ArrayList<ScheduleEvent> events, ScheduleManager mainFrame) {
        this.monday = monday;
        this.events = events;
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(TIME_COLUMN_WIDTH + DAY_WIDTH * 7, 
            HOUR_HEIGHT * 12 + HEADER_HEIGHT));
        
        // To handle clicks on the events in the calender view
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleCalendarClick(e.getX(), e.getY());
            }
        });

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        ScheduleEvent event = findEventAt(e.getX(), e.getY());
        if (event != null) {
            return String.format("<html>%s<br>Location: %s<br>Time: %s - %s</html>",
                event.getName(),
                event.getLocation(),
                event.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                event.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        return null;
    }

    // Set the colors of the calender view
    @Override
    protected void paintComponent(Graphics comp) {
        super.paintComponent(comp);
        Graphics2D comp_ = (Graphics2D) comp;
        comp_.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        createCalenderGrid(comp_);
        drawTimeIndicators(comp_);
        displayScheduledEvents(comp_);
        drawCurrentTimeLine(comp_);
    }

    // Create the calender backgroud
    private void createCalenderGrid(Graphics2D grid) {
        grid.setColor(new Color(240, 240, 240));
        grid.fillRect(0, 0, getWidth(), HEADER_HEIGHT);
        grid.setColor(Color.BLACK);

        // Headers for each day
        Font headerFont = new Font("Arial", Font.BOLD, 12);
        grid.setFont(headerFont);
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            int x = TIME_COLUMN_WIDTH + (i * DAY_WIDTH);
            
            // For day name
            String dayName = date.getDayOfWeek().getDisplayName(
                TextStyle.SHORT, Locale.getDefault());
            grid.drawString(dayName, x + 5, 20);
            
            // For date
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM/dd"));
            grid.drawString(dateStr, x + 5, 40);

            grid.drawLine(x, 0, x, getHeight());

            // Highlight weekend
            if (i >= 5) {  
                grid.setColor(new Color(255, 240, 240, 100));
                grid.fillRect(x, HEADER_HEIGHT, DAY_WIDTH, getHeight() - HEADER_HEIGHT);
                grid.setColor(Color.BLACK);
            }
        }

        grid.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        // For grid hour lines
        grid.setColor(new Color(200, 200, 200));
        for (int i = 0; i <= 12; i++) {
            int y = i * HOUR_HEIGHT + HEADER_HEIGHT;
            grid.drawLine(TIME_COLUMN_WIDTH, y, getWidth(), y);
        }
    }

    // For the time indicators on the left side of the calender
    private void drawTimeIndicators(Graphics2D gridIn) {
        gridIn.setColor(Color.BLACK);
        Font timeFont = new Font("Arial", Font.PLAIN, 10);
        gridIn.setFont(timeFont);
        
        for (int i = 0; i <= 12; i++) {
            int hour = i + 8;  // From 8am
            int y = i * HOUR_HEIGHT + HEADER_HEIGHT;
            
            // For hour marks
            gridIn.drawString(String.format("%02d:00", hour), 5, y - 5);

            gridIn.setColor(new Color(200, 200, 200));
            int halfHourY = y + (HOUR_HEIGHT / 2);
            gridIn.drawLine(TIME_COLUMN_WIDTH, halfHourY, getWidth(), halfHourY);
            gridIn.setColor(Color.BLACK);
        }
    }

    // To display the sheduled events on the calender view
    private void displayScheduledEvents(Graphics2D gridEv) {
        for (ScheduleEvent event : events) {
            LocalDateTime startTime = event.getStartTime();
            LocalDateTime endTime = event.getEndTime();
            
            // Skip the event if it is not in current week
            if (!isEventWithinCurrentWeek(event)) continue;
            
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

            // To show the event in the event box
            gridEv.setColor(event.getColor());
            gridEv.fillRoundRect(x, y, DAY_WIDTH - 10, height, 10, 10);

            gridEv.setColor(event.getColor().darker());
            gridEv.drawRoundRect(x, y, DAY_WIDTH - 10, height, 10, 10);

            gridEv.setColor(Color.BLACK);
            Font eventFont = new Font("Arial", Font.BOLD, 11);
            gridEv.setFont(eventFont);

            String timeStr = String.format("%02d:%02d-%02d:%02d", 
                startTime.getHour(), startTime.getMinute(),
                endTime.getHour(), endTime.getMinute());
            gridEv.drawString(timeStr, x + 5, y + 15);

            gridEv.drawString(event.getName(), x + 5, y + 30);
        }
    }

    // To show the current time
    private void drawCurrentTimeLine(Graphics2D gridLi) {
        LocalDateTime now = LocalDateTime.now();
        if (isTimeInWorkHours(now)) {
            int day = now.getDayOfWeek().getValue() - 1;
            int hour = now.getHour() - 8;
            int minute = now.getMinute();
            
            int y = hour * HOUR_HEIGHT + (minute * HOUR_HEIGHT / 60) + HEADER_HEIGHT;

            // Current time line
            gridLi.setColor(Color.RED);
            Stroke oldStroke = gridLi.getStroke();
            gridLi.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            gridLi.drawLine(TIME_COLUMN_WIDTH, y, getWidth(), y);
            gridLi.setStroke(oldStroke);
        }
    }

    // To check the working hours
    private boolean isTimeInWorkHours(LocalDateTime time) {
        int dayOfWeek = time.getDayOfWeek().getValue();
        int hour = time.getHour();
        
        if (dayOfWeek <= 5) { 
            return hour >= 8 && hour < 20;
        } else if (dayOfWeek == 6) {
            return hour >= 8 && hour < 15;
        }
        return false; 
    }

    // To check the event is within the week
    private boolean isEventWithinCurrentWeek(ScheduleEvent event) {
        LocalDate eventDate = event.getStartTime().toLocalDate();
        LocalDate weekEnd = monday.plusDays(6);
        return !eventDate.isBefore(monday) && !eventDate.isAfter(weekEnd);
    }

    // To check if clicked in the calender view area
    private void handleCalendarClick(int x, int y) {
        
        if (y < HEADER_HEIGHT && x > TIME_COLUMN_WIDTH) {
            int day = (x - TIME_COLUMN_WIDTH) / DAY_WIDTH;
            if (day >= 0 && day < 7) {
                showDailyView(monday.plusDays(day));
                return;
            }
        }

        // To check if clicked on an sheduled event
        ScheduleEvent clickedEvent = findEventAt(x, y);
        if (clickedEvent != null) {
            openEventDetailsForm(clickedEvent);
        }
    }

    // To open the event details
    private void openEventDetailsForm(ScheduleEvent event) {
        EventDetailsForm dialog = new EventDetailsForm((ScheduleManager) SwingUtilities.getWindowAncestor(this), event);
        dialog.setVisible(true);
        if (dialog.getEvent() != null) { // Check if the evet is modified
            events.remove(event);
            events.add(dialog.getEvent());
            repaint();
        } else if (!events.contains(event)) { // Check if the event is deleted
            repaint();
        }
    }

    private ScheduleEvent findEventAt(int x, int y) {
        if (x < TIME_COLUMN_WIDTH || y < HEADER_HEIGHT) return null;
        
        int day = (x - TIME_COLUMN_WIDTH) / DAY_WIDTH;
        double hour = (double)(y - HEADER_HEIGHT) / HOUR_HEIGHT + 8;
        
        LocalDateTime clickTime = monday.plusDays(day)
            .atTime((int)hour, (int)((hour % 1) * 60));

        for (ScheduleEvent event : events) {
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

        DefaultListModel<ScheduleEvent> model = new DefaultListModel<>();
        for (ScheduleEvent event : events) {
            if (event.getStartTime().toLocalDate().equals(date)) {
                model.addElement(event);
            }
        }

        JList<ScheduleEvent> eventList = new JList<>(model);
        eventList.setCellRenderer(new EventListDisplay());
        eventList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ScheduleEvent selected = eventList.getSelectedValue();
                    if (selected != null) {
                        dailyView.dispose();
                        openEventDetailsForm(selected);
                    }
                }
            }
        });

        contentPanel.add(new JScrollPane(eventList), BorderLayout.CENTER);

        // To add a label showing work hours when sheduling
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

    private boolean isTimeInEvent(LocalDateTime time, ScheduleEvent event) {
        return !time.isBefore(event.getStartTime()) && 
               !time.isAfter(event.getEndTime());
    }
}