package br.edu.unespar.trabalho.model;

public enum TipoMedida {
    LA("LA", "Liberdade Assistida"),
    PSC("PSC", "Prestação de Serviços à Comunidade");

    private final String codigo;
    private final String descricao;

    TipoMedida(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }

    public static TipoMedida fromCodigo(String c) {
        if (c == null) return null;
        for (TipoMedida t : values()) {
            if (t.codigo.equalsIgnoreCase(c.trim())) return t;
        }
        throw new IllegalArgumentException("Tipo de medida inválido: " + c);
    }

    @Override
    public String toString() { return descricao; }
}