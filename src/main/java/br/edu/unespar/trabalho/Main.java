package br.edu.unespar.trabalho;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/View/Login.fxml"));

            primaryStage.setTitle("Instituto C.A.S.A. - Sistema de Gestão");
            primaryStage.setScene(new Scene(root));

            // Configura a janela para iniciar maximizada em tela cheia
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();