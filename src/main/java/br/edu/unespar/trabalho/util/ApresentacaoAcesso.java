package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.Operacao;
import br.edu.unespar.trabalho.service.AutorizacaoService;
import javafx.scene.Node;

/** Apresentação de controles, sem conceder permissões. */
public final class ApresentacaoAcesso {
    private ApresentacaoAcesso() {}
    public static void aplicar(Node node, Operacao operacao) {
        boolean permitido=AutorizacaoService.pode(operacao);
        node.setVisible(permitido);
        node.setManaged(permitido);
    }
}
