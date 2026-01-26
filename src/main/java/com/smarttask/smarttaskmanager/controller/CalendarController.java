package com.smarttask.smarttaskmanager.controller;

import com.smarttask.smarttaskmanager.util.DatabaseConnection;
import com.smarttask.smarttaskmanager.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class CalendarController {

    @FXML private Text yearText;
    @FXML private Text monthText;
    @FXML private FlowPane calendarLayout;
    @FXML private Label yearMonthLabel;

    private YearMonth currentYearMonth;
    private String userEmail;

    @FXML
    public void initialize() {
        // Njibu User
        UserSession session = UserSession.getInstance();
        if (session != null) {
            this.userEmail = session.getEmail();
        }

        // Nebdaw b chher l'7ali (daba)
        currentYearMonth = YearMonth.now();
        drawCalendar();
    }

    // --- RESSM L'CALENDRIER ---
    private void drawCalendar() {
        calendarLayout.getChildren().clear(); // Nmss7u l9dim

        // Update Label lfo9 (ex: January 2026)
        yearMonthLabel.setText(currentYearMonth.getMonth().toString() + " " + currentYearMonth.getYear());

        // N3rfu chher bach kaybda (Ltnin, tlat...?)
        LocalDate calendarDate = LocalDate.of(currentYearMonth.getYear(), currentYearMonth.getMonthValue(), 1);

        // Loop bach n3mmru khawyin 9bel 1er (ila bda chher nhar Larb3a, khassna nkhlliw tnin w tlat khawyin)
        int dayOfWeek = calendarDate.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        for (int i = 1; i < dayOfWeek; i++) {
            StackPane emptyPane = new StackPane();
            emptyPane.setPrefSize(100, 80);
            calendarLayout.getChildren().add(emptyPane);
        }

        // Loop bach nrssmu Ayyamat Chher (1 -> 30/31)
        int daysInMonth = currentYearMonth.lengthOfMonth();
        for (int i = 1; i <= daysInMonth; i++) {

            LocalDate date = LocalDate.of(currentYearMonth.getYear(), currentYearMonth.getMonth(), i);

            // 1. La Boite dyal nhar
            StackPane dayPane = new StackPane();
            dayPane.setPrefSize(100, 80);
            dayPane.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);");

            // 2. Nmra dyal nhar (Top Left)
            Label dayNumber = new Label(String.valueOf(i));
            dayNumber.setFont(Font.font("System Bold", 14));
            StackPane.setAlignment(dayNumber, Pos.TOP_LEFT);
            dayNumber.setTranslateX(5);
            dayNumber.setTranslateY(5);

            // 3. Check wach kayn TACHE f had nhar?
            int taskCount = getTasksCountForDate(date);
            if (taskCount > 0) {
                Label taskIndicator = new Label(taskCount + " Tasks");
                taskIndicator.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 2 5; -fx-font-size: 10px;");
                StackPane.setAlignment(taskIndicator, Pos.BOTTOM_RIGHT);
                taskIndicator.setTranslateX(-5);
                taskIndicator.setTranslateY(-5);
                dayPane.getChildren().add(taskIndicator);
            }

            // Ila kan lyoum, ndiru lih couleur spécial
            if (date.equals(LocalDate.now())) {
                dayPane.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #3498db; -fx-border-width: 2; -fx-background-radius: 5;");
            }

            dayPane.getChildren().add(dayNumber);
            calendarLayout.getChildren().add(dayPane);
        }
    }

    // --- QUERY DATABASE ---
    private int getTasksCountForDate(LocalDate date) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM tasks WHERE deadline = ? AND user_email = ?";
        Connection connect = DatabaseConnection.getInstance().getConnection();

        try {
            PreparedStatement prepare = connect.prepareStatement(sql);
            prepare.setDate(1, java.sql.Date.valueOf(date));
            prepare.setString(2, userEmail);

            ResultSet rs = prepare.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    // --- NAVIGATION ---
    @FXML
    public void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        drawCalendar();
    }

    @FXML
    public void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        drawCalendar();
    }

    // Navigation Menu (Standard)
    @FXML public void goToDashboard(ActionEvent event) throws IOException { navigate(event, "/com/smarttask/smarttaskmanager/view/dashboard.fxml"); }
    @FXML public void goToTasks(ActionEvent event) throws IOException { navigate(event, "/com/smarttask/smarttaskmanager/view/tasks.fxml"); }
    @FXML public void goToProfile(ActionEvent event) throws IOException { navigate(event, "/com/smarttask/smarttaskmanager/view/profile.fxml"); }
    @FXML public void handleLogout(ActionEvent event) throws IOException { UserSession.getInstance().cleanUserSession(); navigate(event, "/com/smarttask/smarttaskmanager/view/login.fxml"); }

    private void navigate(ActionEvent event, String path) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(new FXMLLoader(getClass().getResource(path)).load()));
        stage.show();
    }
}