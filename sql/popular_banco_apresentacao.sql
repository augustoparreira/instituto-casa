-- PostgreSQL: base ficticia para apresentacao do Instituto C.A.S.A.
-- Executar em db_instituto_casa com os ajustes de cadastro/frequencia ja aplicados.
-- ATENCAO: a limpeza remove todos os adolescentes, responsaveis, atividades e
-- historicos dependentes, incluindo frequencias, medidas, PIA, agenda e relatorios.
-- Faca backup antes. Para execucao atomica, use a opcao de transacao da ferramenta SQL.
-- Pessoa e EquipeTecnica dos usuarios existentes sao preservadas para manter o login.
-- Nao altera tabelas, colunas, constraints ou sequencias. Usa apenas TRUNCATE, DELETE e INSERT.
-- Todos os nomes, CPFs, NIS, telefones, enderecos e instituicoes abaixo sao ficticios.
-- CPFs tem digitos verificadores validos para demonstracao, sem vinculo com pessoas reais.
-- O banco armazena CPF como BIGINT; a pontuacao e aplicada pela interface.
-- Reexecutar este arquivo limpa novamente a base e recria os mesmos cadastros.

TRUNCATE TABLE Adolescente, Responsavel, Atividade CASCADE;

DELETE FROM Pessoa
WHERE NOT EXISTS (
    SELECT 1 FROM EquipeTecnica WHERE EquipeTecnica.cpf_equipe = Pessoa.cpf
);

INSERT INTO Pessoa (cpf, nome_completo, data_nascimento, contato, email) VALUES
    -- CPF 900.000.101-38 - adolescente
    (90000010138, 'Lucas Henrique da Silva', '2009-03-14', '(43) 90000-0101', 'adolescente01@example.com'),
    -- CPF 900.000.102-19 - adolescente
    (90000010219, 'Gabriel Santos Oliveira', '2008-07-22', '(43) 90000-0102', 'adolescente02@example.com'),
    -- CPF 900.000.103-08 - adolescente
    (90000010308, 'Ana Clara Rodrigues', '2010-02-09', '(43) 90000-0103', 'adolescente03@example.com'),
    -- CPF 900.000.104-80 - adolescente
    (90000010480, 'Pedro Augusto Ferreira', '2007-11-18', '(43) 90000-0104', 'adolescente04@example.com'),
    -- CPF 900.000.105-61 - adolescente
    (90000010561, 'Julia Beatriz Costa', '2011-05-30', '(43) 90000-0105', 'adolescente05@example.com'),
    -- CPF 900.000.106-42 - adolescente
    (90000010642, 'Matheus Vinicius Almeida', '2006-12-12', '(43) 90000-0106', 'adolescente06@example.com'),
    -- CPF 900.000.107-23 - adolescente
    (90000010723, 'Rafael Moreira Souza', '2009-09-05', '(43) 90000-0107', 'adolescente07@example.com'),
    -- CPF 900.000.108-04 - adolescente
    (90000010804, 'Isabela Cristina Pereira', '2012-01-26', '(43) 90000-0108', 'adolescente08@example.com'),
    -- CPF 900.000.109-95 - adolescente
    (90000010995, 'Gustavo Lima Martins', '2008-04-17', '(43) 90000-0109', 'adolescente09@example.com'),
    -- CPF 900.000.110-29 - adolescente
    (90000011029, 'Beatriz Fernandes Rocha', '2005-12-03', '(43) 90000-0110', 'adolescente10@example.com'),
    -- CPF 900.000.201-09 - responsavel
    (90000020109, 'Mariana Aparecida da Silva', '1987-06-22', '(43) 90000-0201', 'responsavel01@example.com'),
    -- CPF 900.000.202-81 - responsavel
    (90000020281, 'Roberto Carlos Oliveira', '1984-11-10', '(43) 90000-0202', 'responsavel02@example.com'),
    -- CPF 900.000.203-62 - responsavel
    (90000020362, 'Patricia Rodrigues', '1989-04-18', '(43) 90000-0203', 'responsavel03@example.com'),
    -- CPF 900.000.204-43 - responsavel
    (90000020443, 'Sandra Regina Ferreira', '1982-08-05', '(43) 90000-0204', 'responsavel04@example.com'),
    -- CPF 900.000.205-24 - responsavel
    (90000020524, 'Renata Beatriz Costa', '1990-12-12', '(43) 90000-0205', 'responsavel05@example.com'),
    -- CPF 900.000.206-05 - responsavel
    (90000020605, 'Joao Batista Almeida', '1979-03-16', '(43) 90000-0206', 'responsavel06@example.com'),
    -- CPF 900.000.207-96 - responsavel
    (90000020796, 'Eliane Moreira Souza', '1986-05-20', '(43) 90000-0207', 'responsavel07@example.com'),
    -- CPF 900.000.208-77 - responsavel
    (90000020877, 'Adriana Cristina Pereira', '1991-09-14', '(43) 90000-0208', 'responsavel08@example.com'),
    -- CPF 900.000.209-58 - responsavel
    (90000020958, 'Sueli Lima Martins', '1985-02-27', '(43) 90000-0209', 'responsavel09@example.com'),
    -- CPF 900.000.210-91 - responsavel
    (90000021091, 'Paulo Fernandes Rocha', '1977-07-08', '(43) 90000-0210', 'responsavel10@example.com');

