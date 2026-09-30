package br.edu.unespar.trabalho.util;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavegacaoUtil {

    public static void mudarTela(Event event, String fxmlPath, String titulo) {
        try {
            Parent root = FXMLLoader.load(NavegacaoUtil.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            trocarRaiz(stage, root, "Instituto C.A.S.A. - " + titulo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Troca o conteúdo da janela SEM criar uma nova Scene (isso evita o encolhimento)
    public static void trocarRaiz(Stage stage, Parent root, String tituloCompleto) {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            scene.setRoot(root);
        }
        stage.setTitle(tituloCompleto);
    }
}