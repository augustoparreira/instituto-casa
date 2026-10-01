package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.dao.MedidaSocioeducativaDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.MedidaSocioeducativa;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        AdolescenteDAO adoDao = new AdolescenteDAO();
        List<Adolescente> jovens = adoDao.listar();

        if (jovens.isEmpty()) {
            System.out.println("Nenhum adolescente encontrado no banco.");
            return;
        }

        Adolescente jovem = jovens.get(0);
        long cpfJovem = jovem.getCpf();

        System.out.println("--- TESTE DE MEDIDA SOCIOEDUCATIVA ---");
        System.out.println("Adolescente: " + jovem.getNomeCompleto());

        // 1. Configurando a Medida Judicial de 120 horas
        MedidaSocioeducativa medida = new MedidaSocioeducativa();
        medida.setIdMedida(1);
        medida.setCpfAdolescente(cpfJovem);
        medida.setReincidencia(false);
        medida.setTipoMedida("PSC"); // Prestação de Serviços à Comunidade
        medida.setDataInicio(LocalDate.now());
        medida.setHistoricoInfracional("Ato infracional leve");
        medida.setDuracaoMeses(null); // PSC usa apenas horas
        medida.setDuracaoHoras(120);

        MedidaSocioeducativaDAO medidaDao = new MedidaSocioeducativaDAO();

        System.out.println("Gravando medida no PostgreSQL...");
        boolean sucesso = medidaDao.inserir(medida);

        if (sucesso) {
            System.out.println("-> Sucesso! Medida de PSC (120h) registrada.");
        } else {
            System.out.println("-> Aviso: Medida falhou (ou o ID 1 já foi cadastrado).");
        }

        // 2. O Método de Ouro: Calculando o Progresso
        System.out.println("\n--- CÁLCULO DE PROGRESSO JUDICIAL ---");
        int horasCumpridas = medidaDao.consultarHorasCumpridas(cpfJovem);

        System.out.println("-> Horas cumpridas em oficinas: " + horasCumpridas + "h");
        System.out.println("-> Meta estipulada pelo juiz: " + medida.getDuracaoHoras() + "h");

        // Calculando a porcentagem exata que o Gabriel e o João Vitor precisam para a tela
        double porcentagem = ((double) horasCumpridas / medida.getDuracaoHoras()) * 100;
        System.out.println("-> Progresso Total: " + String.format("%.1f", porcentagem) + "%");
    }
}