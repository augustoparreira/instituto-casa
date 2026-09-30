package br.edu.unespar.trabalho.model;

public class AdolescenteDTO {
    private String nome;
    private String cpf;
    private String medida; // "PSC" ou "LA"
    private double progresso; // Ex: 0.40 para 40%
    private String textoProgresso; // Ex: "48/120h"
    private String tecnico;
    private String status; // "Ativo", "Suspenso", "Encerrado"
    private String bairro;
    private String dataNascimento;
    private String genero;

    public AdolescenteDTO(String nome, String cpf, String medida, double progresso, String textoProgresso, String tecnico, String status, String bairro, String dataNascimento, String genero) {
        this.nome = nome;
        this.cpf = cpf;
        this.medida = medida;
        this.progresso = progresso;
        this.textoProgresso = textoProgresso;
        this.tecnico = tecnico;
        this.status = status;
        this.bairro = bairro;
        this.dataNascimento = dataNascimento;
        this.genero = genero;
    }

    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getMedida() { return medida; }
    public double getProgresso() { return progresso; }
    public String getTextoProgresso() { return textoProgresso; }
    public String getTecnico() { return tecnico; }
    public String getStatus() { return status; }
    public String getBairro() { return bairro; }
    public String getDataNascimento() { return dataNascimento; }
    public String getGenero() { return genero; }
}