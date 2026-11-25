package br.edu.unespar.trabalho;

import java.util.List;
import java.time.YearMonth;
import javafx.scene.layout.HBox;
import br.edu.unespar.trabalho.model.RelatorioAcompanhamento;
import br.edu.unespar.trabalho.util.ModeloRelatorio;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import br.edu.unespar.trabalho.util.ImpressaoRelatorio;

final class VerificacaoImpressao {
    static void executar() {
        verificarModeloInstitucional();
        String original=("Acompanhamento socioeducativo, frequência e contato com a família. Acentuação: ação, saúde.\n").repeat(300)
                +"SemEspaços".repeat(300);
        List<VBox> paginas=ImpressaoRelatorio.paginar(List.of(original),523,720);
        assert paginas.size()>1 : "Texto longo precisa de múltiplas páginas.";
        StringBuilder reconstruido=new StringBuilder();
        for(VBox pagina:paginas) {
            new Scene(pagina,523,740);
            pagina.applyCss(); pagina.layout();
            double altura=0;
            for(var node:pagina.getChildren()) {
                Text texto=(Text)node;
                reconstruido.append(texto.getText());
                assert texto.getLayoutBounds().getWidth()<=524 : "Texto excede largura.";
                altura+=texto.getLayoutBounds().getHeight()+6;
            }
            assert altura<=726 : "Texto excede a altura da página.";
        }
        var primeira=paginas.getFirst();
        var imagem=primeira.snapshot(null,null);
        var png=new java.awt.image.BufferedImage((int)imagem.getWidth(),(int)imagem.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,imagem.getPixelReader().getArgb(x,y));
        try {
            javax.imageio.ImageIO.write(png,"png",java.nio.file.Path.of("target/verificacao/ImpressaoRelatorio.png").toFile());
        } catch(java.io.IOException e) { throw new IllegalStateException(e); }
        assert original.contentEquals(reconstruido) : "Paginação precisa preservar cada caractere.";
        assert ImpressaoRelatorio.paginar(List.of("Relatório curto"),523,720).size()==1;
        List<VBox> folhas=ImpressaoRelatorio.prepararPaginas(List.of(original),523,748);
        assert folhas.size()==paginas.size();
        for(int i=0;i<folhas.size();i++) {
            Text rodape=(Text)folhas.get(i).getChildren().getLast();
            assert rodape.getText().equals("Página "+(i+1)+" de "+folhas.size());
            VBox conteudo=(VBox)folhas.get(i).getChildren().getFirst();
            String esperado=paginas.get(i).getChildren().stream().map(n -> ((Text)n).getText()).collect(java.util.stream.Collectors.joining());
            String atual=conteudo.getChildren().stream().map(n -> ((Text)n).getText()).collect(java.util.stream.Collectors.joining());
            assert esperado.equals(atual) : "Prévia e impressão devem usar os mesmos trechos.";
        }
        System.out.println("OK: paginação de texto longo, palavra extensa, acentos e preservação integral em "+paginas.size()+" páginas.");
    }
    private static void verificarModeloInstitucional() {
        double largura = javafx.print.Paper.A4.getWidth()-72;
        double altura = javafx.print.Paper.A4.getHeight()-72;
        var relatorio = new RelatorioAcompanhamento();
        relatorio.setCompetencia(YearMonth.of(2025,1));
        relatorio.setCampo(RelatorioAcompanhamento.Campo.NOME,"Adolescente de teste");
        relatorio.setRegistroFrequencia("Compareceu à oficina.");
        relatorio.setDescumprimento("Sem descumprimento.");
        assert ModeloRelatorio.prepararPaginas(relatorio,largura,altura).size()==1 : "O modelo curto deve caber em uma página.";

        relatorio.setCampo(RelatorioAcompanhamento.Campo.NOME, "Nome fictício ".repeat(80));
        relatorio.setCampo(RelatorioAcompanhamento.Campo.FILIACAO, "Filiação com acentos e detalhes ".repeat(330));
        relatorio.setRegistroFrequencia(("Presença em atividade.\n").repeat(280) + "PalavraSemEspaços".repeat(100));
        relatorio.setDescumprimento("Observação para conferência.\n".repeat(180) + "Fim da observação.");
        StringBuilder identificacao = new StringBuilder();
        for (var campo : RelatorioAcompanhamento.Campo.values())
            identificacao.append(campo.getRotulo()).append(": ").append(relatorio.getCampo(campo)).append('\n');
        List<VBox> paginas = ModeloRelatorio.prepararPaginas(relatorio,largura,altura);
        assert paginas.size()>3;
        StringBuilder dados = new StringBuilder(), frequencia = new StringBuilder(), observacoes = new StringBuilder();
        for (int i=0; i<paginas.size(); i++) {
            VBox pagina = paginas.get(i);
            new Scene(pagina,largura,altura);
            pagina.applyCss(); pagina.layout();
            double limite = 0;
            for (var bloco : pagina.getChildren()) {
                var bounds = bloco.getBoundsInParent();
                assert bounds.getMinY()+1>=limite : "Blocos do relatório se sobrepõem.";
                assert bounds.getMaxY()<=altura+1 && bounds.getMaxX()<=largura+1
                        : "Bloco fora da área útil na página "+(i+1)+" ("+largura+" × "+altura+"): "+bounds;
                limite = bounds.getMaxY();
                if (!(bloco instanceof VBox secao) || secao.getChildren().size()!=2
                        || !(secao.getChildren().getFirst() instanceof Text titulo)) continue;
                var conteudo = secao.getChildren().getLast();
                Text texto = conteudo instanceof VBox caixa ? (Text)caixa.getChildren().getFirst() : (Text)conteudo;
                assert texto.getBoundsInParent().getMaxY() <= conteudo.getLayoutBounds().getHeight()+1
                        || conteudo==texto : "Texto não cabe na caixa.";
                if (titulo.getText().startsWith("Identificação")) dados.append(texto.getText());
                if (titulo.getText().startsWith("1. ")) frequencia.append(texto.getText());
                if (titulo.getText().startsWith("2. ")) observacoes.append(texto.getText());
            }
            HBox rodape = (HBox)pagina.getChildren().getLast();
            assert ((Text)rodape.getChildren().getLast()).getText().equals("Página "+(i+1)+" de "+paginas.size());
            assert ((Text)rodape.getChildren().getFirst()).getText().startsWith("Apucarana")== (i==paginas.size()-1);
        }
        assert identificacao.toString().contentEquals(dados) : "A identificação perdeu texto.";
        assert relatorio.getRegistroFrequencia().contentEquals(frequencia) : "O registro perdeu texto.";
        assert relatorio.getDescumprimento().contentEquals(observacoes) : "A seção de observações perdeu texto.";
        System.out.println("OK: modelo institucional curto e relatório extenso em "+paginas.size()+" páginas, com identificação, texto integral e rodapés.");
    }

}
