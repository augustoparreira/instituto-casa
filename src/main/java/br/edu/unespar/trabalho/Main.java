package br.edu.unespar.trabalho;

import br.edu.unespar.trabalho.dao.AdolescenteDAO;
import br.edu.unespar.trabalho.model.Adolescente;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        AdolescenteDAO dao = new AdolescenteDAO();

        System.out.println("Buscando adolescentes no banco db_instituto_casa...");
        List<Adolescente> jovens = dao.listar();

        for (Adolescente j : jovens) {
            System.out.println("Nome: " + j.getNomeCompleto() + " | Status: " + j.getStatus());
        }
    }
}