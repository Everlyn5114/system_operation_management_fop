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
import my.edu.wix1002.goldenhour.StorageSystem.StoreManager;

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

        // check if employee already clocked in today
        String[] todayRecord = findTodayRecord(employee.getEmployeeID(), today);
        if (todayRecord != null && !todayRecord[2].isEmpty() && todayRecord[3].isEmpty()) { //if clocked in exists and no clock out yet, print output to user
            System.out.println("\nError: You have already clocked in today at " 
                    + formatTimeForDisplay(todayRecord[2]));
            return;
        }

        if (todayRecord != null && !todayRecord[3].isEmpty()) {
            System.out.println("\nError: You have already clocked in and out today.");
            return;
        }

        // find outlet information
        String outletCode = employee.getEmployeeID().substring(0, 3);
        String outletName = findOutletName(outletCode, allOutlets);

        // save attendance record via StoreManager
        saveClockIn(employee.getEmployeeID(), today, now, outletCode); //today is LocalDate.now() and now is LocalTime.now() and outletCode is first 3 chars of employeeID and pass all to saveClockIn method

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
        LocalDate today = LocalDate.now(); //current date
        LocalTime now = LocalTime.now(); //current time

        // find today's record
        String[] todayRecord = findTodayRecord(employee.getEmployeeID(), today);

        if (todayRecord == null || todayRecord[2].isEmpty()) {
            System.out.println("\nError: You have not clocked in today. Please clock in first.");
            return; //if no record at all or clock in time is empty return output to user
        }

        if (!todayRecord[3].isEmpty()) { //if clock out time already exists, output to user
            System.out.println("\nError: You have already clocked out today at " 
                    + formatTimeForDisplay(todayRecord[3]));
            return;
        }

        // calculate hours worked
        //
        LocalTime clockInTime = LocalTime.parse(todayRecord[2], TIME_FMT); //parse clock in time from string to LocalTime and time format & Converts a string into a LocalTime object.
        //Now, clockInTime is a LocalTime object 
        //need to convert to LocalTime object as clock-in time from the CSV is stored as a string, string cant do calculations
        //once converted, can easily perform calculations and format it.
        double hoursWorked = calculateHoursWorked(clockInTime, now); //calls calculateHoursWorked method, now is current time

        // Find outlet information
        String outletCode = employee.getEmployeeID().substring(0, 3);
        String outletName = findOutletName(outletCode, allOutlets);

        // update record with clock out time
        updateClockOut(employee.getEmployeeID(), today, now);

        // display success message with hours worked
        System.out.println("\n=== Attendance Clock Out ===");
        System.out.println("Employee ID: " + employee.getEmployeeID());
        System.out.println("Name: " + employee.getName());
        System.out.println("Outlet: " + outletCode + " (" + outletName + ")");
        System.out.println("\nClock Out Successful!");
        System.out.println("Date: " + today.format(DATE_FMT));
        System.out.println("Time: " + formatTimeForDisplay(now));
        System.out.println("Total Hours Worked: " + String.format("%.1f", hoursWorked) + " hours");
    }

    // find today's attendance record for an employee based on the specific date(returns [ID, Date, ClockInTime, ClockOutTime, OutletCode])
    //remove extra spaces and quotes from each field
    private static String[] findTodayRecord(String employeeID, LocalDate date) {
        try {
            //Read all lines from the CSV
            List<String> lines = Files.readAllLines(Paths.get(ATTENDANCE_FILE_PATH)); //reads the entire attendance.csv into a List of strings,
            String dateStr = date.format(DATE_FMT); //format LocalDate to string using DATE_FMT
            
            //loop through each line (skip header)
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);  //splits the line into columns using commas. & -1 ensures that if the line ends with empty columns, we still get them (important for ClockOutTime, which may be empty).

                //clean up each field
                for (int p = 0; p < parts.length; p++) {
                    if (parts[p] != null) { //removes extra spaces from each field.
                        parts[p] = parts[p].trim();
                        if (parts[p].length() >= 2 && parts[p].startsWith("\"") && parts[p].endsWith("\"")) { //this condition is true if the field is wrapped in quotes
                            parts[p] = parts[p].substring(1, parts[p].length() - 1); //removing the starting and ending quotes and takes the characters from index 1 to length-2,
                        }
                    }
                }

                if (parts.length >= 3 && parts[0].equals(employeeID) && parts[1].equals(dateStr)) { //part 0 is EmployeeID, part 1 is Date, if both match, we found today's record
                    // ensure we always have 5 elements (EmployeeID, Date, ClockInTime, ClockOutTime, OutletCode)
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
                    return parts; //returns the cleaned and padded array back to where the method was called. (at clockIn or clockOut methods)
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
            if (outlet.getOutletCode().equals(outletCode)) {  //if outlet code matches with the provided outletCode
                return outlet.getOutletName(); //return the corresponding outlet name
            }
        }
        return "Unknown Outlet";
    }

    // Save clock in record
    private static void saveClockIn(String employeeID, LocalDate date, LocalTime clockInTime, String outletCode) {
        try {
            String[] record = new String[]{
                employeeID,
                date.format(DATE_FMT), //format it 
                clockInTime.format(TIME_FMT),
                "", //clock out time empty
                outletCode //Outlet code extracted from Employee ID (first 3 letters
            };
            StoreManager.appendAttendance(record);
        } catch (Exception e) {
            System.err.println("Error saving clock in record: " + e.getMessage());
        }
    }

    // Update record with clock out time
    private static void updateClockOut(String employeeID, LocalDate date, LocalTime clockOutTime) {
        try {
            String dateStr = date.format(DATE_FMT);
            String timeStr = clockOutTime.format(TIME_FMT);

            // First attempt: call StoreManager with unquoted values
            StoreManager.updateClockOut(employeeID, dateStr, timeStr);

            // Verify update persisted
            String[] after = findTodayRecord(employeeID, date);
            if (after != null && !after[3].isEmpty()) return;

            // second attempt: some CSV rows are quoted. try with quoted employeeID/date
            // StoreManager.updateClockOut("\"" + employeeID + "\"", "\"" + dateStr + "\"", timeStr);

            after = findTodayRecord(employeeID, date);
            if (after == null || after[3].isEmpty()) {
                System.err.println("Warning: clock out update may not have been saved.");
            }

        } catch (Exception e) {
            System.err.println("Error updating clock out record: " + e.getMessage());
        }
    }

    // calculate hours worked
    private static double calculateHoursWorked(LocalTime clockIn, LocalTime clockOut) { //receives clock in and clock out time as LocalTime objects
        long seconds = java.time.temporal.ChronoUnit.SECONDS.between(clockIn, clockOut);
        //Example: clockIn = 09:15:00, clockOut = 17:45:00
        //Difference = 8 hours 30 minutes = 83600 + 3060 = 30,600 seconds
        double hours = seconds / 3600.0; 
        //convert seconds to hours by dividing by 3600.0
        //30,600 / 3600 = 8.5 hours
        return Math.round(hours * 10.0) / 10.0;  // round to 1 decimal place
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
