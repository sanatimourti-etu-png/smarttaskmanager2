package com.smarttask.smarttaskmanager.controller;

import com.smarttask.smarttaskmanager.DAO.TaskDAO;
import com.smarttask.smarttaskmanager.model.Task;
import com.smarttask.smarttaskmanager.service.AIService;
import com.smarttask.smarttaskmanager.service.NotificationService;
import com.smarttask.smarttaskmanager.util.DatabaseConnection;
import com.smarttask.smarttaskmanager.util.UserSession;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
// 👇 ZIDNA HADU (Imports d l-Popup)
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class DashboardController {

    @FXML private Label lblEnCours;
    @FXML private Label lblTerminees;
    @FXML private Label lblEnRetard;
    @FXML private Label aiSuggestionLabel;
    @FXML private PieChart pieChartPriority;
    @FXML private BarChart<String, Number> productivityChart;
    @FXML private Button btnMyTasks;

    private NotificationService notifService;

    @FXML
    public void initialize() {
        System.out.println("🚀 DÉMARRAGE DU DASHBOARD");

        // UI Check
        if (aiSuggestionLabel != null) {
            aiSuggestionLabel.setText("System Ready");
        }

        updateDashboardKPIs();
        loadPieChartData();
        loadPerformanceTrends();
        checkNotifications();
        startNotificationService();
        loadAIInsights();
    }

    private void startNotificationService() {
        try {
            notifService = new NotificationService(this);
            notifService.startService();
            System.out.println("✅ Service Notification : DÉMARRÉ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ MODIFIÉ : Zidna l-Appel l showInvitationDialog
    public void addNotificationToQueue(int taskId, String type, String message, boolean isUrgent) {
        System.out.println("📞 Reçu notification : " + message);
        Platform.runLater(() -> {
            if (aiSuggestionLabel != null) {
                aiSuggestionLabel.setText("🔔 " + message);
                aiSuggestionLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: #e74c3c; -fx-padding: 5px;");
            }

            // 👇 HNA FIN KAN-TEL3O L-POPUP
            if (type.equals("INVITE")) {
                showInvitationDialog(taskId, message);
            }
        });
    }

    // ✅ NOUVEAU : Popup bach t-Accepti l-invitation
    private void showInvitationDialog(int taskId, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("📩 Invitation Reçue");
        alert.setHeaderText("Nouvelle Tâche Partagée");
        alert.setContentText(message + "\n\nVoulez-vous accepter cette tâche ?");

        ButtonType btnAccept = new ButtonType("Accepter");
        ButtonType btnDecline = new ButtonType("Refuser");
        ButtonType btnLater = new ButtonType("Plus tard", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnAccept, btnDecline, btnLater);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnAccept) {
                updateShareStatus(taskId, "ACCEPTED");
            } else if (response == btnDecline) {
                updateShareStatus(taskId, "DECLINED");
            }
        });
    }

    // ✅ NOUVEAU : Update SQL Status
    private void updateShareStatus(int taskId, String status) {
        String sql = "UPDATE tasks SET share_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, status);
            pst.setInt(2, taskId);
            pst.executeUpdate();

            Platform.runLater(() -> {
                if (aiSuggestionLabel != null) {
                    aiSuggestionLabel.setText("✅ Tâche " + status + " !");
                    aiSuggestionLabel.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5px;");
                }
                updateDashboardKPIs(); // Refresh KPIs
                checkNotifications();  // Refresh Badge
            });
            System.out.println("✅ Tâche " + taskId + " : " + status);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LE RESTE DU CODE (SANS CHANGEMENT) ---
    private void loadAIInsights() {
        new Thread(() -> {
            try {
                TaskDAO taskDAO = new TaskDAO();
                List<Task> tasks = taskDAO.getAllTasks();
                AIService aiService = new AIService();
                String insight = aiService.getProductivityInsights(tasks);
                Platform.runLater(() -> {
                    if (aiSuggestionLabel != null && !aiSuggestionLabel.getText().startsWith("🔔")) {
                        aiSuggestionLabel.setText("💡 AI Tip: " + insight);
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadPerformanceTrends() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tasks Completed");
        String sql = "SELECT deadline, COUNT(*) as total FROM tasks WHERE status = 'Completed' GROUP BY deadline ORDER BY deadline LIMIT 7";
        try (Connection connect = DatabaseConnection.getInstance().getConnection();
             PreparedStatement prepare = connect.prepareStatement(sql)) {
            ResultSet result = prepare.executeQuery();
            while (result.next()) {
                series.getData().add(new XYChart.Data<>(result.getString("deadline"), result.getInt("total")));
            }
            if (productivityChart != null) productivityChart.getData().add(series);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void checkNotifications() {
        if(UserSession.getInstance() == null) return;
        String currentUser = UserSession.getInstance().getEmail();
        String sql = "SELECT COUNT(*) FROM tasks WHERE shared_with = ? AND status != 'Completed'";
        try (Connection connect = DatabaseConnection.getInstance().getConnection();
             PreparedStatement prepare = connect.prepareStatement(sql)) {
            prepare.setString(1, currentUser);
            ResultSet rs = prepare.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                if (btnMyTasks != null) {
                    btnMyTasks.setText("My Tasks (" + rs.getInt(1) + ")");
                    btnMyTasks.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void goToDashboard(ActionEvent event) { System.out.println("Déjà sur le Dashboard"); }
    @FXML public void goToCalendar(ActionEvent event) { navigate(event, "/com/smarttask/smarttaskmanager/view/calendar_view.fxml", "Calendrier"); }
    @FXML public void goToTasks(ActionEvent event) { navigate(event, "/com/smarttask/smarttaskmanager/view/tasks.fxml", "Mes Tâches"); }
    @FXML public void goToProfile(ActionEvent event) { navigate(event, "/com/smarttask/smarttaskmanager/view/profile.fxml", "Profil"); }
    @FXML public void handleLogout(ActionEvent event) {
        if (notifService != null) notifService.stopService();
        UserSession.getInstance().cleanUserSession();
        navigate(event, "/com/smarttask/smarttaskmanager/view/login.fxml", "Login");
    }

    @FXML public void handleNewTask(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smarttask/smarttaskmanager/view/add_task.fxml"));
            Stage s = new Stage(); s.setScene(new Scene(loader.load())); s.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigate(ActionEvent event, String fxmlPath, String title) {
        try {
            if (notifService != null) notifService.stopService();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { System.out.println("Erreur Navigation: " + fxmlPath); }
    }

    private void updateDashboardKPIs() {
        try (Connection connect = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connect.createStatement()) {
            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM tasks WHERE status = 'In Progress'");
            if(rs1.next()) lblEnCours.setText(String.valueOf(rs1.getInt(1)));
            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) FROM tasks WHERE status = 'Completed'");
            if(rs2.next()) lblTerminees.setText(String.valueOf(rs2.getInt(1)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPieChartData() {
        try (Connection connect = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connect.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT priority, COUNT(*) as count FROM tasks GROUP BY priority");
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            while (rs.next()) {
                pieData.add(new PieChart.Data(rs.getString("priority") + " (" + rs.getInt("count") + ")", rs.getInt("count")));
            }
            pieChartPriority.setData(pieData);
        } catch (Exception e) { e.printStackTrace(); }
    }
}