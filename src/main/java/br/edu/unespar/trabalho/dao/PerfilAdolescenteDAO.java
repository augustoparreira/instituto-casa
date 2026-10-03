package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.FrequenciaLinhaDTO;
import br.edu.unespar.trabalho.model.MedidaSocioeducativa;
import br.edu.unespar.trabalho.model.PIA;
import br.edu.unespar.trabalho.model.StatusPresenca;
import br.edu.unespar.trabalho.model.TipoMedida;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas e ações da tela de perfil do adolescente que os DAOs do back ainda não oferecem. */
public class PerfilAdolescenteDAO {

    /** Medida mais recente do adolescente, ou null se ele ainda não tem medida. */
    public MedidaSocioeducativa buscarMedidaAtual(long cpfAdolescente) {
        String sql = "SELECT id_medida, reincidencia, tipo_medida, data_inicio, historico_infracional, " +
                "duracao_meses, duracao_horas FROM MedidaSocioeducativa " +
                "WHERE cpf_adolescente = ? ORDER BY data_inicio DESC LIMIT 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                MedidaSocioeducativa m = new MedidaSocioeducativa();
                m.setIdMedida(rs.getInt("id_medida"));
                m.setCpfAdolescente(cpfAdolescente);
                m.setReincidencia(rs.getBoolean("reincidencia"));
                m.setTipoMedida(TipoMedida.fromCodigo(rs.getString("tipo_medida")));
                m.setDataInicio(rs.getDate("data_inicio").toLocalDate());
                m.setHistoricoInfracional(rs.getString("historico_infracional"));

                int meses = rs.getInt("duracao_meses");
                m.setDuracaoMeses(rs.wasNull() ? null : meses);
                int horas = rs.getInt("duracao_horas");
                m.setDuracaoHoras(rs.wasNull() ? null : horas);
                return m;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar medida: " + e.getMessage());
            return null;
        }
    }

    /** CPF do técnico de referência do adolescente, ou null se não houver. */
    public Long buscarCpfTecnicoReferencia(long cpfAdolescente) {
        String sql = "SELECT cpf_equipe FROM Acompanhamento " +
                "WHERE cpf_adolescente = ? AND tecnico_referencia = true LIMIT 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("cpf_equipe") : null;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar técnico de referência: " + e.getMessage());
            return null;
        }
    }

    /**
     * Define o técnico de referência: tira a marca de quem era referência,
     * marca o novo (criando o vínculo se ele ainda não existir). Tudo numa transação.
     */
    public boolean definirTecnicoReferencia(long cpfAdolescente, long cpfEquipe) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement s1 = conn.prepareStatement(
                        "UPDATE Acompanhamento SET tecnico_referencia = false WHERE cpf_adolescente = ?")) {
                    s1.setLong(1, cpfAdolescente);
                    s1.executeUpdate();
                }

                int atualizados;
                try (PreparedStatement s2 = conn.prepareStatement(
                        "UPDATE Acompanhamento SET tecnico_referencia = true " +
                                "WHERE cpf_adolescente = ? AND cpf_equipe = ?")) {
                    s2.setLong(1, cpfAdolescente);
                    s2.setLong(2, cpfEquipe);
                    atualizados = s2.executeUpdate();
                }

                if (atualizados == 0) {
                    try (PreparedStatement s3 = conn.prepareStatement(
                            "INSERT INTO Acompanhamento (cpf_adolescente, cpf_equipe, tecnico_referencia) VALUES (?, ?, true)")) {
                        s3.setLong(1, cpfAdolescente);
                        s3.setLong(2, cpfEquipe);
                        s3.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erro ao definir técnico de referência: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro de conexão ao definir técnico: " + e.getMessage());
            return false;
        }
    }

    // ===================== FREQUÊNCIA =====================

    /** Registros de frequência do adolescente (mais recentes primeiro), já com o nome da atividade. */
    public List<FrequenciaLinhaDTO> listarFrequencia(long cpfAdolescente) {
        List<FrequenciaLinhaDTO> lista = new ArrayList<>();
        String sql = "SELECT f.data_presenca, f.status_presenca, f.horas_cumpridas, a.nome_atividade " +
                "FROM Frequencia f INNER JOIN Atividade a ON a.id_atividade = f.id_atividade " +
                "WHERE f.cpf_adolescente = ? ORDER BY f.data_presenca DESC, a.nome_atividade";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StatusPresenca status;
                    try {
                        status = StatusPresenca.fromCodigo(rs.getString("status_presenca"));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Registro de frequência ignorado: " + e.getMessage());
                        continue;
                    }
                    int horas = rs.getInt("horas_cumpridas");
                    Integer horasOuNulo = rs.wasNull() ? null : horas;
                    lista.add(new FrequenciaLinhaDTO(rs.getDate("data_presenca").toLocalDate(),
                            status, horasOuNulo, rs.getString("nome_atividade")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar frequência: " + e.getMessage());
        }
        return lista;
    }

    /** Já existe presença/falta desse adolescente nessa atividade nessa data? (chave primária da tabela) */
    public boolean existeFrequencia(long cpfAdolescente, int idAtividade, LocalDate data) {
        String sql = "SELECT 1 FROM Frequencia WHERE cpf_adolescente = ? AND id_atividade = ? AND data_presenca = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, cpfAdolescente);
            stmt.setInt(2, idAtividade);
            stmt.setDate(3, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar frequência: " + e.getMessage());
            return false;
        }
    }

    // ===================== PIA =====================

    /** Nome do técnico que elaborou o PIA (tabela ElaborarPIA), ou null. */
    public String buscarNomeAutorPia(int idPia) {
        String sql = "SELECT p.nome_completo FROM ElaborarPIA e " +
                "INNER JOIN Pessoa p ON p.cpf = e.cpf_equipe WHERE e.id_pia = ? LIMIT 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPia);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("nome_completo") : null;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar autor do PIA: " + e.getMessage());
            return null;
        }
    }

    /** Grava o PIA e o vínculo com o técnico que o elaborou na mesma transação. */
    public boolean criarPia(PIA pia, long cpfEquipe) {
        String sqlPia = "INSERT INTO PIA (id_pia, data_elaboracao, diagnostico, vulnerabilidades, " +
                "potencialidades, estrategias, documento_enviado, cpf_adolescente) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement s1 = conn.prepareStatement(sqlPia)) {
                    s1.setInt(1, pia.getIdPia());
                    s1.setDate(2, Date.valueOf(pia.getDataElaboracao()));
                    s1.setString(3, pia.getDiagnostico());
                    s1.setString(4, pia.getVulnerabilidades());
                    s1.setString(5, pia.getPotencialidades());
                    s1.setString(6, pia.getEstrategias());
                    s1.setBoolean(7, pia.isDocumentoEnviado());
                    s1.setLong(8, pia.getCpfAdolescente());
                    s1.executeUpdate();
                }
                try (PreparedStatement s2 = conn.prepareStatement(
                        "INSERT INTO ElaborarPIA (cpf_equipe, id_pia) VALUES (?, ?)")) {
                    s2.setLong(1, cpfEquipe);
                    s2.setInt(2, pia.getIdPia());
                    s2.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erro ao criar PIA: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro de conexão ao criar PIA: " + e.getMessage());
            return false;
        }
    }
}