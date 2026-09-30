package br.edu.unespar.trabalho.service;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;

public class AuthServiceImpl implements AuthService {

    @Override
    public EquipeTecnicaDTO autenticar(String login, String senha) throws Exception {
        if ("admin".equals(login) && "1234".equals(senha)) {
            return new EquipeTecnicaDTO(12345678900L, "admin", "Administrador do Sistema", "ADMIN");
        }
        return null;
    }
}