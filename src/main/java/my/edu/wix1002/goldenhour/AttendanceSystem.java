package my.edu.wix1002.goldenhour;

import my.edu.wix1002.goldenhour.model.Employee;
import my.edu.wix1002.goldenhour.model.Outlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class AttendanceSystem {
    private static final String ATTENDANCE_FILE_PATH = "data/attendance.csv";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Initialize attendance file if it doesn't exist
    static {
        try {
            if (!Files.exists(Paths.get(ATTENDANCE_FILE_PATH))) {
                Files.write(Paths.get(ATTENDANCE_FILE_PATH), 
                           "EmployeeID,Date,ClockInTime,ClockOutTime,OutletCode\n".getBytes(),
                           StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            System.err.println("Error initializing attendance file: " + e.getMessage());
        }
    }

    public static void showAttendanceMenu(Scanner input, Employee loggedInEmployee, List<Outlet> allOutlets) {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Attendance Menu ===");
            System.out.println("1. Clock In");
            System.out.println("2. Clock Out");
            System.out.println("3. Back to Main Menu");
            System.out.print("Enter choice: ");

            String choice = input.nextLine().trim();
            switch (choice) {
                case "1":
                    clockIn(loggedInEmployee, allOutlets);
                    break;
                case "2":
                    clockOut(loggedInEmployee, allOutlets);
                    break;
                case "3":
                    running = false;
                    System.out.println("Returning to Employee Menu...");
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    public static void clockIn(Employee employee, List<Outlet> allOutlets) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Check if employee already clocked in today
        String[] todayRecord = findTodayRecord(employee.getEmployeeID(), today);
        if (todayRecord != null && !todayRecord[2].isEmpty() && todayRecord[3].isEmpty()) {
            System.out.println("\n❌ Error: You have already clocked in today at " 
                    + formatTimeForDisplay(todayRecord[2]));
            return;
        }

        if (todayRecord != null && !todayRecord[3].isEmpty()) {
            System.out.println("\n❌ Error: You have already clocked in and out today.");
            return;
        }

        // Find outlet information
        String outletCode = employee.getEmployeeID().substring(0, 3);
        String outletName = findOutletName(outletCode, allOutlets);

        // Save attendance record
        saveClockIn(employee.getEmployeeID(), today, now, outletCode);

        // Display success message
        System.out.println("\n=== Attendance Clock In ===");
        System.out.println("Employee ID: " + employee.getEmployeeID());
        System.out.println("Name: " + employee.getName());
        System.out.println("Outlet: " + outletCode + " (" + outletName + ")");
        System.out.println("\n Clock In Successful!");
        System.out.println("Date: " + today.format(DATE_FMT));
        System.out.println("Time: " + formatTimeForDisplay(now));
    }

    public static void clockOut(Employee employee, List<Outlet> allOutlets) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Find today's record
        String[] todayRecord = findTodayRecord(employee.getEmployeeID(), today);

        if (todayRecord == null || todayRecord[2].isEmpty()) {
            System.out.println("\n❌ Error: You have not clocked in today. Please clock in first.");
            return;
        }

        if (!todayRecord[3].isEmpty()) {
            System.out.println("\n❌ Error: You have already clocked out today at " 
                    + formatTimeForDisplay(todayRecord[3]));
            return;
        }

        // Calculate hours worked
        LocalTime clockInTime = LocalTime.parse(todayRecord[2], TIME_FMT);
        double hoursWorked = calculateHoursWorked(clockInTime, now);

        // Find outlet information
        String outletCode = employee.getEmployeeID().substring(0, 3);
        String outletName = findOutletName(outletCode, allOutlets);

        // Update record with clock out time
        updateClockOut(employee.getEmployeeID(), today, now);

        // Display success message with hours worked
        System.out.println("\n=== Attendance Clock Out ===");
        System.out.println("Employee ID: " + employee.getEmployeeID());
        System.out.println("Name: " + employee.getName());
        System.out.println("Outlet: " + outletCode + " (" + outletName + ")");
        System.out.println("\nClock Out Successful!");
        System.out.println("Date: " + today.format(DATE_FMT));
        System.out.println("Time: " + formatTimeForDisplay(now));
        System.out.println("Total Hours Worked: " + String.format("%.1f", hoursWorked) + " hours");
    }

    // Find today's attendance record for an employee (returns [ID, Date, ClockInTime, ClockOutTime, OutletCode])
    private static String[] findTodayRecord(String employeeID, LocalDate date) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(ATTENDANCE_FILE_PATH));
            String dateStr = date.format(DATE_FMT);
            
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);  // Use -1 to keep trailing empty strings
                if (parts.length >= 3 && parts[0].equals(employeeID) && parts[1].equals(dateStr)) {
                    // Ensure we always have 5 elements (EmployeeID, Date, ClockInTime, ClockOutTime, OutletCode)
                    if (parts.length < 5) {
                        String[] padded = new String[5];
                        for (int j = 0; j < parts.length; j++) {
                            padded[j] = parts[j];
                        }
                        for (int j = parts.length; j < 5; j++) {
                            padded[j] = "";
                        }
                        parts = padded;
                    }
                    return parts;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading attendance file: " + e.getMessage());
        }
        return null;
    }

    // Find outlet name by outlet code
    private static String findOutletName(String outletCode, List<Outlet> allOutlets) {
        for (Outlet outlet : allOutlets) {
            if (outlet.getOutletCode().equals(outletCode)) {
                return outlet.getOutletName();
            }
        }
        return "Unknown Outlet";
    }

    // Save clock in record
    private static void saveClockIn(String employeeID, LocalDate date, LocalTime clockInTime, String outletCode) {
        try {
            String record = String.format("%s,%s,%s,%s,%s%n", 
                    employeeID, 
                    date.format(DATE_FMT), 
                    clockInTime.format(TIME_FMT),
                    "",  // Empty clock out time
                    outletCode);
            
            Files.write(Paths.get(ATTENDANCE_FILE_PATH), 
                       record.getBytes(), 
                       StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error saving clock in record: " + e.getMessage());
        }
    }

    // Update record with clock out time
    private static void updateClockOut(String employeeID, LocalDate date, LocalTime clockOutTime) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(ATTENDANCE_FILE_PATH));
            String dateStr = date.format(DATE_FMT);
            
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",", -1);
                
                if (parts.length >= 3 && parts[0].equals(employeeID) && parts[1].equals(dateStr)) {
                    // Update this line with clock out time, preserving outlet code
                    String outletCode = (parts.length >= 5) ? parts[4] : "";
                    lines.set(i, String.format("%s,%s,%s,%s,%s", 
                            parts[0], parts[1], parts[2], clockOutTime.format(TIME_FMT), outletCode));
                    break;
                }
            }
            
            Files.write(Paths.get(ATTENDANCE_FILE_PATH), lines);
        } catch (IOException e) {
            System.err.println("Error updating clock out record: " + e.getMessage());
        }
    }

    // Calculate hours worked
    private static double calculateHoursWorked(LocalTime clockIn, LocalTime clockOut) {
        long seconds = java.time.temporal.ChronoUnit.SECONDS.between(clockIn, clockOut);
        double hours = seconds / 3600.0;
        return Math.round(hours * 10.0) / 10.0;  // Round to 1 decimal place
    }

    // Format time for display (e.g., "09:58 a.m.")
    private static String formatTimeForDisplay(String timeStr) {
        try {
            LocalTime time = LocalTime.parse(timeStr, TIME_FMT);
            return time.format(DISPLAY_FMT);
        } catch (Exception e) {
            return timeStr;
        }
    }

    private static String formatTimeForDisplay(LocalTime time) {
        return time.format(DISPLAY_FMT);
    }
}
