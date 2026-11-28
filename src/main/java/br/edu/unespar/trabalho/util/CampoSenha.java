package br.edu.unespar.trabalho.util;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/** Campo editável em modo oculto ou visível, com a mesma validação nos dois modos. */
public final class CampoSenha extends HBox {
    private final PasswordField oculto = new PasswordField();
    private final TextField visivel = new TextField() {
        @Override public void copy() { }
        @Override public void cut() { }
    };
    private final Button alternar = new Button("Mostrar");
    private final ReadOnlyStringWrapper texto = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper erro = new ReadOnlyStringWrapper("");
    private TextField ativo = oculto;

    public CampoSenha(String rotulo, int limite) {
        this(rotulo, limite, false);
    }

    CampoSenha(String rotulo) {
        this(rotulo, Senhas.MAXIMO_NOVA, true);
    }

    private CampoSenha(String rotulo, int limite, boolean senhaNova) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getStylesheets().add(CampoSenha.class.getResource("/View/senha-style.css").toExternalForm());
        for (TextField campo : new TextField[]{oculto, visivel}) {
            campo.setAccessibleText(rotulo);
            campo.setMaxWidth(Double.MAX_VALUE);
            campo.setContextMenu(menuEdicao(campo));
            campo.addEventFilter(MouseEvent.DRAG_DETECTED, MouseEvent::consume);
            ValidacaoFormulario.limitarSenha(campo, limite, senhaNova, aviso -> {
                if (campo == ativo) erro.set(aviso);
            });
            campo.textProperty().addListener((obs, antes, depois) -> {
                if (campo == ativo) texto.set(depois);
            });
            campo.visibleProperty().bind(campo.managedProperty());
        }
        visivel.setManaged(false);
        StackPane editores = new StackPane(oculto, visivel);
        HBox.setHgrow(editores, Priority.ALWAYS);
        alternar.getStyleClass().add("senha-alternar");
        alternar.setMnemonicParsing(false);
        alternar.setAccessibleText("Mostrar " + rotulo);
        alternar.setOnAction(e -> {
            trocarEditor(ativo == oculto ? visivel : oculto);
            alternar.setAccessibleText(alternar.getText() + " " + rotulo);
        });
        getChildren().addAll(editores, alternar);

        EventHandler<WindowEvent> limparAoOcultar = e -> limpar();
        ChangeListener<Window> observarJanela = (obs, antes, depois) -> {
            if (antes != null) antes.removeEventHandler(WindowEvent.WINDOW_HIDDEN, limparAoOcultar);
            if (depois != null) depois.addEventHandler(WindowEvent.WINDOW_HIDDEN, limparAoOcultar);
        };
        sceneProperty().addListener((obs, antes, depois) -> {
            if (antes != null) {
                antes.windowProperty().removeListener(observarJanela);
                if (antes.getWindow() != null) antes.getWindow().removeEventHandler(WindowEvent.WINDOW_HIDDEN, limparAoOcultar);
                limpar();
            }
            if (depois != null) {
                depois.windowProperty().addListener(observarJanela);
                if (depois.getWindow() != null) depois.getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN, limparAoOcultar);
            }
        });
    }

    private static ContextMenu menuEdicao(TextField campo) {
        MenuItem desfazer = new MenuItem("Desfazer");
        desfazer.disableProperty().bind(campo.undoableProperty().not());
        desfazer.setOnAction(e -> campo.undo());
        MenuItem refazer = new MenuItem("Refazer");
        refazer.disableProperty().bind(campo.redoableProperty().not());
        refazer.setOnAction(e -> campo.redo());
        MenuItem colar = new MenuItem("Colar");
        colar.setOnAction(e -> campo.paste());
        MenuItem selecionar = new MenuItem("Selecionar tudo");
        selecionar.setOnAction(e -> campo.selectAll());
        return new ContextMenu(desfazer, refazer, colar, selecionar);
    }

    public void estilizarLogin() {
        oculto.setId("txtSenha");
        oculto.getStyleClass().add("input-field");
        visivel.getStyleClass().add("input-field");
    }

    public ReadOnlyStringProperty textoProperty() { return texto.getReadOnlyProperty(); }
    public ReadOnlyStringProperty erroProperty() { return erro.getReadOnlyProperty(); }
    public String getTexto() { return texto.get(); }

    public void ocultar() {
        if (ativo != oculto) trocarEditor(oculto);
        alternar.setAccessibleText("Mostrar " + oculto.getAccessibleText());
    }

    public void limpar() {
        ativo.clear();
        erro.set("");
        ocultar();
    }

    private void trocarEditor(TextField destino) {
        TextField anterior = ativo;
        int ancora = anterior.getAnchor();
        int cursor = anterior.getCaretPosition();
        boolean focar = anterior.isFocused() || alternar.isFocused();
        destino.setText(texto.get());
        ativo = destino;
        anterior.setManaged(false);
        destino.setManaged(true);
        anterior.clear();
        alternar.setText(destino == oculto ? "Mostrar" : "Ocultar");
        if (focar) destino.requestFocus();
        destino.selectRange(ancora, cursor);
    }
}
