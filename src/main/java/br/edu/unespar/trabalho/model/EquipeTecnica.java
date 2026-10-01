package br.edu.unespar.trabalho.model;

public class EquipeTecnica extends Pessoa {
    private String login;
    private String senha;
    private String cargoFuncao;
    private String nivelAcesso;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getCargoFuncao() { return cargoFuncao; }
    public void setCargoFuncao(String cargoFuncao) { this.cargoFuncao = cargoFuncao; }
    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }
}