package br.edu.unespar.trabalho.dao;

import static br.edu.unespar.trabalho.service.AutorizacaoService.*;
import static br.edu.unespar.trabalho.model.Operacao.*;
import br.edu.unespar.trabalho.model.*;
import br.edu.unespar.trabalho.util.IdUtil;
import br.edu.unespar.trabalho.model.AdolescenteDTO;
import br.edu.unespar.trabalho.model.StatusAdolescente;
import br.edu.unespar.trabalho.util.ConnectionFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste e consulta adolescentes; salva o cadastro completo em uma transação JDBC.
 */
public class AdolescenteDAO {

    public boolean inserir(Adolescente adolescente) {
        exigir(SOCIAL_ALTERAR);
        return salvarDadosOperacionais(adolescente, new EducacaoTrabalho(), false);
    }

    public List<Adolescente> listar() {
        exigir(ADOLESCENTE_CONSULTAR);

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
        exigir(ADOLESCENTE_EDITAR);

        String sqlPessoa = "UPDATE Pessoa SET nome_completo = ?, data_nascimento = ?, contato = ?, email = ? WHERE cpf = ?";
        String sqlAdolescente = "UPDATE Adolescente SET naturalidade = ?, genero = ?, cor_raca = ? WHERE cpf_adolescente = ?";

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);
            exigirEdicaoDePessoa(conn,adolescente.getCpf());

            try (PreparedStatement stmtPessoa = conn.prepareStatement(sqlPessoa)) {
                stmtPessoa.setString(1, adolescente.getNomeCompleto());
                stmtPessoa.setDate(2, Date.valueOf(adolescente.getDataNascimento()));
                stmtPessoa.setString(3, ContatoPessoaDAO.validar(conn,adolescente.getCpf(),adolescente.getContato(),true,true));
                stmtPessoa.setString(4, adolescente.getEmail());
                stmtPessoa.setLong(5, adolescente.getCpf());
                stmtPessoa.executeUpdate();
            }

            try (PreparedStatement stmtAdolescente = conn.prepareStatement(sqlAdolescente)) {
                stmtAdolescente.setString(1, adolescente.getNaturalidade());
                stmtAdolescente.setString(2, adolescente.getGenero());
                stmtAdolescente.setString(3, adolescente.getCorRaca());
                stmtAdolescente.setLong(4, adolescente.getCpf());
                if (stmtAdolescente.executeUpdate() != 1) throw new SQLException("Adolescente não encontrado.");
            }

