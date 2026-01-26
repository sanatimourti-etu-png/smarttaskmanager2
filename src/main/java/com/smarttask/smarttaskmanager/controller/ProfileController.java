package com.smarttask.smarttaskmanager.controller;

import com.smarttask.smarttaskmanager.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileController {

    // --- FXML FIELDS ---
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextArea bioField;
    @FXML private Circle profileImage;

    // Password Fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Variable bach n3rfu chkun connecté (Email d l'utilisateur)
    // F l'ghalib katjibih mn Session, hna ghadi nkhdmu b emailField
    private String userEmail;

    @FXML
    public void initialize() {
        // 1. Initialiser l'data (Normalement hadchi kayji mn LoginSession)
        // Daba ghandiru exemple statique, walakin ila 3ndk Session, jibih mn tmak
        userEmail = "test@email.com"; // <-- HADA LI GHAYTBDDEL MOT DE PASS DYALO

        usernameField.setText("Student Master");
        emailField.setText(userEmail);
        bioField.setText("Developing Smart Task Manager Project.");

        // Image par défaut (Optional)
        // profileImage.setFill(new ImagePattern(new Image("...")));
    }

    // --- 1. CHANGE PHOTO ---
    @FXML
    public void handleImportImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                profileImage.setFill(new ImagePattern(image));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- 2. SAVE INFO (Update Name/Bio) ---
    @FXML
    public void saveInfo(ActionEvent event) {
        String newUsername = usernameField.getText();
        String newBio = bioField.getText();

        String sql = "UPDATE users SET username = ?, bio = ? WHERE email = ?";
        Connection connect = DatabaseConnection.getInstance().getConnection();

        try {
            PreparedStatement prepare = connect.prepareStatement(sql);
            prepare.setString(1, newUsername);
            prepare.setString(2, newBio);
            prepare.setString(3, userEmail);

            int result = prepare.executeUpdate();
            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Informations mises à jour !");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 3. UPDATE PASSWORD (HNA L'MA39OUL) ---
    @FXML
    public void updatePassword(ActionEvent event) {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        // A. Vérifications lwalin
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les nouveaux mots de passe ne correspondent pas !");
            return;
        }

        Connection connect = DatabaseConnection.getInstance().getConnection();

        try {
            // B. Vérifier wach Mot de passe l9dim s7i7?
            String checkSql = "SELECT password FROM users WHERE email = ?";
            PreparedStatement checkStmt = connect.prepareStatement(checkSql);
            checkStmt.setString(1, userEmail);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");

                // Ila kan l'mot de passe s7i7 -> Nbeddluh
                if (dbPassword.equals(currentPass)) {

                    // C. Update l'mot de passe jdid
                    String updateSql = "UPDATE users SET password = ? WHERE email = ?";
                    PreparedStatement updateStmt = connect.prepareStatement(updateSql);
                    updateStmt.setString(1, newPass);
                    updateStmt.setString(2, userEmail);

                    int result = updateStmt.executeUpdate();

                    if (result > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Mot de passe modifié avec succès !");
                        // Nkhwiw les champs
                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification.");
                    }

                } else {
                    // Ila kan mot de passe l9dim ghalet
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le mot de passe actuel est incorrect.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Base de Données", "Erreur: " + e.getMessage());
        }
    }

    // Helper Method
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- NAVIGATION ---
    @FXML public void goToDashboard(ActionEvent event) throws IOException { navigate(event, "/com/smarttask/smarttaskmanager/view/dashboard.fxml"); }
    @FXML public void goToTasks(ActionEvent event) throws IOException { navigate(event, "/com/smarttask/smarttaskmanager/view/tasks.fxml"); }
    @FXML public void handleLogout(ActionEvent event) throws IOException { navigate(event, "/com/smarttask/smarttaskmanager/view/login.fxml"); }

    private void navigate(ActionEvent event, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}