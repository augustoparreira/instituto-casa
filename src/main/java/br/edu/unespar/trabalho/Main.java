package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.dao.FrequenciaDAO;
import br.edu.unespar.trabalho.dao.ResponsavelDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.Responsavel;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        AdolescenteDAO adoDao = new AdolescenteDAO();
        List<Adolescente> jovens = adoDao.listar();

        if (jovens.isEmpty()) return;

        Adolescente jovem = jovens.get(0);
        long cpfJovem = jovem.getCpf();
        System.out.println("Relatório do Jovem: " + jovem.getNomeCompleto() + "\n");

        // 1. Testando a listagem de responsáveis
        ResponsavelDAO respDao = new ResponsavelDAO();
        List<Responsavel> familia = respDao.listarResponsaveis(cpfJovem);

        System.out.println("--- COMPOSIÇÃO FAMILIAR ---");
        for (Responsavel r : familia) {
            System.out.println("Nome: " + r.getNomeCompleto());
            System.out.println("Vínculo: " + r.getParentesco() + (r.isContatoPrincipal() ? " (Contato Principal)" : ""));
            System.out.println("Telefone: " + r.getContato() + "\n");
        }

        // 2. Testando a contagem de faltas
        FrequenciaDAO freqDao = new FrequenciaDAO();
        int totalFaltas = freqDao.consultarFaltas(cpfJovem);

        System.out.println("--- CONTROLE DE MEDIDA ---");
        System.out.println("Faltas acumuladas: " + totalFaltas);
    }
}