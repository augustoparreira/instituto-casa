package br.edu.unespar.trabalho.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;

public class RelatorioAcompanhamento {
    public enum Campo {
        NOME("Nome"), NASCIMENTO("Data de nascimento"), FILIACAO("Filiação"), RESPONSAVEL("Responsável"),
        ENDERECO("Endereço"), TELEFONE("Telefone do adolescente"), TELEFONE_RESPONSAVEL("Telefone do responsável"),
        EMAIL("E-mail"), PROCESSO("Número do processo"), HORAS("Horas de cumprimento de medida socioeducativa");
        private final String rotulo;
        Campo(String rotulo) { this.rotulo=rotulo; }
        public String getRotulo() { return rotulo; }
        public String getColuna() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private int idRelatorio;
    private long cpfAdolescente;
    private YearMonth competencia;
    private LocalDate dataEmissao=LocalDate.now();
    private final EnumMap<Campo,String> campos=new EnumMap<>(Campo.class);
    private String registroFrequencia="", descumprimento="";
    public int getIdRelatorio() { return idRelatorio; }
    public void setIdRelatorio(int v) { idRelatorio=v; }
    public long getCpfAdolescente() { return cpfAdolescente; }
    public void setCpfAdolescente(long v) { cpfAdolescente=v; }
    public YearMonth getCompetencia() { return competencia; }
    public void setCompetencia(YearMonth v) { competencia=v; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate v) { dataEmissao=v; }
    public String getCampo(Campo campo) { return campos.getOrDefault(campo,""); }
    public void setCampo(Campo campo,String valor) { campos.put(campo,valor==null?"":valor.trim()); }
    public String getRegistroFrequencia() { return registroFrequencia; }
    public void setRegistroFrequencia(String v) { registroFrequencia=v==null?"":v.trim(); }
    public String getDescumprimento() { return descumprimento; }
    public void setDescumprimento(String v) { descumprimento=v==null?"":v.trim(); }
    public void validar() {
        if(cpfAdolescente<=0 || competencia==null || dataEmissao==null || getCampo(Campo.NOME).isBlank())
            throw new IllegalArgumentException("Prepare o relatório de um adolescente e informe nome, competência e data de emissão.");
    }
    @Override public String toString() { return getCampo(Campo.NOME)+" · "+competencia+" · #"+idRelatorio; }
}
