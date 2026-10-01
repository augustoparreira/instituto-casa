package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.RelatorioItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class RelatorioDAO {

    private static RelatorioDAO instance;
    private final ObservableList<RelatorioItem> listaRelatorios;

    private RelatorioDAO() {
        listaRelatorios = FXCollections.observableArrayList(
                new RelatorioItem("Relatório Trimestral - Lucas Oliveira", "Lucas Henrique / PSC", "O adolescente cumpre regularmente as horas estipuladas com ótimo aproveitamento.", "20/09/2026"),
                new RelatorioItem("Acompanhamento Familiar - Mariana Silva", "Mariana dos Santos / LA", "Visita domiciliar efetuada. Família receptiva ao acompanhamento psicológico.", "15/09/2026"),
                new RelatorioItem("Balanço Semestral C.A.S.A.", "Geral / Institucional", "Resumo das atividades desenvolvidas pela equipe técnica durante o semestre.", "01/09/2026")
        );
    }

    public static synchronized RelatorioDAO getInstance() {
        if (instance == null) {
            instance = new RelatorioDAO();
        }
        return instance;
    }

    public ObservableList<RelatorioItem> getListaRelatorios() {
        return listaRelatorios;
    }

    public void adicionar(RelatorioItem item) {
        listaRelatorios.add(0, item);
    }

    public void remover(RelatorioItem item) {
        listaRelatorios.remove(item);
    }

    public void atualizar(int index, RelatorioItem item) {
        if (index >= 0 && index < listaRelatorios.size()) {
            listaRelatorios.set(index, item);
        }
    }
}