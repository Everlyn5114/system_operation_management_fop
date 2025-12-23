package my.edu.wix1002.goldenhour.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import my.edu.wix1002.goldenhour.model.Employee;
import my.edu.wix1002.goldenhour.StorageSystem.StoreManager;

import java.time.LocalDate;
import java.time.LocalTime;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    private Employee loggedIn;

    public void initSession(Employee e) {
        this.loggedIn = e;
        welcomeLabel.setText("Welcome, " + e.getName() + " (" + e.getEmployeeID().substring(0, 3) + ")");
    }

    @FXML
    private void onClockIn() {
        try {
            String outletCode = loggedIn.getEmployeeID().substring(0,3);
            String[] record = new String[] {
                    loggedIn.getEmployeeID(),
                    LocalDate.now().toString(),
                    LocalTime.now().toString(),
                    "",
                    outletCode
            };
            StoreManager.appendAttendance(record);
            statusLabel.setText("Clock In saved at " + LocalTime.now().toString());
        } catch (Exception ex) {
            statusLabel.setText("Failed to clock in: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onClockOut() {
        try {
            String dateStr = LocalDate.now().toString();
            String timeStr = LocalTime.now().toString();
            StoreManager.updateClockOut(loggedIn.getEmployeeID(), dateStr, timeStr);
            statusLabel.setText("Clock Out saved at " + timeStr);
        } catch (Exception ex) {
            statusLabel.setText("Failed to clock out: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onViewStock() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StockView.fxml"));
            Scene scene = new Scene(loader.load());
            
            // This is the line that was failing before
            StockController controller = loader.getController();
            controller.loadData();

            Stage stage = new Stage();
            stage.setTitle("Current Stock");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onRecordSale() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SalesView.fxml"));
            Scene scene = new Scene(loader.load());
            
            // Ensure you also have SalesController.java created!
            SalesController controller = loader.getController();
            controller.initSession(loggedIn);

            Stage stage = new Stage();
            stage.setTitle("Record Sale");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogout() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}