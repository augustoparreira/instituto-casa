package br.edu.unespar.trabalho.model;

public class EducacaoTrabalho {
    private int idEducacaoTrabalho;
    private boolean estuda;
    private String escola;
    private String serie;
    private boolean trabalha;
    private String funcao;
    private String vinculoEmpregaticio;
    private long cpfAdolescente;

    public int getIdEducacaoTrabalho() { return idEducacaoTrabalho; }
    public void setIdEducacaoTrabalho(int idEducacaoTrabalho) { this.idEducacaoTrabalho = idEducacaoTrabalho; }
    public boolean isEstuda() { return estuda; }
    public void setEstuda(boolean estuda) { this.estuda = estuda; }
    public String getEscola() { return escola; }
    public void setEscola(String escola) { this.escola = escola; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public boolean isTrabalha() { return trabalha; }
    public void setTrabalha(boolean trabalha) { this.trabalha = trabalha; }
    public String getFuncao() { return funcao; }
    public void setFuncao(String funcao) { this.funcao = funcao; }
    public String getVinculoEmpregaticio() { return vinculoEmpregaticio; }
    public void setVinculoEmpregaticio(String vinculoEmpregaticio) { this.vinculoEmpregaticio = vinculoEmpregaticio; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long cpfAdolescente) { this.cpfAdolescente = cpfAdolescente; }
}