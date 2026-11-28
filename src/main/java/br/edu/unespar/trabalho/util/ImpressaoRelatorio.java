package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.RelatorioAcompanhamento;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.print.*;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;

/** Paginação por métricas reais do JavaFX em pontos de impressão. */
public final class ImpressaoRelatorio {
    private ImpressaoRelatorio() {}

    public static List<VBox> paginar(List<String> blocos, double largura, double altura) {
        if(largura<100 || altura<100) throw new IllegalArgumentException("Área de impressão insuficiente.");
        List<VBox> paginas=new ArrayList<>();
        VBox pagina=pagina(largura);
        double usada=0;
        for(String bloco:blocos) {
            String restante=bloco.isEmpty() ? " " : bloco;
            while(!restante.isEmpty()) {
                double disponivel=altura-usada;
                int inicio=0, fim=restante.length();
                while(inicio<fim) {
                    int meio=(inicio+fim+1)/2;
                    if(texto(restante.substring(0,meio),largura).getLayoutBounds().getHeight()<=disponivel) inicio=meio;
                    else fim=meio-1;
                }
                int corte=inicio;
                if(corte==0) {
                    if(pagina.getChildren().isEmpty()) throw new IllegalArgumentException("A fonte não cabe na página.");
                    paginas.add(pagina); pagina=pagina(largura); usada=0; continue;
                }
                // Evita separar pares UTF-16 e prefere uma quebra no espaço.
                if(corte<restante.length() && Character.isHighSurrogate(restante.charAt(corte-1))) corte--;
                if(corte<restante.length()) {
                    int espaco=restante.lastIndexOf(' ',corte-1);
                    if(espaco>corte/2) corte=espaco+1;
                }
                if(corte==0) throw new IllegalArgumentException("Área insuficiente para o texto.");
                Text trecho=texto(restante.substring(0,corte),largura);
                pagina.getChildren().add(trecho);
                usada+=trecho.getLayoutBounds().getHeight()+6;
                restante=restante.substring(corte);
                if(!restante.isEmpty()) { paginas.add(pagina); pagina=pagina(largura); usada=0; }
            }
        }
        if(!pagina.getChildren().isEmpty()) paginas.add(pagina);
        return paginas;
    }

    private static VBox pagina(double largura) {
        VBox v=new VBox(6);
        v.setPrefWidth(largura); v.setMinWidth(largura); v.setMaxWidth(largura);
        v.setStyle("-fx-background-color: white;");
        return v;
    }

    private static Text texto(String valor,double largura) {
        Text t=new Text(valor); t.setFont(Font.font("Arial",11)); t.setWrappingWidth(largura);
        t.setFill(javafx.scene.paint.Color.BLACK);
        return t;
    }

    public static List<VBox> prepararPaginas(List<String> blocos,double largura,double altura) {
        List<VBox> conteudo=paginar(blocos,largura,altura-28);
        List<VBox> folhas=new ArrayList<>();
        for(int i=0;i<conteudo.size();i++) {
            VBox folha=new VBox(10,conteudo.get(i),texto("Página "+(i+1)+" de "+conteudo.size(),largura));
            folha.setPadding(Insets.EMPTY);
            folha.setPrefWidth(largura);
            folhas.add(folha);
        }
        return folhas;
    }

    public static void imprimir(Window owner,List<String> blocos) {
        imprimirPaginas(owner, layout -> prepararPaginas(blocos,layout.getPrintableWidth(),layout.getPrintableHeight()));
    }

    public static void imprimirModelo(Window owner,RelatorioAcompanhamento relatorio) {
        imprimirPaginas(owner, layout -> ModeloRelatorio.prepararPaginas(relatorio,layout.getPrintableWidth(),layout.getPrintableHeight()));
    }

    private static void imprimirPaginas(Window owner,Function<PageLayout,List<VBox>> preparar) {
        var usuario = Sessao.getUsuarioLogado();
        br.edu.unespar.trabalho.service.AutorizacaoService.exigir(br.edu.unespar.trabalho.model.Operacao.RELATORIO_EXPORTAR);
        PrinterJob job=PrinterJob.createPrinterJob();
        if(job==null) {
            new Alert(Alert.AlertType.INFORMATION,"Nenhuma impressora disponível. Para salvar em PDF, configure uma impressora PDF no sistema.").showAndWait();
            return;
        }
        if(!job.showPrintDialog(owner)) { job.cancelJob(); return; }
        try {
            if (usuario != Sessao.getUsuarioLogado())
                throw new br.edu.unespar.trabalho.service.AcessoNegadoException();
            br.edu.unespar.trabalho.service.AutorizacaoService.exigir(
                    br.edu.unespar.trabalho.model.Operacao.RELATORIO_EXPORTAR);
        } catch (br.edu.unespar.trabalho.service.AcessoNegadoException negado) {
            job.cancelJob();
            throw negado;
        }
        PageLayout layout=job.getPrinter().createPageLayout(Paper.A4,PageOrientation.PORTRAIT,36,36,36,36);
        job.getJobSettings().setPageLayout(layout);
        job.getJobSettings().setJobName("Relatório mensal de acompanhamento");
        try {
            List<VBox> paginas=preparar.apply(layout);
            for(int i=0;i<paginas.size();i++) {
                if(!job.printPage(layout,paginas.get(i))) {
                    job.cancelJob();
                    new Alert(Alert.AlertType.ERROR,"A impressão foi interrompida. Confira a fila da impressora.").showAndWait();
                    return;
                }
            }
            if(!job.endJob()) throw new IllegalStateException("A impressora não confirmou o trabalho.");
        } catch(RuntimeException e) {
            job.cancelJob();
            new Alert(Alert.AlertType.ERROR,"Não foi possível imprimir o relatório. "+e.getMessage()).showAndWait();
        }
    }
}
