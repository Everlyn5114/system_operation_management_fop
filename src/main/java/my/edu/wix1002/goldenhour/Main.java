package my.edu.wix1002.goldenhour; 

import my.edu.wix1002.goldenhour.model.Employee;
import my.edu.wix1002.goldenhour.model.Model;
import my.edu.wix1002.goldenhour.model.Outlet;
import my.edu.wix1002.goldenhour.util.DataLoader;
import java.util.List;
import com.opencsv.exceptions.CsvValidationException;

// import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws CsvValidationException{
        System.out.println("--- Starting Store Operations System ---");

        // Load all initial data from CSV files (Data Load State)
        List<Employee> allEmployees = DataLoader.loadEmployees();
        List<Outlet> allOutlets = DataLoader.loadOutlets();
        List<Model> allModels = DataLoader.loadModels();

        if (!allEmployees.isEmpty()) {
            System.out.println("First employee loaded: " + allEmployees.get(0));
        } else {
            System.out.println("No employees loaded.");
        }

        if (!allOutlets.isEmpty()) {
            System.out.println("First outlet loaded: " + allOutlets.get(0));
        } else {
            System.out.println("No outlets loaded. Check CSV file path/content.");
        }

        if (!allModels.isEmpty()) {
            System.out.println("First model loaded: " + allModels.get(0));
        } else {
            System.out.println("No models loaded. Check CSV file path/content.");
        }
        
        // The main menu/login loop will start here 
    }
}