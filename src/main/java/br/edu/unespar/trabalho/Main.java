package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        // Simulando os dados que viriam da tela do Gabriel
        Adolescente jovem = new Adolescente();
        jovem.setCpf(99988877766L); // L no final avisa o Java que é um número BIGINT (long)
        jovem.setNomeCompleto("Carlos Eduardo da Silva");
        jovem.setDataNascimento(LocalDate.of(2008, 5, 20)); // 20 de Maio de 2008
        jovem.setContato("43999999999");
        jovem.setEmail("carlos@email.com");

        jovem.setNaturalidade("Apucarana");
        jovem.setGenero("Masculino");
        jovem.setCorRaca("Parda");
        jovem.setStatus("Ativo");

        System.out.println("Iniciando inserção no banco...");

        // Chamando o seu motor
        AdolescenteDAO dao = new AdolescenteDAO();
        boolean sucesso = dao.inserir(jovem);

        if (sucesso) {
            System.out.println("SUCESSO ABSOLUTO! Carlos foi salvo nas tabelas Pessoa e Adolescente.");
        } else {
            System.out.println("FALHA! Ocorreu um erro no cadastro.");
        }
    }
}