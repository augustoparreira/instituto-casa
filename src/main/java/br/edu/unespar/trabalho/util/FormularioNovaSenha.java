package br.edu.unespar.trabalho.util;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Campos e requisitos compartilhados por criação, alteração e redefinição de senha. */
public final class FormularioNovaSenha extends VBox {
    private final CampoSenha senha = new CampoSenha("nova senha", Senhas.MAXIMO_NOVA);
    private final CampoSenha confirmacao = new CampoSenha("confirmação da senha", Senhas.MAXIMO_NOVA);
    private final BooleanBinding pronta = new BooleanBinding() {
        {
            bind(senha.textoProperty(), confirmacao.textoProperty(), senha.erroProperty(), confirmacao.erroProperty());
        }

        @Override protected boolean computeValue() {
            return Senhas.novaValida(senha.getTexto()) && senha.getTexto().equals(confirmacao.getTexto())
                    && senha.erroProperty().get().isEmpty() && confirmacao.erroProperty().get().isEmpty();
        }
    };

    public FormularioNovaSenha() {
        setSpacing(6);
        Label tamanhoMinimo = new Label(), tamanhoMaximo = new Label(), coincidencia = new Label();
        Label aviso = new Label();
        aviso.setWrapText(true);
        aviso.visibleProperty().bind(aviso.textProperty().isNotEmpty());
        aviso.managedProperty().bind(aviso.visibleProperty());
        Label dica = new Label("Prefira uma frase-senha longa e fácil de lembrar. Maiúsculas, números e símbolos são opcionais.");
        dica.setWrapText(true);
        dica.setMaxWidth(390);
        VBox requisitos = new VBox(2, tamanhoMinimo, tamanhoMaximo, coincidencia, aviso, dica);
        getChildren().addAll(new Label("Nova senha *"), senha, new Label("Confirme a senha *"), confirmacao, requisitos);

        Runnable atualizar = () -> {
            String texto = senha.getTexto(), repetida = confirmacao.getTexto();
            int tamanho = Senhas.tamanho(texto);
            tamanhoMinimo.setText((texto.isEmpty() ? "Pendente" : tamanho >= Senhas.MINIMO_NOVA ? "OK" : "Inválido")
                    + " · Mínimo de " + Senhas.MINIMO_NOVA + " caracteres");
            tamanhoMaximo.setText((texto.isEmpty() ? "Pendente" : tamanho <= Senhas.MAXIMO_NOVA ? "OK" : "Inválido")
                    + " · Máximo de " + Senhas.MAXIMO_NOVA + " caracteres");
            coincidencia.setText(repetida.isEmpty() ? "Pendente · Confirme a senha"
                    : texto.equals(repetida) ? "OK · As senhas coincidem" : "Inválido · As senhas não coincidem");
            String falha = senha.erroProperty().get().isEmpty() ? confirmacao.erroProperty().get() : senha.erroProperty().get();
            aviso.setText(!falha.isEmpty() ? falha + " A edição foi recusada."
                    : !texto.isEmpty() && texto.isBlank() ? "Inválido · Informe uma senha além de espaços em branco." : "");
        };
        senha.textoProperty().addListener(obs -> atualizar.run());
        confirmacao.textoProperty().addListener(obs -> atualizar.run());
        senha.erroProperty().addListener(obs -> atualizar.run());
        confirmacao.erroProperty().addListener(obs -> atualizar.run());
        atualizar.run();
    }

    public BooleanBinding prontaProperty() { return pronta; }

    public String senhaValidada() {
        String texto = senha.getTexto();
        Senhas.validarNova(texto);
        if (!texto.equals(confirmacao.getTexto())) throw new IllegalArgumentException("As senhas não coincidem.");
        if (!pronta.get()) throw new IllegalArgumentException("Corrija a entrada recusada antes de salvar.");
        return texto;
    }

    public void limpar() {
        senha.limpar();
        confirmacao.limpar();
    }
}
