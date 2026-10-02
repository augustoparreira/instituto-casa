package br.edu.unespar.trabalho.model;

public enum NivelAcesso {
    ADMINISTRADOR("ADMINISTRADOR", "Administrador do sistema"),
    EQUIPE_TECNICA("EQUIPE_TECNICA", "Equipe técnica"),
    EDUCADOR("EDUCADOR", "Educador");

    private final String codigo;
    private final String descricao;

    NivelAcesso(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }

    public static NivelAcesso fromCodigo(String c) {
        if (c == null) return null;
        for (NivelAcesso n : values()) {
            if (n.codigo.equalsIgnoreCase(c.trim())) return n;
        }
        throw new IllegalArgumentException("Nível de acesso inválido: " + c);
    }

    @Override
    public String toString() { return descricao; }
}