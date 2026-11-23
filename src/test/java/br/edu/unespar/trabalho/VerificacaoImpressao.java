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
        assert original.contentEquals(reconstruido) : "Paginação precisa preservar cada caractere.";
        assert ImpressaoRelatorio.paginar(List.of("Relatório curto"),523,720).size()==1;
        System.out.println("OK: paginação de texto longo, palavra extensa, acentos e preservação integral em "+paginas.size()+" páginas.");
    }
}
