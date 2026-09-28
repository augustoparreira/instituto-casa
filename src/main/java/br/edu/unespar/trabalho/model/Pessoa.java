package br.edu.unespar.trabalho.model;
import java.time.LocalDate;

public class Pessoa {
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
}