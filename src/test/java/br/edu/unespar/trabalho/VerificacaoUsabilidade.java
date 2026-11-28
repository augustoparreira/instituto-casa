package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.controller.DashboardController;
import br.edu.unespar.trabalho.controller.EquipeTecnicaController;
import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.model.EquipeTecnica;
import br.edu.unespar.trabalho.util.CampoSenha;
import br.edu.unespar.trabalho.util.FormularioNovaSenha;
import br.edu.unespar.trabalho.util.ListaAdolescentesPainel;
import br.edu.unespar.trabalho.util.NavegacaoUtil;
import br.edu.unespar.trabalho.util.Senhas;
import br.edu.unespar.trabalho.util.Sessao;
import br.edu.unespar.trabalho.util.SegurancaInterface;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

final class VerificacaoUsabilidade {
    private static int testes;

    static void executar() throws Exception {
        VerificacaoContas.entrar("teste.entrega", VerificacaoContas.SENHA);
        verificarPainel();
        verificarSenhas();
        verificarDialogosSenha();
        System.out.println("OK: " + testes + " verificações dirigidas de usabilidade.");
    }

    private static void verificarPainel() throws Exception {
        FXMLLoader loader = new FXMLLoader(VerificacaoUsabilidade.class.getResource("/View/Dashboard.fxml"));
        Parent raiz = loader.load();
        var lista = (ListaAdolescentesPainel) raiz.lookup("#vboxAdolescentes");
        var preencher = DashboardController.class.getDeclaredMethod("carregarEstatisticasEAdolescentes", List.class);
        preencher.setAccessible(true);
        Stage janela = new Stage();
        janela.setScene(new Scene(raiz, 1280, 880));
        janela.show();
        try {
            for (int total : new int[]{0, 1, 3, 5, 6, 12}) {
                var jovens = IntStream.range(0, total).mapToObj(i -> new AdolescenteDTO(
                        "Adolescente fictício " + (i + 1), Integer.toString(i + 1), "PSC", .25,
                        "10/40h", "Técnico fictício", "Ativo", "", "", "")).toList();
                preencher.invoke(loader.getController(), jovens);
                verificarLayout(raiz, lista, jovens, 1440, 960, Math.min(total, 5));
                verificar(((Label) raiz.lookup("#lblTotalCadastrados")).getText().equals(Integer.toString(total))
                                && ((Label) raiz.lookup("#lblTotalPsc")).getText().equals(Integer.toString(total)),
                        "Os indicadores precisam continuar contando a lista inteira.");
                verificarLayout(raiz, lista, jovens, 1024, 600, -1);
            }
            verificarLayout(raiz, lista, lista.getChildrenUnmodifiable().stream()
                    .map(no -> (AdolescenteDTO) no.getUserData()).toList(), 1150, 650, -1);
            verificar(lista.getChildrenUnmodifiable().stream().anyMatch(n -> !n.isVisible()),
                    "A resolução menor deve reduzir a quantidade exibida.");
            capturar(raiz, "painel-compacto.png");
            janela.setMaximized(true);
            VerificacaoPreparacaoEntrega.aguardarInterface(janela::isMaximized);
            raiz.applyCss();
            raiz.layout();
            verificar(lista.getChildrenUnmodifiable().stream().filter(Node::isVisible).count() <= 5,
                    "A janela maximizada também respeita o limite.");
            janela.setMaximized(false);

            var reais = new AdolescenteDAO().listarResumoDTO();
            preencher.invoke(loader.getController(), reais);
            raiz.resize(1280, 880);
            raiz.applyCss();
            raiz.layout();
            Node verTodos = raiz.lookupAll(".link-text").stream()
                    .filter(n -> n instanceof Label l && l.getText().startsWith("Ver todos")).findFirst().orElseThrow();
            clicar(verTodos);
            verificar(janela.getScene().getRoot().lookup("#listAdolescentes") instanceof javafx.scene.control.ListView<?> completa
                    && completa.getItems().size() == reais.size(), "Ver todos abre a lista completa.");
            NavegacaoUtil.trocarRaiz(janela, raiz, "Painel");
            Node linha = lista.getChildrenUnmodifiable().getFirst();
            clicar(linha);
            verificar(janela.getScene().getRoot().lookup("#lblNome") != null, "A linha abre o perfil individual.");
        } finally { janela.close(); }
    }

