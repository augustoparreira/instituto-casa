package br.edu.unespar.trabalho.model;

public class EquipeTecnica extends Pessoa {
    private String login;
    private String senha;          // guardar SEMPRE o hash (ex.: BCrypt), nunca a senha em texto
    private String cargoFuncao;
    private NivelAcesso nivelAcesso;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getCargoFuncao() { return cargoFuncao; }
    public void setCargoFuncao(String cargoFuncao) { this.cargoFuncao = cargoFuncao; }
    public NivelAcesso getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(NivelAcesso nivelAcesso) { this.nivelAcesso = nivelAcesso; }
}