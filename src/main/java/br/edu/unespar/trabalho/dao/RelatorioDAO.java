package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.RelatorioItem;
import br.edu.unespar.trabalho.util.ConnectionFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RelatorioDAO {

    private static RelatorioDAO instance;
    private final ObservableList<RelatorioItem> listaRelatorios;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RelatorioDAO() {
        listaRelatorios = FXCollections.observableArrayList();
        carregarDoBanco();
    }

    public static synchronized RelatorioDAO getInstance() {
        if (instance == null) {
            instance = new RelatorioDAO();
        }
        return instance;
    }

    public ObservableList<RelatorioItem> getListaRelatorios() {
        return listaRelatorios;
    }

    private void carregarDoBanco() {
        listaRelatorios.clear();
        String sql = "SELECT id_documento, tipo, data_geracao, status_envio FROM Documento ORDER BY data_geracao DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String titulo = "Documento Institucional #" + rs.getInt("id_documento");
                String categoria = rs.getString("tipo");
                String parecer = rs.getString("status_envio");
                String data = rs.getDate("data_geracao").toLocalDate().format(formatter);

                listaRelatorios.add(new RelatorioItem(titulo, categoria, parecer, data));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar relatórios: " + e.getMessage());
        }
    }

    public void adicionar(RelatorioItem item) {
        String sql = "INSERT INTO Documento (id_documento, tipo, data_geracao, status_envio, cpf_adolescente) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Simulação de ID - Num cenário real usar SERIAL/AUTO_INCREMENT
            int novoId = (int) (System.currentTimeMillis() % 100000);

            stmt.setInt(1, novoId);
            stmt.setString(2, item.getCategoria());
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setString(4, item.getParecer());
            stmt.setLong(5, 11122233344L); // Substituir por lógica de seleção de adolescente depois

            if (stmt.executeUpdate() > 0) {
                listaRelatorios.add(0, item);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir relatório: " + e.getMessage());
        }
    }

    public void remover(RelatorioItem item) {
        // Implementação simplificada para a interface baseada na memória
        listaRelatorios.remove(item);
    }
}