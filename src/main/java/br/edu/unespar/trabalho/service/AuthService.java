package br.edu.unespar.trabalho.service;

import br.edu.unespar.trabalho.model.EquipeTecnicaDTO;

public interface AuthService {
    EquipeTecnicaDTO autenticar(String login, String senha) throws Exception;
}