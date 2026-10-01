package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        AdolescenteDAO dao = new AdolescenteDAO();

        System.out.println("1. BUSCANDO ADOLESCENTES...");
        List<Adolescente> jovens = dao.listar();

        if (jovens.isEmpty()) {
            System.out.println("Nenhum jovem encontrado no banco para realizar o teste.");
            return;
        }

        // Pega o primeiro jovem da lista (provavelmente o Carlos Eduardo que inserimos antes)
        Adolescente alvo = jovens.get(0);
        long cpfTeste = alvo.getCpf();

        System.out.println("Jovem selecionado: " + alvo.getNomeCompleto());
        System.out.println("Contato original: " + alvo.getContato());
        System.out.println("Status original: " + alvo.getStatus());

        System.out.println("\n2. TESTANDO ATUALIZAÇÃO (UPDATE)...");
        alvo.setContato("(43) 99999-8888"); // Simulando uma mudança de telefone
        alvo.setCorRaca("Parda"); // Simulando correção de um dado

        boolean atualizou = dao.atualizar(alvo);
        System.out.println(atualizou ? "-> Dados atualizados com sucesso!" : "-> Falha ao atualizar.");

        System.out.println("\n3. TESTANDO INATIVAÇÃO (EXCLUSÃO LÓGICA)...");
        boolean inativou = dao.inativar(cpfTeste);
        System.out.println(inativou ? "-> Jovem inativado com sucesso!" : "-> Falha ao inativar.");

        System.out.println("\n4. CONFERINDO O RESULTADO FINAL DIRETO DO BANCO...");
        List<Adolescente> conferir = dao.listar();
        for (Adolescente j : conferir) {
            if (j.getCpf() == cpfTeste) {
                System.out.println("Nome: " + j.getNomeCompleto());
                System.out.println("Novo Contato: " + j.getContato());
                System.out.println("Novo Status: " + j.getStatus()); // Deve imprimir "Inativo"
            }
        }
    }
}