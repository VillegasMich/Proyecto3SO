import java.time.*;

public class App {
    public static void main(String[] args) throws Exception {
        LocalTime initalTime = java.time.LocalDateTime.now().toLocalTime();
        System.out.println("Start time: " + initalTime + "\n");
        Initializer init = new Initializer(args);
        try {
            init.Initialize();
            LocalTime endTime = java.time.LocalDateTime.now().toLocalTime();
            System.out.println("\nFinal time: " + endTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