    private static void clicar(Node no) {
        no.fireEvent(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                1, 1, 1, 1, javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false,
                false, false, false, false, false, true, null));
    }

    private static void verificarLayout(Parent raiz, ListaAdolescentesPainel lista, List<AdolescenteDTO> jovens,
                                        int largura, int altura, int totalEsperado) {
        raiz.resize(largura, altura);
        raiz.applyCss();
        raiz.layout();
        lista.layout();
        var visiveis = lista.getChildrenUnmodifiable().stream().filter(Node::isVisible).toList();
        if (totalEsperado >= 0) verificar(visiveis.size() == totalEsperado, "Quantidade incorreta na resolução ampla.");
        verificar(visiveis.size() <= Math.min(jovens.size(), 5), "O painel ultrapassou o limite.");
        verificar(visiveis.stream().map(Node::getUserData).toList().equals(jovens.subList(0, visiveis.size())),
                "A ordem original precisa ser mantida.");
        double fundo = lista.localToScene(lista.getLayoutBounds()).getMaxY();
        for (Node no : visiveis) {
            double finalLinha = no.localToScene(no.getLayoutBounds()).getMaxY();
            verificar(finalLinha <= fundo + .5 && finalLinha <= altura + .5, "Uma linha ficou cortada.");
            verificar(no.lookup(".progress-bar") instanceof ProgressBar barra
                    && barra.getProgress() == ((AdolescenteDTO) no.getUserData()).getProgresso(), "Progresso foi perdido.");
        }
    }

    private static void verificarSenhas() throws Exception {
        var grupo = new FormularioNovaSenha();
        var senha = (CampoSenha) grupo.getChildren().get(1);
        var confirmacao = (CampoSenha) grupo.getChildren().get(3);
        Stage janela = new Stage();
        janela.setScene(new Scene(new VBox(grupo), 430, 290));
        janela.show();
        try {
            verificar(!grupo.prontaProperty().get(), "Campos vazios devem ficar pendentes.");
            for (String valor : List.of("a".repeat(11), "a".repeat(12), "a".repeat(13),
                    "a".repeat(128), "MaiusculaMinuscula", "1".repeat(12), "@".repeat(12),
                    "  frase lembrável  ", "á".repeat(11) + "🔐", " ".repeat(12))) {
                editor(senha).setText(valor);
                editor(confirmacao).setText(valor);
                verificar(grupo.prontaProperty().get() == Senhas.novaValida(valor),
                        "O feedback e a regra de persistência precisam concordar.");
            }
            String ficticia = "  frase fictícia de teste 🔐  ";
            editor(senha).setText(ficticia);
            editor(confirmacao).setText(ficticia + "x");
            verificar(!grupo.prontaProperty().get(), "Confirmação diferente precisa impedir o salvamento.");
            editor(confirmacao).selectAll();
            editor(confirmacao).replaceSelection(ficticia);
            verificar(grupo.prontaProperty().get() && grupo.senhaValidada().equals(ficticia),
                    "A substituição deve preservar acentos e espaços.");
            var oculto = editor(senha);
            oculto.selectRange(3, 8);
            alternar(senha).fire();
            var visivel = editor(senha);
            verificar(!(visivel instanceof PasswordField) && visivel.getText().equals(ficticia)
                            && visivel.getAnchor() == 3 && visivel.getCaretPosition() == 8 && oculto.getText().isEmpty(),
                    "Mostrar deve preservar conteúdo e seleção sem manter o editor inativo preenchido.");
            alternar(senha).fire();
            verificar(editor(senha) == oculto && oculto.getText().equals(ficticia) && visivel.getText().isEmpty()
                            && oculto.getAnchor() == 3 && oculto.getCaretPosition() == 8,
                    "Ocultar deve devolver a seleção ao campo protegido.");
            oculto.selectAll();
            oculto.replaceSelection("x".repeat(129));
            verificar(oculto.getText().equals(ficticia) && !grupo.prontaProperty().get(),
                    "A entrada de 129 unidades deve ser recusada inteira e sinalizada.");
            oculto.replaceSelection("linha\ncom quebra");
            verificar(oculto.getText().equals(ficticia), "Colar quebras invisíveis não pode mudar a senha.");
            oculto.replaceSelection(ficticia + " ");
            editor(confirmacao).setText(ficticia + " ");
            verificar(grupo.prontaProperty().get(), "Uma entrada válida posterior deve remover o aviso.");
            capturar(janela.getScene().getRoot(), "requisitos-senha.png");
            alternar(senha).fire();
            janela.hide();
            janela.show();
            verificar(editor(senha) instanceof PasswordField && senha.getTexto().isEmpty()
                    && confirmacao.getTexto().isEmpty() && !grupo.prontaProperty().get(), "Reabrir deve ocultar e limpar a senha.");
            rejeitar(() -> new EquipeTecnicaDAO().redefinirSenha(Sessao.getUsuarioLogado().getCpfEquipe(), "x".repeat(129)));
            VerificacaoContas.entrar("teste.entrega", VerificacaoContas.SENHA);
            verificar(Sessao.getUsuarioLogado() != null, "Conta existente continua autenticando com sua senha.");
            String hash = Senhas.gerarHash(ficticia);
            verificar(Senhas.verificar(ficticia, hash) && !Senhas.verificar(ficticia.strip(), hash),
                    "O hash existente precisa preservar os espaços e o conteúdo Unicode.");
            rejeitar(() -> Senhas.validarNova("x".repeat(11)));
            rejeitar(() -> Senhas.validarNova("x".repeat(129)));
        } finally { janela.close(); }
    }

    private static TextField editor(CampoSenha senha) {
        return senha.lookupAll(".text-field").stream()
                .filter(Node::isVisible).map(TextField.class::cast).findFirst().orElseThrow();
    }

    private static void verificarDialogosSenha() throws Exception {
        FXMLLoader loader = new FXMLLoader(VerificacaoUsabilidade.class.getResource("/View/EquipeTecnicaView.fxml"));
        Stage janela = new Stage();
        janela.setScene(new Scene(loader.load(), 1100, 650));
        janela.show();
        try {
            var criar = EquipeTecnicaController.class.getDeclaredMethod("abrirFormulario", EquipeTecnica.class);
            var redefinir = EquipeTecnicaController.class.getDeclaredMethod("redefinirSenha", EquipeTecnica.class);
            var alterar = SegurancaInterface.class.getDeclaredMethod("trocarMinhaSenha", Stage.class);
            criar.setAccessible(true);
            redefinir.setAccessible(true);
            alterar.setAccessible(true);
            EquipeTecnica profissional = new EquipeTecnicaDAO().buscarPorCpf(Sessao.getUsuarioLogado().getCpfEquipe());
            verificarDialogo(() -> criar.invoke(loader.getController(), (Object) null), false);
            verificarDialogo(() -> criar.invoke(loader.getController(), (Object) null), false);
            verificarDialogo(() -> redefinir.invoke(loader.getController(), profissional), false);
            verificarDialogo(() -> alterar.invoke(null, janela), true);
        } finally { janela.close(); }
    }

    private static void verificarDialogo(AbrirDialogo abrir, boolean exigeAtual) throws Exception {
        var falha = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var camposRetidos = new java.util.concurrent.atomic.AtomicReference<List<CampoSenha>>();
        Platform.runLater(() -> {
            DialogPane painel = null;
            try {
                painel = Window.getWindows().stream().filter(Window::isShowing).map(Window::getScene)
                        .map(Scene::getRoot).filter(DialogPane.class::isInstance).map(DialogPane.class::cast)
                        .findFirst().orElseThrow();
                painel.applyCss();
                painel.layout();
                var campos = painel.lookupAll(".senha-alternar").stream().map(Node::getParent)
                        .map(CampoSenha.class::cast).toList();
                camposRetidos.set(campos);
                List<CampoSenha> novos = campos.stream().filter(campo ->
                        !editor(campo).getAccessibleText().equals("senha atual")).toList();
                Button confirmar = (Button) painel.lookupButton(painel.getButtonTypes().getFirst());
                verificar(campos.size() == (exigeAtual ? 3 : 2) && confirmar.isDisabled()
                        && campos.stream().allMatch(campo -> editor(campo) instanceof PasswordField && campo.getTexto().isEmpty()),
                        "O formulário deve abrir vazio, oculto e com salvamento indisponível.");
                for (CampoSenha campo : novos) editor(campo).setText("frase de teste sem composição");
                if (exigeAtual) {
                    verificar(confirmar.isDisabled(), "Alterar senha exige preencher o campo atual.");
                    CampoSenha atual = campos.stream().filter(campo -> !novos.contains(campo)).findFirst().orElseThrow();
                    editor(atual).setText("legada");
                }
                verificar(!confirmar.isDisabled(), "Campos de senha válidos devem liberar a ação.");
                alternar(novos.getFirst()).fire();
            } catch (Throwable erro) { falha.set(erro); }
            finally {
                if (painel != null) ((Button) painel.lookupButton(ButtonType.CANCEL)).fire();
            }
        });
        abrir.executar();
        if (falha.get() != null) throw new AssertionError("Falha no diálogo real de senha.", falha.get());
        verificar(camposRetidos.get() != null && camposRetidos.get().stream()
                        .allMatch(campo -> campo.getTexto().isEmpty() && editor(campo) instanceof PasswordField),
                "Cancelar o formulário precisa remover e ocultar todos os valores.");
    }

    @FunctionalInterface
    private interface AbrirDialogo {
        void executar() throws Exception;
    }

    private static Button alternar(CampoSenha senha) {
        return (Button) senha.lookup(".senha-alternar");
    }

    private static void rejeitar(Runnable operacao) {
        try { operacao.run(); }
        catch (IllegalArgumentException esperado) { testes++; return; }
        throw new AssertionError("A validação final deveria recusar a operação.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        testes++;
    }

    private static void capturar(Parent raiz, String nome) throws Exception {
        String destino = System.getProperty("casa.teste.imagens");
        if (destino == null) return;
        Path pasta = Files.createDirectories(Path.of(destino));
        var imagem = raiz.snapshot(null, null);
        BufferedImage png = new BufferedImage((int) imagem.getWidth(), (int) imagem.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++)
            for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, imagem.getPixelReader().getArgb(x, y));
        ImageIO.write(png, "png", pasta.resolve(nome).toFile());
    }
}
