import java.awt.*;
import java.io.*;
import java.time.*;

// Class for a single event in the schedule
class ScheduleEvent implements Serializable {
    private String name;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Color color;

    // Constructor with validation for name and location length
    public ScheduleEvent(String name, String location, LocalDateTime startTime, 
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
