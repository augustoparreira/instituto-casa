package br.edu.unespar.trabalho.util;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/** Apresenta o prefixo da lista cuja altura cabe integralmente no card. */
public final class ListaAdolescentesPainel extends Pane {
    public static final int LIMITE = 5;
    private static final double ESPACAMENTO = 10;

    @Override protected double computeMinHeight(double largura) {
        return 0;
    }

    @Override protected double computePrefHeight(double largura) {
        double altura = 0;
        for (Node linha : getChildren()) {
            if (altura > 0) altura += ESPACAMENTO;
            altura += Math.ceil(linha.prefHeight(largura));
        }
        return altura;
    }

    @Override protected void layoutChildren() {
        double topo = 0;
        boolean cabe = true;
        for (Node linha : getChildren()) {
            double altura = Math.ceil(linha.prefHeight(getWidth()));
            cabe = cabe && topo + altura <= getHeight();
            linha.setVisible(cabe);
            if (cabe) {
                linha.resizeRelocate(0, topo, getWidth(), altura);
                topo += altura + ESPACAMENTO;
            }
        }
    }
}
