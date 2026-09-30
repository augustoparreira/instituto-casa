package br.edu.unespar.trabalho.model;

public class EquipeTecnicaDTO {
    private Long cpfEquipe;
    private String login;
    private String cargoFuncao;
    private String nivelAcesso;

    public EquipeTecnicaDTO(Long cpfEquipe, String login, String cargoFuncao, String nivelAcesso) {
        this.cpfEquipe = cpfEquipe;
        this.login = login;
        this.cargoFuncao = cargoFuncao;
        this.nivelAcesso = nivelAcesso;
    }

    public Long getCpfEquipe() { return cpfEquipe; }
    public String getLogin() { return login; }
    public String getCargoFuncao() { return cargoFuncao; }
    public String getNivelAcesso() { return nivelAcesso; }
}