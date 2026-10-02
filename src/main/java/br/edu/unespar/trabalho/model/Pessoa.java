package br.edu.unespar.trabalho.model;

import java.time.LocalDate;
import java.time.Period;

public abstract class Pessoa {
    private long cpf;
    private String nomeCompleto;
    private LocalDate dataNascimento;
    private String contato;
    private String email;

    public long getCpf() { return cpf; }
    public void setCpf(long cpf) { this.cpf = cpf; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** CPF com 11 dígitos (preserva zeros à esquerda): 123.456.789-00 */
    public String getCpfFormatado() {
        String d = String.format("%011d", cpf);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Idade calculada a partir da data de nascimento; -1 se não informada. */
    public int getIdade() {
        if (dataNascimento == null) return -1;
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    /** Alerta de faixa etária: true se a idade passou do limite de atendimento. */
    public boolean isAcimaDaIdadeLimite(int idadeLimite) {
        return getIdade() > idadeLimite;
    }
}