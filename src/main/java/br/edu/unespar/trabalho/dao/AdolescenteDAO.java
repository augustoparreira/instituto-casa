package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.Adolescente;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.model.StatusAdolescente;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AdolescenteDAO {

    public boolean inserir(Adolescente adolescente) {
        String sqlPessoa = "INSERT INTO Pessoa (cpf, nome_completo, data_nascimento, contato, email) VALUES (?, ?, ?, ?, ?)";
        String sqlAdolescente = "INSERT INTO Adolescente (cpf_adolescente, naturalidade, genero, cor_raca, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setLong(1, adolescente.getCpf());
                stmtPessoa.setString(2, adolescente.getNomeCompleto());
                stmtPessoa.setDate(3, Date.valueOf(adolescente.getDataNascimento()));
                stmtPessoa.setString(4, adolescente.getContato());
                stmtPessoa.setString(5, adolescente.getEmail());
                stmtPessoa.executeUpdate();
            }

            try (PreparedStatement stmtAdolescente = conn.prepareStatement(sqlAdolescente)) {
                stmtAdolescente.setLong(1, adolescente.getCpf());
                stmtAdolescente.setString(2, adolescente.getNaturalidade());
                stmtAdolescente.setString(3, adolescente.getGenero());
                stmtAdolescente.setString(4, adolescente.getCorRaca());
                stmtAdolescente.setString(5, adolescente.getStatus() != null ? adolescente.getStatus().getCodigo() : null);
                stmtAdolescente.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao salvar adolescente: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { }
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { }
        }
    }

    public List<Adolescente> listar() {
        List<Adolescente> lista = new ArrayList<>();
        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, p.contato, p.email, " +
                "a.naturalidade, a.genero, a.cor_raca, a.status " +
                "FROM Pessoa p INNER JOIN Adolescente a ON p.cpf = a.cpf_adolescente " +
                "WHERE a.status <> 'INATIVO'";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Adolescente jovem = new Adolescente();
                jovem.setCpf(rs.getLong("cpf"));
                jovem.setNomeCompleto(rs.getString("nome_completo"));
                jovem.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                jovem.setContato(rs.getString("contato"));
                jovem.setEmail(rs.getString("email"));
                jovem.setNaturalidade(rs.getString("naturalidade"));
                jovem.setGenero(rs.getString("genero"));
                jovem.setCorRaca(rs.getString("cor_raca"));
                jovem.setStatus(StatusAdolescente.fromCodigo(rs.getString("status")));
                lista.add(jovem);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar adolescentes: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizar(Adolescente adolescente) {
        String sqlPessoa = "UPDATE Pessoa SET nome_completo = ?, data_nascimento = ?, contato = ?, email = ? WHERE cpf = ?";
        String sqlAdolescente = "UPDATE Adolescente SET naturalidade = ?, genero = ?, cor_raca = ?, status = ? WHERE cpf_adolescente = ?";

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setString(1, adolescente.getNomeCompleto());
                stmtPessoa.setDate(2, Date.valueOf(adolescente.getDataNascimento()));
                stmtPessoa.setString(3, adolescente.getContato());
                stmtPessoa.setString(4, adolescente.getEmail());
                stmtPessoa.setLong(5, adolescente.getCpf());
                stmtPessoa.executeUpdate();
            }

            try (PreparedStatement stmtAdolescente = conn.prepareStatement(sqlAdolescente)) {
                stmtAdolescente.setString(1, adolescente.getNaturalidade());
                stmtAdolescente.setString(2, adolescente.getGenero());
                stmtAdolescente.setString(3, adolescente.getCorRaca());
                stmtAdolescente.setString(4, adolescente.getStatus() != null ? adolescente.getStatus().getCodigo() : null);
                stmtAdolescente.setLong(5, adolescente.getCpf());
                stmtAdolescente.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar adolescente: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { }
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { }
        }
    }

    public boolean excluir(long cpf) {
        String sql = "UPDATE Adolescente SET status = 'INATIVO' WHERE cpf_adolescente = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cpf);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir adolescente: " + e.getMessage());
            return false;
        }
    }

    // NOVO MÉTODO PARA A TELA: Executa a lógica complexa mantendo o Controller limpo
    public List<AdolescenteDTO> listarResumoDTO() {
        List<AdolescenteDTO> lista = new ArrayList<>();
        String sql = "SELECT p.cpf, p.nome_completo, p.data_nascimento, a.genero, a.status, " +
                "ss.bairro, m.tipo_medida, m.duracao_horas, m.duracao_meses, m.data_inicio, " +
                "peq.nome_completo AS nome_tecnico " +
                "FROM Pessoa p " +
                "INNER JOIN Adolescente a ON p.cpf = a.cpf_adolescente " +
                "LEFT JOIN SituacaoSocial ss ON ss.cpf_adolescente = a.cpf_adolescente " +
                "LEFT JOIN MedidaSocioeducativa m ON m.cpf_adolescente = a.cpf_adolescente " +
                "LEFT JOIN Acompanhamento ac ON ac.cpf_adolescente = a.cpf_adolescente AND ac.tecnico_referencia = true " +
                "LEFT JOIN Pessoa peq ON peq.cpf = ac.cpf_equipe " +
                "WHERE a.status <> 'INATIVO'";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            MedidaSocioeducativaDAO medidaDao = new MedidaSocioeducativaDAO();

            while (rs.next()) {
                long cpfRaw = rs.getLong("cpf");
                String cpfFormatado = String.format("%011d", cpfRaw);
                cpfFormatado = cpfFormatado.substring(0,3) + "." + cpfFormatado.substring(3,6) + "." + cpfFormatado.substring(6,9) + "-" + cpfFormatado.substring(9);

                String nome = rs.getString("nome_completo");
                java.sql.Date dbData = rs.getDate("data_nascimento");
                String dtNasc = (dbData != null) ? dbData.toLocalDate().format(formatter) : "Não informada";
                String genero = rs.getString("genero");
                if (genero == null) genero = "Não informado";

                String statusRaw = rs.getString("status");
                String status = "Ativo";
                if ("EM_DESCUMPRIMENTO".equalsIgnoreCase(statusRaw)) status = "Suspenso";
                else if ("EM_ANALISE_EXTINCAO".equalsIgnoreCase(statusRaw)) status = "Encerrado";

                String bairro = rs.getString("bairro");
                if (bairro == null) bairro = "Não informado";

                String tecnico = rs.getString("nome_tecnico");
                if (tecnico == null) tecnico = "Sem técnico vinculado";

                String medida = rs.getString("tipo_medida");
                if (medida == null) medida = "N/A";

                int duracaoHoras = rs.getInt("duracao_horas");
                int duracaoMeses = rs.getInt("duracao_meses");
                java.sql.Date dtInicio = rs.getDate("data_inicio");

                double progresso = 0.0;
                String txtProgresso = "--";

                if ("PSC".equalsIgnoreCase(medida) && duracaoHoras > 0) {
                    int horasCumpridas = medidaDao.consultarHorasCumpridas(cpfRaw);
                    progresso = Math.min(1.0, (double) horasCumpridas / duracaoHoras);
                    txtProgresso = horasCumpridas + "/" + duracaoHoras + "h";
                } else if ("LA".equalsIgnoreCase(medida) && duracaoMeses > 0 && dtInicio != null) {
                    int mesesCorridos = (int) ChronoUnit.MONTHS.between(dtInicio.toLocalDate(), LocalDate.now());
                    mesesCorridos = Math.max(0, mesesCorridos);
                    progresso = Math.min(1.0, (double) mesesCorridos / duracaoMeses);
                    txtProgresso = mesesCorridos + "/" + duracaoMeses + "m";
                }

                lista.add(new AdolescenteDTO(
                        nome, cpfFormatado, medida.toUpperCase(), progresso, txtProgresso,
                        tecnico, status, bairro, dtNasc, genero
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar resumo de adolescentes: " + e.getMessage());
        }
        return lista;
    }
}