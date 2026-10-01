package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.dao.PIADAO;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.PIA;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        AdolescenteDAO adoDao = new AdolescenteDAO();
        List<Adolescente> jovens = adoDao.listar();

        if (jovens.isEmpty()) {
            System.out.println("Nenhum adolescente encontrado.");
            return;
        }

        Adolescente jovem = jovens.get(0);
        long cpfJovem = jovem.getCpf();

        System.out.println("--- ELABORAÇÃO DO PIA ---");
        System.out.println("Adolescente: " + jovem.getNomeCompleto());

        PIA pia = new PIA();
        pia.setIdPia(1);
        pia.setDataElaboracao(LocalDate.now());
        pia.setDiagnostico("Adolescente apresenta boa comunicação, mas evade o ambiente escolar.");
        pia.setVulnerabilidades("Baixa renda familiar e convívio em área de risco social.");
        pia.setPotencialidades("Interesse por tecnologia e facilidade com montagem de computadores.");
        pia.setEstrategias("Inserção obrigatória na Oficina de Informática e acompanhamento pedagógico.");
        pia.setDocumentoEnviado(false);
        pia.setCpfAdolescente(cpfJovem);

        PIADAO piaDao = new PIADAO();
        System.out.println("Salvando o Plano Individual de Atendimento no banco...");

        if (piaDao.inserir(pia)) {
            System.out.println("-> Sucesso! PIA registrado com sucesso.");
        } else {
            System.out.println("-> Falha ao registrar PIA. Verifique o console.");
        }
    }
}