            conn.commit();
            return true;

        } catch (RuntimeException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
            throw e;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar adolescente: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { }
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { }
        }
    }

    public boolean excluir(long cpf) {
        exigir(ADOLESCENTE_EXCLUIR);
        exigir(ADOLESCENTE_SITUACAO_ALTERAR);

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
        return consultarResumos(null);
    }

    public AdolescenteDTO buscarResumoDTO(long cpf) {
        var resumos = consultarResumos(cpf);
        return resumos.isEmpty() ? null : resumos.getFirst();
    }

    private List<AdolescenteDTO> consultarResumos(Long cpf) {
        var permissoes = permissoesDaConsulta(ADOLESCENTE_CONSULTAR,FREQUENCIA_CONSULTAR);
        boolean social = permissoes.contains(SOCIAL_CONSULTAR);
        boolean medidas = permissoes.contains(MEDIDA_CONSULTAR);
        boolean horas = medidas && permissoes.contains(HORAS_PSC_CONSULTAR);
        LocalDate hoje = LocalDate.now();
        var mes = java.time.YearMonth.from(hoje);
        String sql = """
                SELECT p.cpf,p.nome_completo,p.data_nascimento,a.genero,a.status,t.nome_tecnico,
                       %s AS bairro, m.id_medida,m.tipo_medida,m.data_inicio,m.data_fim,
                       m.duracao_meses,m.duracao_horas,%s AS horas_cumpridas
                FROM Adolescente a JOIN Pessoa p ON p.cpf=a.cpf_adolescente
                %s
                LEFT JOIN MedidaSocioeducativa m ON %s
                %s
                LEFT JOIN LATERAL (
                    SELECT tecnico.nome_completo AS nome_tecnico
                    FROM Acompanhamento ac JOIN Pessoa tecnico ON tecnico.cpf=ac.cpf_equipe
                    WHERE ac.cpf_adolescente=a.cpf_adolescente AND ac.tecnico_referencia=true
                    ORDER BY ac.cpf_equipe LIMIT 1
                ) t ON true
                %s
                ORDER BY p.nome_completo,p.cpf,m.data_inicio,m.id_medida
                """.formatted(social?"ss.bairro":"NULL::text",horas?"COALESCE(h.total,0)":"0",
                social?"LEFT JOIN SituacaoSocial ss ON ss.cpf_adolescente=a.cpf_adolescente":"",
                medidas ? """
                        m.cpf_adolescente=a.cpf_adolescente AND m.data_inicio<=?
                        AND (m.data_fim IS NULL OR m.data_fim>=?)
                        """ : "false",
                horas ? """
                        LEFT JOIN LATERAL (
                            SELECT SUM(f.horas_cumpridas) AS total
                            FROM Frequencia f
                            WHERE upper(btrim(m.tipo_medida))='PSC' AND f.id_medida=m.id_medida
                              AND f.cpf_adolescente=a.cpf_adolescente
                              AND f.data_presenca BETWEEN DATE '1900-01-01' AND ?
                              AND upper(btrim(f.status_presenca))='PRESENTE'
                        ) h ON true
                        """ : "",
                cpf==null?"":"WHERE a.cpf_adolescente=?");
        List<AdolescenteDTO> lista = new ArrayList<>();
        try (Connection c=ConnectionFactory.getConnection(); PreparedStatement consulta=c.prepareStatement(sql)) {
            int parametro=1;
            if (medidas) {
                consulta.setObject(parametro++,mes.atEndOfMonth());
                consulta.setObject(parametro++,mes.atDay(1));
            }
            if (horas) consulta.setObject(parametro++,hoje);
            if (cpf!=null) consulta.setLong(parametro,cpf);
            try (ResultSet r=consulta.executeQuery()) {
                ResumoCadastro atual = null;
                while (r.next()) {
                    if (atual==null || atual.cpf!=r.getLong("cpf")) {
                        if (atual!=null) lista.add(atual.toDTO());
                        atual = new ResumoCadastro(r);
                    }
                    if (r.getObject("id_medida")!=null) atual.adicionarMedida(r,hoje);
                }
                if (atual!=null) lista.add(atual.toDTO());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível consultar o resumo dos adolescentes.",e);
        }
        return lista;
    }

    private static final class ResumoCadastro {
        private final long cpf;
        private final String nome, nascimento, genero, status, bairro, tecnico;
        private final java.util.EnumSet<TipoMedida> tipos = java.util.EnumSet.noneOf(TipoMedida.class);
        private final List<String> mesesLA = new ArrayList<>();
        private double progressoLA;
        private int previstas, cumpridas;

        private ResumoCadastro(ResultSet r) throws SQLException {
            cpf = r.getLong("cpf");
            nome = r.getString("nome_completo");
            nascimento = r.getObject("data_nascimento",LocalDate.class).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            genero = r.getString("genero");
            status = StatusAdolescente.fromCodigo(r.getString("status")).getDescricao();
            bairro = java.util.Objects.toString(r.getString("bairro"),"");
            tecnico = java.util.Objects.toString(r.getString("nome_tecnico"),"Sem técnico vinculado");
        }

        private void adicionarMedida(ResultSet r, LocalDate hoje) throws SQLException {
            TipoMedida tipo = TipoMedida.fromCodigo(r.getString("tipo_medida"));
            tipos.add(tipo);
            if (tipo==TipoMedida.PSC) {
                previstas += r.getInt("duracao_horas");
                cumpridas += (int)r.getLong("horas_cumpridas");
            } else {
                MedidaSocioeducativa medida = new MedidaSocioeducativa();
                medida.setDataInicio(r.getObject("data_inicio",LocalDate.class));
                medida.setDataFim(r.getObject("data_fim",LocalDate.class));
                int duracao = r.getInt("duracao_meses");
                int decorridos = medida.getMesesCorridos(hoje);
                if (mesesLA.isEmpty()) progressoLA = Math.min(1.0,(double)decorridos/duracao);
                mesesLA.add(decorridos+"/"+duracao);
            }
        }

        private AdolescenteDTO toDTO() {
            String medida = tipos.isEmpty()?"Sem medida":tipos.stream().map(TipoMedida::getCodigo)
                    .collect(java.util.stream.Collectors.joining("/"));
            String texto = previstas>0 ? cumpridas+"/"+previstas+"h" : "";
            if (!mesesLA.isEmpty()) texto+=(texto.isEmpty()?"":" · ")+String.join("; ",mesesLA)+"m";
            double progresso = previstas>0 ? Math.min(1.0,(double)cumpridas/previstas) : progressoLA;
            return new AdolescenteDTO(nome, Pessoa.formatarCpf(cpf), medida, progresso,
                    texto.isEmpty()?"—":texto,tecnico,status,bairro,nascimento,genero);
        }
    }

    public List<Adolescente> listarCadastros() {
        exigir(ADOLESCENTE_CONSULTAR);

        List<Adolescente> lista=new ArrayList<>();
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                consultaCadastro()+" ORDER BY p.nome_completo"); ResultSet r=s.executeQuery()) {
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
        exigir(ADOLESCENTE_CONSULTAR);

        String sql = consultaCadastro()+" WHERE p.cpf=?";
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

    public List<Adolescente> listarParaFrequencia() {
        exigir(FREQUENCIA_CONSULTAR);
        List<Adolescente> lista=new ArrayList<>();
        try(Connection c=ConnectionFactory.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT p.cpf,p.nome_completo,a.status FROM Pessoa p JOIN Adolescente a ON a.cpf_adolescente=p.cpf ORDER BY p.nome_completo");
            ResultSet rs=s.executeQuery()) {
            while(rs.next()) {
                Adolescente a=new Adolescente(); a.setCpf(rs.getLong(1)); a.setNomeCompleto(rs.getString(2));
                a.setStatus(StatusAdolescente.fromCodigo(rs.getString(3))); lista.add(a);
            }
            return lista;
        } catch(SQLException e) { throw new IllegalStateException("Não foi possível consultar participantes.",e); }
    }

    private String consultaCadastro() {
        String social = pode(SOCIAL_CONSULTAR) ? "ss.bairro" : "NULL::text AS bairro";
        String pia = pode(PIA_CONSULTAR) ? "a.pia_enviado" : "false AS pia_enviado";
        String notas = pode(SIGILOSO_CONSULTAR) ? "a.observacoes" : "NULL::text AS observacoes";
        return "SELECT p.*,a.naturalidade,a.genero,a.cor_raca,a.status,"+notas+",a.imm,a.vale_transporte,a.medida_protetiva,"
                + pia + "," + social + " FROM Pessoa p JOIN Adolescente a ON a.cpf_adolescente=p.cpf"
                + (pode(SOCIAL_CONSULTAR) ? " LEFT JOIN SituacaoSocial ss ON ss.cpf_adolescente=p.cpf" : "");
    }

    public boolean salvarDadosOperacionais(Adolescente a, EducacaoTrabalho estudo) {
        return salvarDadosOperacionais(a, estudo, true);
    }

    public boolean salvarDadosOperacionais(Adolescente a, EducacaoTrabalho estudo, boolean edicao) {
        exigir(edicao ? ADOLESCENTE_EDITAR : ADOLESCENTE_CADASTRAR);
        exigir(EDUCACAO_ALTERAR);
        if(a.getNomeCompleto()==null || a.getNomeCompleto().isBlank() || a.getNomeCompleto().length()>80
                || a.getCpf()<=0 || a.getDataNascimento()==null || a.getDataNascimento().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Confira nome e nascimento.");
        if(a.getContato()==null || a.getContato().isBlank())
            throw new IllegalArgumentException("Telefone é obrigatório.");
        estudo.validar();
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (edicao) {
                    exigirEdicaoDePessoa(c,a.getCpf());
                    if (executarCadastro(c,"UPDATE Adolescente SET naturalidade=?,genero=?,cor_raca=?,imm=?,vale_transporte=?,medida_protetiva=? WHERE cpf_adolescente=?",
                            a.getNaturalidade(),a.getGenero(),a.getCorRaca(),a.isImm(),a.isValeTransporte(),a.isMedidaProtetiva(),a.getCpf()) != 1)
                        throw new IllegalArgumentException("Cadastro não encontrado.");
                    executarCadastro(c,"UPDATE Pessoa SET nome_completo=?,data_nascimento=?,contato=?,email=? WHERE cpf=?",
                            a.getNomeCompleto(),a.getDataNascimento(),ContatoPessoaDAO.validar(c,a.getCpf(),a.getContato(),true,true),a.getEmail(),a.getCpf());
                } else {
                    executarCadastro(c,"INSERT INTO Pessoa(cpf,nome_completo,data_nascimento,contato,email) VALUES(?,?,?,?,?)",
                            a.getCpf(),a.getNomeCompleto(),a.getDataNascimento(),ContatoPessoaDAO.validar(c,a.getCpf(),a.getContato(),false,true),a.getEmail());
                    executarCadastro(c,"INSERT INTO Adolescente(cpf_adolescente,naturalidade,genero,cor_raca,status,imm,vale_transporte,medida_protetiva) VALUES(?,?,?,?,?,?,?,?)",
                            a.getCpf(),a.getNaturalidade(),a.getGenero(),a.getCorRaca(),StatusAdolescente.ATIVO.getCodigo(),a.isImm(),a.isValeTransporte(),a.isMedidaProtetiva());
                }
                executarCadastro(c,"INSERT INTO EducacaoTrabalho(id_educacaoTrabalho,estuda,escola,ano_serie,trabalha,local_trabalho,funcao,vinculo_empregaticio,cpf_adolescente) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(cpf_adolescente) DO UPDATE SET estuda=excluded.estuda,escola=excluded.escola,ano_serie=excluded.ano_serie,trabalha=excluded.trabalha,local_trabalho=excluded.local_trabalho,funcao=excluded.funcao,vinculo_empregaticio=excluded.vinculo_empregaticio",
                        IdUtil.proximoId(c,"EducacaoTrabalho"),estudo.isEstuda(),estudo.getEscola(),estudo.getSerie(),estudo.isTrabalha(),estudo.getLocalTrabalho(),estudo.getFuncao(),estudo.getVinculoEmpregaticio(),a.getCpf());
                c.commit(); return true;
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) {
            if ("23505".equals(e.getSQLState())) throw new IllegalArgumentException("O CPF já está cadastrado.",e);
            throw new IllegalStateException("Não foi possível salvar os dados operacionais.",e);
        }
    }

    /** Dados pessoais, sociais, saúde e escolaridade são salvos juntos ou revertidos juntos. */
    public boolean salvarCadastro(Adolescente a, SituacaoSocial social, Saude saude, EducacaoTrabalho estudo, boolean edicao) {
        return salvarCadastro(a,social,saude,estudo,edicao,true);
    }

    public boolean salvarCadastro(Adolescente a, SituacaoSocial social, Saude saude,
                                  EducacaoTrabalho estudo, boolean edicao, boolean alterarSituacao) {
        exigir(SIGILOSO_ALTERAR);
        if (edicao && alterarSituacao) {
            exigir(ADOLESCENTE_SITUACAO_ALTERAR);
            if (a.getStatus()==null) throw new IllegalArgumentException("Informe a situação do cadastro.");
        }
        exigir(edicao ? ADOLESCENTE_EDITAR : ADOLESCENTE_CADASTRAR); exigir(SOCIAL_ALTERAR); exigir(SAUDE_ALTERAR); exigir(EDUCACAO_ALTERAR); exigir(PIA_ALTERAR);

        if(a.getNomeCompleto()==null || a.getNomeCompleto().isBlank() || a.getNomeCompleto().length()>80)
            throw new IllegalArgumentException("Informe o nome completo (até 80 caracteres).");
        if(a.getCpf()<=0 || a.getDataNascimento()==null || a.getDataNascimento().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("CPF ou data de nascimento inválidos.");
        if (a.getContato()==null || a.getContato().isBlank())
            throw new IllegalArgumentException("Informe o telefone/contato.");
        if (social==null) throw new IllegalArgumentException("Informe os dados da situação social.");
        social.validarCadastro();
        saude.validar(); estudo.validar();
        try(Connection c=ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                if(edicao) {
                    exigirEdicaoDePessoa(c,a.getCpf());
                    if(executarCadastro(c,"UPDATE Pessoa SET nome_completo=?,data_nascimento=?,contato=?,email=? WHERE cpf=?",
                            a.getNomeCompleto(),a.getDataNascimento(),ContatoPessoaDAO.validar(c,a.getCpf(),a.getContato(),true,true),a.getEmail(),a.getCpf())!=1)
                        throw new SQLException("Cadastro não encontrado.");
                    if(executarCadastro(c,"UPDATE Adolescente SET naturalidade=?,genero=?,cor_raca=?,observacoes=?,imm=?,vale_transporte=?,pia_enviado=?,medida_protetiva=? WHERE cpf_adolescente=?",
                            a.getNaturalidade(),a.getGenero(),a.getCorRaca(),a.getObservacoes(),a.isImm(),a.isValeTransporte(),a.isPiaEnviado(),a.isMedidaProtetiva(),a.getCpf())!=1)
                        throw new IllegalArgumentException("O cadastro do adolescente não existe mais. Recarregue a lista antes de editar.");
                    if (alterarSituacao)
                        executarCadastro(c,"UPDATE Adolescente SET status=? WHERE cpf_adolescente=?",a.getStatus().getCodigo(),a.getCpf());
                    executarCadastro(c,"UPDATE PIA SET documento_enviado=? WHERE cpf_adolescente=?",a.isPiaEnviado(),a.getCpf());
                } else {
                    executarCadastro(c,"INSERT INTO Pessoa(cpf,nome_completo,data_nascimento,contato,email) VALUES(?,?,?,?,?)",
                            a.getCpf(),a.getNomeCompleto(),a.getDataNascimento(),ContatoPessoaDAO.validar(c,a.getCpf(),a.getContato(),false,true),a.getEmail());
                    executarCadastro(c,"INSERT INTO Adolescente(cpf_adolescente,naturalidade,genero,cor_raca,status,observacoes,imm,vale_transporte,pia_enviado,medida_protetiva) VALUES(?,?,?,?,?,?,?,?,?,?)",
                            a.getCpf(),a.getNaturalidade(),a.getGenero(),a.getCorRaca(),StatusAdolescente.ATIVO.getCodigo(),a.getObservacoes(),a.isImm(),a.isValeTransporte(),a.isPiaEnviado(),a.isMedidaProtetiva());
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

    private void exigirEdicaoDePessoa(Connection c,long cpf) throws SQLException {
        if (pode(USUARIO_GERENCIAR)) return;
        try (PreparedStatement s=c.prepareStatement("SELECT 1 FROM EquipeTecnica WHERE cpf_equipe=?")) {
            s.setLong(1,cpf);
            try (ResultSet r=s.executeQuery()) {
                if (r.next()) throw new br.edu.unespar.trabalho.service.AcessoNegadoException();
            }
        }
    }

    private int executarCadastro(Connection c,String sql,Object... valores) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            return s.executeUpdate();
        }
    }

}