INSERT INTO Adolescente (cpf_adolescente, naturalidade, genero, cor_raca, status, observacoes, imm, vale_transporte, pia_enviado, medida_protetiva) VALUES
    (90000010138, 'Apucarana', 'Masculino', 'Parda', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, FALSE, FALSE, FALSE),
    (90000010219, 'Arapongas', 'Masculino', 'Branca', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', TRUE, TRUE, FALSE, FALSE),
    (90000010308, 'Apucarana', 'Feminino', 'Parda', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, TRUE, FALSE, TRUE),
    (90000010480, 'Londrina', 'Masculino', 'Preta', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', TRUE, TRUE, FALSE, FALSE),
    (90000010561, 'Apucarana', 'Feminino', 'Branca', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, FALSE, FALSE, FALSE),
    (90000010642, 'Cambira', 'Masculino', 'Parda', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, TRUE, FALSE, FALSE),
    (90000010723, 'Apucarana', 'Masculino', 'Branca', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', TRUE, TRUE, FALSE, FALSE),
    (90000010804, 'Arapongas', 'Feminino', 'Parda', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, TRUE, FALSE, TRUE),
    (90000010995, 'Apucarana', 'Masculino', 'Preta', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, TRUE, FALSE, FALSE),
    (90000011029, 'Londrina', 'Feminino', 'Branca', 'ATIVO', 'Comparecimento as tercas e quintas-feiras.', FALSE, TRUE, FALSE, FALSE);

INSERT INTO Responsavel (cpf_responsavel, parentesco, contato_principal) VALUES
    (90000020109, 'Mae', TRUE),
    (90000020281, 'Pai', TRUE),
    (90000020362, 'Mae', TRUE),
    (90000020443, 'Mae', TRUE),
    (90000020524, 'Mae', TRUE),
    (90000020605, 'Pai', TRUE),
    (90000020796, 'Mae', TRUE),
    (90000020877, 'Mae', TRUE),
    (90000020958, 'Mae', TRUE),
    (90000021091, 'Pai', TRUE);

INSERT INTO Responsabiliza (cpf_responsavel, cpf_adolescente, parentesco_vinculo, principal) VALUES
    (90000020109, 90000010138, 'Mae', TRUE),
    (90000020281, 90000010219, 'Pai', TRUE),
    (90000020362, 90000010308, 'Mae', TRUE),
    (90000020443, 90000010480, 'Mae', TRUE),
    (90000020524, 90000010561, 'Mae', TRUE),
    (90000020605, 90000010642, 'Pai', TRUE),
    (90000020796, 90000010723, 'Mae', TRUE),
    (90000020877, 90000010804, 'Mae', TRUE),
    (90000020958, 90000010995, 'Mae', TRUE),
    (90000021091, 90000011029, 'Pai', TRUE);

-- Renda familiar em salarios minimos, conforme o cadastro; NIS demonstrativo com 11 digitos.
INSERT INTO SituacaoSocial (id_situacaoSocial, renda, beneficios_sociais, endereco, bairro, telefone, numero_nis, cras_referencia, cras_nome, cpf_adolescente) VALUES
    (1, 1.5, 'Bolsa Familia', 'Rua das Hortensias, 120', 'Jardim das Flores', '(43) 90000-0101', 90000000101, NULL, 'CRAS Jardim Esperanca', 90000010138),
    (2, 2.0, 'Nao recebe', 'Rua dos Ipes, 137', 'Vila Esperanca', '(43) 90000-0102', 90000000102, NULL, 'CRAS Vila Nova', 90000010219),
    (3, 1.2, 'Bolsa Familia', 'Rua das Margaridas, 154', 'Jardim America', '(43) 90000-0103', 90000000103, NULL, 'CRAS Jardim Esperanca', 90000010308),
    (4, 1.0, 'Bolsa Familia', 'Rua das Acacias, 171', 'Parque das Acacias', '(43) 90000-0104', 90000000104, NULL, 'CRAS Vila Nova', 90000010480),
    (5, 2.5, 'Nao recebe', 'Rua dos Girassois, 188', 'Vila Nova', '(43) 90000-0105', 90000000105, NULL, 'CRAS Jardim Esperanca', 90000010561),
    (6, 1.8, 'Nao recebe', 'Rua das Violetas, 205', 'Jardim Primavera', '(43) 90000-0106', 90000000106, NULL, 'CRAS Vila Nova', 90000010642),
    (7, 1.3, 'Bolsa Familia', 'Rua dos Lirios, 222', 'Jardim das Palmeiras', '(43) 90000-0107', 90000000107, NULL, 'CRAS Jardim Esperanca', 90000010723),
    (8, 1.0, 'Bolsa Familia', 'Rua das Rosas, 239', 'Vila Sao Jose', '(43) 90000-0108', 90000000108, NULL, 'CRAS Vila Nova', 90000010804),
    (9, 2.2, 'Nao recebe', 'Rua das Camelias, 256', 'Jardim Alvorada', '(43) 90000-0109', 90000000109, NULL, 'CRAS Jardim Esperanca', 90000010995),
    (10, 1.7, 'Nao recebe', 'Rua das Orquideas, 273', 'Jardim Horizonte', '(43) 90000-0110', 90000000110, NULL, 'CRAS Vila Nova', 90000011029);

INSERT INTO Saude (id_fichaSaude, ubs_referencia, uso_spa, observacoes, substancias_utilizadas, cpf_adolescente) VALUES
    (1, 'UBS Jardim das Flores', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010138),
    (2, 'UBS Vila Nova', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010219),
    (3, 'UBS Jardim das Flores', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010308),
    (4, 'UBS Vila Nova', TRUE, 'Acompanhamento periodico na unidade de referencia.', 'Tabaco', 90000010480),
    (5, 'UBS Jardim das Flores', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010561),
    (6, 'UBS Vila Nova', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010642),
    (7, 'UBS Jardim das Flores', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010723),
    (8, 'UBS Vila Nova', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000010804),
    (9, 'UBS Jardim das Flores', TRUE, 'Acompanhamento periodico na unidade de referencia.', 'Tabaco', 90000010995),
    (10, 'UBS Vila Nova', FALSE, 'Acompanhamento periodico na unidade de referencia.', NULL, 90000011029);

INSERT INTO EducacaoTrabalho (id_educacaoTrabalho, estuda, escola, ano_serie, trabalha, local_trabalho, funcao, vinculo_empregaticio, cpf_adolescente) VALUES
    (1, TRUE, 'Colegio Estadual Jardim das Flores', '2o ano do Ensino Medio', FALSE, NULL, NULL, NULL, 90000010138),
    (2, TRUE, 'Colegio Estadual Vila Nova', '3o ano do Ensino Medio', TRUE, 'Mercado Primavera', 'Auxiliar administrativo', 'Jovem aprendiz', 90000010219),
    (3, TRUE, 'Colegio Estadual Parque das Acacias', '1o ano do Ensino Medio', FALSE, NULL, NULL, NULL, 90000010308),
    (4, TRUE, 'Colegio Estadual Jardim das Flores', '3o ano do Ensino Medio', TRUE, 'Mercado Primavera', 'Auxiliar administrativo', 'Jovem aprendiz', 90000010480),
    (5, TRUE, 'Colegio Estadual Vila Nova', '9o ano do Ensino Fundamental', FALSE, NULL, NULL, NULL, 90000010561),
    (6, FALSE, NULL, NULL, TRUE, 'Mercado Primavera', 'Atendente', 'CLT', 90000010642),
    (7, TRUE, 'Colegio Estadual Jardim das Flores', '2o ano do Ensino Medio', FALSE, NULL, NULL, NULL, 90000010723),
    (8, TRUE, 'Colegio Estadual Vila Nova', '8o ano do Ensino Fundamental', FALSE, NULL, NULL, NULL, 90000010804),
    (9, TRUE, 'Colegio Estadual Parque das Acacias', '3o ano do Ensino Medio', FALSE, NULL, NULL, NULL, 90000010995),
    (10, FALSE, NULL, NULL, TRUE, 'Mercado Primavera', 'Atendente', 'CLT', 90000011029);

INSERT INTO ComposicaoFamiliar (id_composicaoFamiliar, nome, parentesco, idade, renda, escolaridade, profissao, cpf_adolescente) VALUES
    (1, 'Mariana Aparecida da Silva', 'Mae', 39, 1.5, 'Ensino Medio completo', 'Auxiliar de servicos gerais', 90000010138),
    (2, 'Roberto Carlos Oliveira', 'Pai', 41, 2.0, 'Ensino Medio completo', 'Atendente', 90000010219),
    (3, 'Patricia Rodrigues', 'Mae', 37, 1.2, 'Ensino Medio completo', 'Auxiliar de servicos gerais', 90000010308),
    (4, 'Sandra Regina Ferreira', 'Mae', 44, 1.0, 'Ensino Medio completo', 'Atendente', 90000010480),
    (5, 'Renata Beatriz Costa', 'Mae', 35, 2.5, 'Ensino Medio completo', 'Auxiliar de servicos gerais', 90000010561),
    (6, 'Joao Batista Almeida', 'Pai', 47, 1.8, 'Ensino Medio completo', 'Atendente', 90000010642),
    (7, 'Eliane Moreira Souza', 'Mae', 40, 1.3, 'Ensino Medio completo', 'Auxiliar de servicos gerais', 90000010723),
    (8, 'Adriana Cristina Pereira', 'Mae', 35, 1.0, 'Ensino Medio completo', 'Atendente', 90000010804),
    (9, 'Sueli Lima Martins', 'Mae', 41, 2.2, 'Ensino Medio completo', 'Auxiliar de servicos gerais', 90000010995),
    (10, 'Paulo Fernandes Rocha', 'Pai', 49, 1.7, 'Ensino Medio completo', 'Atendente', 90000011029);

-- Medidas comecam no primeiro dia do mes atual. LA usa meses; PSC usa horas.
-- Ana Clara e Gustavo tem LA e PSC simultaneas, em registros independentes.
INSERT INTO MedidaSocioeducativa (id_medida, reincidencia, tipo_medida, data_inicio, historico_infracional, duracao_meses, duracao_horas, data_fim, cpf_adolescente) VALUES
    (1, FALSE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 80, NULL, 90000010138),
    (2, FALSE, 'LA', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', 6, NULL, NULL, 90000010219),
    (3, FALSE, 'LA', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', 6, NULL, NULL, 90000010308),
    (4, TRUE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 80, NULL, 90000010480),
    (5, FALSE, 'LA', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', 6, NULL, NULL, 90000010561),
    (6, TRUE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 80, NULL, 90000010642),
    (7, FALSE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 80, NULL, 90000010723),
    (8, FALSE, 'LA', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', 6, NULL, NULL, 90000010804),
    (9, FALSE, 'LA', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', 6, NULL, NULL, 90000010995),
    (10, FALSE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 80, NULL, 90000011029),
    (11, FALSE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 48, NULL, 90000010308),
    (12, FALSE, 'PSC', date_trunc('month', CURRENT_DATE)::date, 'Registro ficticio para demonstracao.', NULL, 64, NULL, 90000010995);

INSERT INTO Atividade (id_atividade, nome_atividade, tipo, carga_horaria) VALUES
    (1, 'Oficina de informatica', 'Oficina socioeducativa', 4),
    (2, 'Oficina de jardinagem', 'Prestacao de servicos', 4),
    (3, 'Roda de conversa', 'Acompanhamento socioeducativo', 2),
    (4, 'Oficina de artes', 'Oficina socioeducativa', 4);

-- Frequencias e confirmacoes de envio de PIA comecam vazias para serem demonstradas pela interface.
