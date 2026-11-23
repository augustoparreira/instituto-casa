package br.edu.unespar.trabalho;

import java.util.List;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import br.edu.unespar.trabalho.util.ImpressaoRelatorio;

final class VerificacaoImpressao {
    static void executar() {
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
}
