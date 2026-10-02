package br.edu.unespar.trabalho.model;

public enum StatusAdolescente {
    ATIVO("ATIVO", "Ativo"),
    INATIVO("INATIVO", "Inativo"),
    EM_DESCUMPRIMENTO("EM_DESCUMPRIMENTO", "Em descumprimento"),
    EM_ANALISE_EXTINCAO("EM_ANALISE_EXTINCAO", "Em análise para extinção da medida");

    private final String codigo;     // valor gravado no banco
    private final String descricao;  // texto para a tela

    StatusAdolescente(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }

    public static StatusAdolescente fromCodigo(String c) {
        if (c == null) return null;
        for (StatusAdolescente s : values()) {
            if (s.codigo.equalsIgnoreCase(c.trim())) return s;
        }
        throw new IllegalArgumentException("Status de adolescente inválido: " + c);
    }

    @Override
    public String toString() { return descricao; }
}