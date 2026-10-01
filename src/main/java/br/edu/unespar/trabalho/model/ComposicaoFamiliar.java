package br.edu.unespar.trabalho.model;

public class ComposicaoFamiliar {
    private int idComposicaoFamiliar;
    private String nome;
    private String parentesco;
    private Integer idade;
    private Double renda;
    private String escolaridade;
    private String profissao;
    private long cpfAdolescente;

    public int getIdComposicaoFamiliar() { return idComposicaoFamiliar; }
    public void setIdComposicaoFamiliar(int idComposicaoFamiliar) { this.idComposicaoFamiliar = idComposicaoFamiliar; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }

    public Integer getIdade() { return idade; }
    public void setIdade(Integer idade) { this.idade = idade; }

    public Double getRenda() { return renda; }
    public void setRenda(Double renda) { this.renda = renda; }

    public String getEscolaridade() { return escolaridade; }
    public void setEscolaridade(String escolaridade) { this.escolaridade = escolaridade; }

    public String getProfissao() { return profissao; }
    public void setProfissao(String profissao) { this.profissao = profissao; }

    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
}