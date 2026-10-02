package br.edu.unespar.trabalho.model;

public class EducacaoTrabalho {
    private int idEducacaoTrabalho;
    private boolean estuda;
    private String escola;              // somente se estuda
    private String serie;               // coluna: ano_serie; somente se estuda
    private boolean trabalha;
    private String localTrabalho;       // somente se trabalha
    private String funcao;              // somente se trabalha
    private String vinculoEmpregaticio; // somente se trabalha
    private long cpfAdolescente;

    public int getIdEducacaoTrabalho() { return idEducacaoTrabalho; }
    public void setIdEducacaoTrabalho(int v) { this.idEducacaoTrabalho = v; }
    public boolean isEstuda() { return estuda; }
    public void setEstuda(boolean v) { this.estuda = v; }
    public String getEscola() { return escola; }
    public void setEscola(String v) { this.escola = v; }
    public String getSerie() { return serie; }
    public void setSerie(String v) { this.serie = v; }
    public boolean isTrabalha() { return trabalha; }
    public void setTrabalha(boolean v) { this.trabalha = v; }
    public String getLocalTrabalho() { return localTrabalho; }
    public void setLocalTrabalho(String v) { this.localTrabalho = v; }
    public String getFuncao() { return funcao; }
    public void setFuncao(String v) { this.funcao = v; }
    public String getVinculoEmpregaticio() { return vinculoEmpregaticio; }
    public void setVinculoEmpregaticio(String v) { this.vinculoEmpregaticio = v; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long v) { this.cpfAdolescente = v; }

    public void validar() {
        if (!estuda && (preenchido(escola) || preenchido(serie)))
            throw new IllegalArgumentException("Escola e série só podem ser informadas se o adolescente estuda.");
        if (!trabalha && (preenchido(localTrabalho) || preenchido(funcao) || preenchido(vinculoEmpregaticio)))
            throw new IllegalArgumentException("Local, função e vínculo só podem ser informados se o adolescente trabalha.");
    }

    private static boolean preenchido(String s) { return s != null && !s.isBlank(); }
}