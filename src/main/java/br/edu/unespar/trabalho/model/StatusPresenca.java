package br.edu.unespar.trabalho.model;

public enum StatusPresenca {
    // códigos com no máximo 10 caracteres (coluna status_presenca VARCHAR(10))
    PRESENTE("PRESENTE", "Presente"),
    FALTA_JUSTIFICADA("FALTA_JUST", "Falta justificada"),
    FALTA_INJUSTIFICADA("FALTA_INJ", "Falta injustificada");

    private final String codigo;
    private final String descricao;

    StatusPresenca(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }

    public static StatusPresenca fromCodigo(String c) {
        if (c == null) return null;
        for (StatusPresenca s : values()) {
            if (s.codigo.equalsIgnoreCase(c.trim())) return s;
        }
        throw new IllegalArgumentException("Status de presença inválido: " + c);
    }

    @Override
    public String toString() { return descricao; }
}