package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.dao.FrequenciaDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.Frequencia;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        AdolescenteDAO adoDao = new AdolescenteDAO();
        List<Adolescente> jovens = adoDao.listar();

        if (jovens.isEmpty()) {
            System.out.println("Nenhum jovem cadastrado para receber presença.");
            return;
        }

        Adolescente jovem = jovens.get(0);

        // Montando o objeto de Frequência
        Frequencia presenca = new Frequencia();
        presenca.setCpfAdolescente(jovem.getCpf());
        presenca.setIdAtividade(1); // O ID da atividade que acabamos de inserir no banco
        presenca.setDataPresenca(LocalDate.now());
        presenca.setStatusPresenca("Presente");
        presenca.setHorasCumpridas(4);

        FrequenciaDAO freqDao = new FrequenciaDAO();
        System.out.println("Registrando presença na oficina para: " + jovem.getNomeCompleto());

        boolean sucesso = freqDao.registrar(presenca);

        if (sucesso) {
            System.out.println("-> Sucesso! Presença contabilizada no banco.");
        } else {
            System.out.println("-> Falha ao registrar presença. Verifique o console.");
        }
    }
}