package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;

public class Sessao {
    private static EquipeTecnicaDTO usuarioLogado;

    public static EquipeTecnicaDTO getUsuarioLogado() {
        return usuarioLogado;
    }

    public static void setUsuarioLogado(EquipeTecnicaDTO usuario) {
        usuarioLogado = usuario;
    }

    public static void limparSessao() {
        usuarioLogado = null;
    }
}