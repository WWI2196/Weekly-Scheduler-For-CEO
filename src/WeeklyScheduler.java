import javax.swing.*;

// Main class to start the application
public class WeeklyScheduler {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ScheduleManager().setVisible(true);
        });
    }
}
