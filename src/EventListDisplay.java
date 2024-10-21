import java.awt.*;
import javax.swing.*;

class EventListDisplay extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof ScheduleEvent) {
            ScheduleEvent event = (ScheduleEvent) value;
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