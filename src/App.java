import java.time.*;

public class App {
    public static void main(String[] args) throws Exception {
        LocalTime initalTime = java.time.LocalDateTime.now().toLocalTime();
        System.out.println(ConsoleColors.YELLOW_BOLD + "\nStart time: " + initalTime + "\n" + ConsoleColors.RESET);
        Initializer init = new Initializer(args);
        try {
            init.Initialize();
            LocalTime endTime = java.time.LocalDateTime.now().toLocalTime();
            System.out.println(ConsoleColors.YELLOW_BOLD + "\nFinal time: " + endTime + ConsoleColors.RESET);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
