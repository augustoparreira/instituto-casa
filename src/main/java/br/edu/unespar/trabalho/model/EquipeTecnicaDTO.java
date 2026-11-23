package br.edu.unespar.trabalho.model;

public class EquipeTecnicaDTO {
    private final Long cpfEquipe;
    private final String login;
    private final String nomeCompleto;
    private final String cargoFuncao;
    private final NivelAcesso perfil;

    public EquipeTecnicaDTO(Long cpfEquipe, String login, String cargoFuncao, String nivelAcesso) {
        this(cpfEquipe, login, login, cargoFuncao, converterPerfil(nivelAcesso));
    }

    public EquipeTecnicaDTO(Long cpfEquipe, String login, String nomeCompleto,
                           String cargoFuncao, NivelAcesso perfil) {
        this.cpfEquipe = cpfEquipe;
        this.login = login;
        this.nomeCompleto = nomeCompleto;
        this.cargoFuncao = cargoFuncao;
        this.perfil = perfil;
    }

    private static NivelAcesso converterPerfil(String valor) {
        for (NivelAcesso nivel : NivelAcesso.values()) {
            if (nivel.getCodigo().equalsIgnoreCase(valor) || nivel.getDescricao().equalsIgnoreCase(valor)) return nivel;
        }
        return null;
    }

    public Long getCpfEquipe() { return cpfEquipe; }
    public String getLogin() { return login; }
    public String getCargoFuncao() { return cargoFuncao; }
    public String getNivelAcesso() { return perfil == null ? "Desconhecido" : perfil.getDescricao(); }
    public String getNomeCompleto() { return nomeCompleto; }
    public NivelAcesso getPerfil() { return perfil; }
}
