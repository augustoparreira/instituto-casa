package br.edu.unespar.trabalho.service;

import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.model.EquipeTecnica;
import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;

public class AuthServiceImpl implements AuthService {

    private EquipeTecnicaDAO equipeTecnicaDAO;

    public AuthServiceImpl() {
        this.equipeTecnicaDAO = new EquipeTecnicaDAO();
    }

    @Override
    public EquipeTecnicaDTO autenticar(String login, String senha) throws Exception {
        // Autenticação real acessando o banco via DAO (sem backdoor)
        EquipeTecnica usuario = equipeTecnicaDAO.autenticar(login, senha);

        if (usuario != null) {
            String nivelAcesso = usuario.getNivelAcesso() != null ? usuario.getNivelAcesso().getDescricao() : "Desconhecido";

            return new EquipeTecnicaDTO(
                    usuario.getCpf(),
                    usuario.getNomeCompleto(),
                    usuario.getCargoFuncao(),
                    nivelAcesso
            );
        }

        return null;
    }
}