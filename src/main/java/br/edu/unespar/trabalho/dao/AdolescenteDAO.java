package br.edu.unespar.trabalho.dao;

import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.IdUtil;
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
        // Garante que o registo inicial da Situação Social seja criado com os campos NOT NULL preenchidos e o bairro correto.
        String sqlSituacao = "INSERT INTO SituacaoSocial (id_situacaoSocial, renda, endereco, bairro, telefone, numero_nis, cras_referencia, cpf_adolescente) VALUES ((SELECT COALESCE(MAX(id_situacaoSocial), 0) + 1 FROM SituacaoSocial), 0, 'Não informado', ?, ?, 0, NULL, ?)";

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

            // Insere a Situação Social inicial para guardar o Bairro
            try (PreparedStatement stmtSituacao = conn.prepareStatement(sqlSituacao)) {
                stmtSituacao.setString(1, adolescente.getBairro() != null && !adolescente.getBairro().trim().isEmpty() ? adolescente.getBairro() : "Não informado");
                stmtSituacao.setString(2, adolescente.getContato() != null ? adolescente.getContato() : "Não informado");
                stmtSituacao.setLong(3, adolescente.getCpf());
                stmtSituacao.executeUpdate();
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

    public List<AdolescenteDTO> listarResumoDTO() {
        List<AdolescenteDTO> lista=new ArrayList<>();
        var linhas=new FrequenciaMensalDAO().listar(java.time.YearMonth.now());
        java.util.Map<Long,String> tecnicos=new java.util.HashMap<>();
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT ac.cpf_adolescente,p.nome_completo FROM Acompanhamento ac JOIN Pessoa p ON p.cpf=ac.cpf_equipe WHERE ac.tecnico_referencia=true ORDER BY ac.cpf_equipe"); ResultSet r=s.executeQuery()) {
            while(r.next()) tecnicos.putIfAbsent(r.getLong(1),r.getString(2));
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar os técnicos.",e); }
        for(var linha:linhas) {
            Adolescente a=linha.getAdolescente();
            String progresso=linha.getHorasPrevistas()>0 ? linha.getHorasCumpridas()+"/"+linha.getHorasPrevistas()+"h" : "";
            if(!linha.getMeses().isBlank()) progresso+=(progresso.isEmpty()?"":" · ")+linha.getMeses()+"m";
            double percentual=linha.getHorasPrevistas()>0 ? Math.min(1.0,(double)linha.getHorasCumpridas()/linha.getHorasPrevistas()) :
                    linha.getMedidasDoMes().stream().filter(MedidaSocioeducativa::isLA).mapToDouble(m->Math.min(1.0,(double)m.getMesesCorridos()/m.getDuracaoMeses())).findFirst().orElse(0);
            lista.add(new AdolescenteDTO(a.getNomeCompleto(),a.getCpfFormatado(),linha.getMse().isEmpty()?"Sem medida":linha.getMse(),percentual,
                    progresso.isEmpty()?"—":progresso,tecnicos.getOrDefault(a.getCpf(),"Sem técnico vinculado"),a.getStatus().getDescricao(),
                    a.getBairro()==null?"":a.getBairro(),a.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),a.getGenero()));
        }
        return lista;
    }

    public List<Adolescente> listarCadastros() {
        List<Adolescente> lista=new ArrayList<>();
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT p.*,a.*,ss.bairro FROM Pessoa p JOIN Adolescente a ON a.cpf_adolescente=p.cpf LEFT JOIN SituacaoSocial ss ON ss.cpf_adolescente=p.cpf ORDER BY p.nome_completo"); ResultSet r=s.executeQuery()) {
            while(r.next()) {
                Adolescente a=new Adolescente(); a.setCpf(r.getLong("cpf")); a.setNomeCompleto(r.getString("nome_completo"));
                a.setDataNascimento(r.getDate("data_nascimento").toLocalDate()); a.setGenero(r.getString("genero"));
                a.setBairro(r.getString("bairro")); a.setStatus(StatusAdolescente.fromCodigo(r.getString("status")));
                a.setImm(r.getBoolean("imm")); a.setValeTransporte(r.getBoolean("vale_transporte"));
                a.setPiaEnviado(r.getBoolean("pia_enviado"));
                a.setMedidaProtetiva(r.getBoolean("medida_protetiva")); lista.add(a);
            }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar adolescentes. Confira o ajuste SQL de cadastro e frequência.",e); }
        return lista;
    }

    public Adolescente buscarPorCpf(long cpf) {
        String sql = "SELECT p.*, a.*, ss.bairro FROM Pessoa p JOIN Adolescente a ON a.cpf_adolescente=p.cpf "
                + "LEFT JOIN SituacaoSocial ss ON ss.cpf_adolescente=p.cpf WHERE p.cpf=?";
        try (Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setLong(1,cpf);
            try (ResultSet r=s.executeQuery()) {
                if (!r.next()) return null;
                Adolescente a=new Adolescente();
                a.setCpf(cpf); a.setNomeCompleto(r.getString("nome_completo"));
                a.setDataNascimento(r.getDate("data_nascimento").toLocalDate());
                a.setContato(r.getString("contato")); a.setEmail(r.getString("email"));
                a.setNaturalidade(r.getString("naturalidade")); a.setGenero(r.getString("genero"));
                a.setCorRaca(r.getString("cor_raca")); a.setBairro(r.getString("bairro"));
                a.setStatus(StatusAdolescente.fromCodigo(r.getString("status")));
                a.setObservacoes(r.getString("observacoes")); a.setImm(r.getBoolean("imm"));
                a.setValeTransporte(r.getBoolean("vale_transporte"));
                a.setPiaEnviado(r.getBoolean("pia_enviado"));
                a.setMedidaProtetiva(r.getBoolean("medida_protetiva"));
                return a;
            }
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível carregar o cadastro.",e); }
    }

    /** Dados pessoais, sociais, saúde e escolaridade são salvos juntos ou revertidos juntos. */
    public boolean salvarCadastro(Adolescente a, SituacaoSocial social, Saude saude, EducacaoTrabalho estudo, boolean edicao) {
        if(a.getNomeCompleto()==null || a.getNomeCompleto().isBlank() || a.getNomeCompleto().length()>80)
            throw new IllegalArgumentException("Informe o nome completo (até 80 caracteres).");
        if(a.getCpf()<=0 || a.getDataNascimento()==null || a.getDataNascimento().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("CPF ou data de nascimento inválidos.");
        if(!Double.isFinite(social.getRendaFamiliar()) || social.getRendaFamiliar()<0)
            throw new IllegalArgumentException("Renda deve ser um valor não negativo em salários mínimos.");
        if (social.getEndereco()==null || social.getEndereco().isBlank()
                || social.getBairro()==null || social.getBairro().isBlank()
                || a.getContato()==null || a.getContato().isBlank())
            throw new IllegalArgumentException("Endereço, bairro e telefone são obrigatórios.");
        saude.validar(); estudo.validar();
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                if(edicao) {
                    if(executarCadastro(c,"UPDATE Pessoa SET nome_completo=?,data_nascimento=?,contato=?,email=? WHERE cpf=?",
                            a.getNomeCompleto(),a.getDataNascimento(),a.getContato(),a.getEmail(),a.getCpf())!=1)
                        throw new SQLException("Cadastro não encontrado.");
                    executarCadastro(c,"UPDATE Adolescente SET naturalidade=?,genero=?,cor_raca=?,status=?,observacoes=?,imm=?,vale_transporte=?,pia_enviado=?,medida_protetiva=? WHERE cpf_adolescente=?",
                            a.getNaturalidade(),a.getGenero(),a.getCorRaca(),a.getStatus().getCodigo(),a.getObservacoes(),a.isImm(),a.isValeTransporte(),a.isPiaEnviado(),a.isMedidaProtetiva(),a.getCpf());
                } else {
                    executarCadastro(c,"INSERT INTO Pessoa(cpf,nome_completo,data_nascimento,contato,email) VALUES(?,?,?,?,?)",
                            a.getCpf(),a.getNomeCompleto(),a.getDataNascimento(),a.getContato(),a.getEmail());
                    executarCadastro(c,"INSERT INTO Adolescente(cpf_adolescente,naturalidade,genero,cor_raca,status,observacoes,imm,vale_transporte,pia_enviado,medida_protetiva) VALUES(?,?,?,?,?,?,?,?,?,?)",
                            a.getCpf(),a.getNaturalidade(),a.getGenero(),a.getCorRaca(),a.getStatus().getCodigo(),a.getObservacoes(),a.isImm(),a.isValeTransporte(),a.isPiaEnviado(),a.isMedidaProtetiva());
                }
                executarCadastro(c,"INSERT INTO SituacaoSocial(id_situacaoSocial,renda,beneficios_sociais,endereco,bairro,telefone,numero_nis,cras_nome,cpf_adolescente) "
                        + "VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(cpf_adolescente) DO UPDATE SET renda=excluded.renda,beneficios_sociais=excluded.beneficios_sociais,"
                        + "endereco=excluded.endereco,bairro=excluded.bairro,telefone=excluded.telefone,numero_nis=excluded.numero_nis,cras_nome=excluded.cras_nome",
                        IdUtil.proximoId(c,"SituacaoSocial"),social.getRendaFamiliar(),social.getBeneficioSocial(),social.getEndereco(),social.getBairro(),
                        a.getContato(),social.getNumeroNis(),social.getCrasNome(),a.getCpf());
                executarCadastro(c,"INSERT INTO Saude(id_fichaSaude,ubs_referencia,uso_spa,observacoes,substancias_utilizadas,cpf_adolescente) VALUES(?,?,?,?,?,?) "
                        + "ON CONFLICT(cpf_adolescente) DO UPDATE SET ubs_referencia=excluded.ubs_referencia,uso_spa=excluded.uso_spa,observacoes=excluded.observacoes,substancias_utilizadas=excluded.substancias_utilizadas",
                        IdUtil.proximoId(c,"Saude"),saude.getUbsReferencia(),saude.isUsoSpa(),saude.getObservacoes(),saude.getSubstanciasUtilizadas(),a.getCpf());
                executarCadastro(c,"INSERT INTO EducacaoTrabalho(id_educacaoTrabalho,estuda,escola,ano_serie,trabalha,local_trabalho,funcao,vinculo_empregaticio,cpf_adolescente) VALUES(?,?,?,?,?,?,?,?,?) "
                        + "ON CONFLICT(cpf_adolescente) DO UPDATE SET estuda=excluded.estuda,escola=excluded.escola,ano_serie=excluded.ano_serie,trabalha=excluded.trabalha,local_trabalho=excluded.local_trabalho,funcao=excluded.funcao,vinculo_empregaticio=excluded.vinculo_empregaticio",
                        IdUtil.proximoId(c,"EducacaoTrabalho"),estudo.isEstuda(),estudo.getEscola(),estudo.getSerie(),estudo.isTrabalha(),estudo.getLocalTrabalho(),estudo.getFuncao(),estudo.getVinculoEmpregaticio(),a.getCpf());
                c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) throw new IllegalArgumentException("Este CPF já está cadastrado. Abra o cadastro existente para editar.",e);
            throw new IllegalStateException("Não foi possível salvar o cadastro completo. Nenhuma alteração foi gravada.",e);
        }
    }

    private int executarCadastro(Connection c,String sql,Object... valores) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            return s.executeUpdate();
        }
    }

}
