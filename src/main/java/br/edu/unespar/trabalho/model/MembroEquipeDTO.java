package br.edu.unespar.trabalho.model;

public class MembroEquipeDTO {
    private String nome;
    private String cargo;
    private String login;
    private String adolescentesAtendidos;
    private String nivelAcesso;
    private String iniciais;

    public MembroEquipeDTO(String nome, String cargo, String login, String adolescentesAtendidos, String nivelAcesso, String iniciais) {
        this.nome = nome;
        this.cargo = cargo;
        this.login = login;
        this.adolescentesAtendidos = adolescentesAtendidos;
        this.nivelAcesso = nivelAcesso;
        this.iniciais = iniciais;
    }

    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public String getLogin() { return login; }
    public String getAdolescentesAtendidos() { return adolescentesAtendidos; }
    public String getNivelAcesso() { return nivelAcesso; }
    public String getIniciais() { return iniciais; }
}