CREATE TABLE Pessoa ( 
  cpf BIGINT NOT NULL, 
  nome_completo VARCHAR(80) NOT NULL, 
  data_nascimento DATE NOT NULL, 
  contato VARCHAR(15) NULL, 
  email VARCHAR(90) NULL, 
   
  CONSTRAINT pk_pessoa 
   PRIMARY KEY (cpf) 
); 
 
CREATE TABLE Responsavel ( 
  cpf_responsavel BIGINT NOT NULL, 
  parentesco VARCHAR(10) NOT NULL, 
  contato_principal BOOL NULL, 
   
  CONSTRAINT pk_responsavel 
   PRIMARY KEY (cpf_responsavel), 
   
  CONSTRAINT fk_responsavel 
   FOREIGN KEY (cpf_responsavel) 
   REFERENCES Pessoa (cpf) 


); 
 
CREATE TABLE Adolescente ( 
  cpf_adolescente BIGINT NOT NULL, 
  naturalidade VARCHAR(15) NOT NULL, 
  genero VARCHAR(15) NULL, 
  cor_raca VARCHAR(10) NOT NULL, 
  status VARCHAR(40) NOT NULL, 
   
  CONSTRAINT pk_adolescente 
   PRIMARY KEY (cpf_adolescente), 
   
  CONSTRAINT fk_adolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Pessoa (cpf) 
); 
 
CREATE TABLE EquipeTecnica ( 
  cpf_equipe BIGINT NOT NULL, 
  login VARCHAR(100) NOT NULL, 
  senha VARCHAR(200) NOT NULL, 
  cargo_funcao VARCHAR(60) NOT NULL, 
  nivel_acesso VARCHAR(15) NOT NULL, 
   
  CONSTRAINT pk_equipeTecnica 
   PRIMARY KEY (cpf_equipe), 
   
  CONSTRAINT fk_equipeTecnica 
   FOREIGN KEY (cpf_equipe) 
   REFERENCES Pessoa (cpf) 
); 
 
CREATE TABLE ComposicaoFamiliar ( 
  id_composicaoFamiliar INTEGER NOT NULL, 
  nome VARCHAR(80) NULL, 
  parentesco VARCHAR(80) NULL, 
  idade INTEGER NULL, 
  renda DECIMAL NULL, 
  escolaridade VARCHAR(10) NULL, 
  profissao VARCHAR(25) NULL, 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_composicaoFamiliar 
   PRIMARY KEY (id_composicaoFamiliar), 


   
  CONSTRAINT fk_compFamiliarAdolescente 
   FOREIGN KEY (cpf_adolescente) 
 REFERENCES Adolescente (cpf_adolescente) 
); 
 
CREATE TABLE SituacaoSocial ( 
  id_situacaoSocial INTEGER NOT NULL, 
  renda DECIMAL NOT NULL, 
  beneficios_sociais VARCHAR(256) NULL, 
  endereco VARCHAR(60) NOT NULL, 
  bairro VARCHAR(60) NOT NULL, 
  telefone VARCHAR(15) NOT NULL, 
  numero_nis INTEGER NOT NULL, 
  cras_referencia INTEGER NULL, 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_situacaoSocial 
   PRIMARY KEY (id_situacaoSocial), 
   
  CONSTRAINT fk_situacaoSocialAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente (cpf_adolescente), 
   
  CONSTRAINT unico_situacaoSocialAdolescente 
   UNIQUE (cpf_adolescente) 
); 
 
CREATE TABLE Saude ( 
  id_fichaSaude INTEGER NOT NULL, 
  ubs_referencia VARCHAR(25) NULL, 
  uso_spa BOOL NOT NULL, 
  observacoes VARCHAR(256) NULL, 
  substancias_utilizadas VARCHAR(90) NULL,  -- somente se uso_spa = true 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_saude 
   PRIMARY KEY (id_fichaSaude), 
   
  CONSTRAINT fk_saudeAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente(cpf_adolescente), 
   
  CONSTRAINT unico_saudeAdolescente 


   UNIQUE (cpf_adolescente) 
); 
 
CREATE TABLE EducacaoTrabalho ( 
  id_educacaoTrabalho INTEGER NOT NULL, 
  estuda BOOL NOT NULL, 
  escola VARCHAR(30) NULL,              -- somente se estuda = true 
  ano_serie VARCHAR(10) NULL,           -- somente se estuda = true 
  trabalha BOOL NOT NULL,               -- corrigido: campo portão não pode ser NULL 
  local_trabalho VARCHAR(80) NULL,      -- somente se trabalha = true 
  funcao VARCHAR(25) NULL,              -- somente se trabalha = true 
  vinculo_empregaticio VARCHAR(30) NULL,-- somente se trabalha = true 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_educacaoTrabalho 
   PRIMARY KEY (id_educacaoTrabalho), 
   
  CONSTRAINT fk_educTrabAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente (cpf_adolescente), 
   
  CONSTRAINT unico_educTrabAdolescente 
   UNIQUE (cpf_adolescente)  
); 
 
CREATE TABLE Responsabiliza ( 
  cpf_responsavel BIGINT NOT NULL, 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_responsabiliza 
   PRIMARY KEY (cpf_responsavel, cpf_adolescente), 
   
  CONSTRAINT fk_responsavelAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente (cpf_adolescente), 
   
  CONSTRAINT fk_responsabilizaResponsavel 
   FOREIGN KEY (cpf_responsavel) 
   REFERENCES Responsavel (cpf_responsavel) 
); 
 
CREATE TABLE Acompanhamento ( 
  cpf_adolescente BIGINT NOT NULL, 
  cpf_equipe BIGINT NOT NULL, 


  tecnico_referencia BOOL NULL, 
   
  CONSTRAINT pk_acompanhamento 
   PRIMARY KEY (cpf_adolescente, cpf_equipe), 
   
  CONSTRAINT fk_AcompanhamentoAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente (cpf_adolescente), 
   
  CONSTRAINT fk_EquipeAcompanhamento 
   FOREIGN KEY (cpf_equipe) 
   REFERENCES EquipeTecnica (cpf_equipe) 
); 
 
CREATE TABLE PIA ( 
  id_pia INTEGER NOT NULL, 
  data_elaboracao DATE NOT NULL, 
  diagnostico VARCHAR(100) NULL, 
  vulnerabilidades VARCHAR(125) NULL, 
  potencialidades VARCHAR(150) NULL, 
  estrategias VARCHAR(256) NULL, 
  documento_enviado BOOL NOT NULL, 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_pia 
   PRIMARY KEY (id_pia), 
   
  CONSTRAINT fk_AdolescentePIA 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente (cpf_adolescente) 
); 
 
CREATE TABLE ElaborarPIA ( 
  cpf_equipe BIGINT NOT NULL, 
  id_pia INTEGER NOT NULL,   
   
  CONSTRAINT pk_elaborarPIA 
   PRIMARY KEY (id_pia, cpf_equipe), 
   
  CONSTRAINT fk_elaboarPiaEquipe 
   FOREIGN KEY (cpf_equipe) 
   REFERENCES EquipeTecnica (cpf_equipe), 
 
  CONSTRAINT fk_elaborarPia_Pia 


        FOREIGN KEY (id_pia) 
        REFERENCES PIA (id_pia) 
); 
 
CREATE TABLE MedidaSocioeducativa ( 
  id_medida INTEGER NOT NULL, 
  reincidencia BOOL NOT NULL, 
  tipo_medida VARCHAR(90) NOT NULL, 
  data_inicio DATE NOT NULL, 
  historico_infracional VARCHAR(256) NULL, 
  duracao_meses INTEGER NULL,   -- somente se tipo_medida = 'LA' 
  duracao_horas INTEGER NULL,   -- somente se tipo_medida = 'PSC' 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_medidaSocioEducativa 
   PRIMARY KEY (id_medida), 
   
  CONSTRAINT fk_medidaAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente(cpf_adolescente) 
); 
 
CREATE TABLE Documento ( 
  id_documento INTEGER NOT NULL, 
  tipo VARCHAR(15) NULL, 
  data_geracao DATE NOT NULL, 
  status_envio VARCHAR(15) NULL, 
  cpf_adolescente BIGINT NOT NULL, 
   
  CONSTRAINT pk_documento 
   PRIMARY KEY (id_documento), 
   
  CONSTRAINT fk_documentoAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente (cpf_adolescente) 
); 
 
CREATE TABLE EmiteDocumento ( 
  id_documento INTEGER NOT NULL, 
  cpf_equipe BIGINT NOT NULL, 
   
  CONSTRAINT pk_emiteDocumento 
   PRIMARY KEY (cpf_equipe, id_documento), 
   


  CONSTRAINT fk_emiteDocEquipe 
   FOREIGN KEY (cpf_equipe) 
   REFERENCES EquipeTecnica (cpf_equipe), 
   
  CONSTRAINT fk_emiteDocumento 
   FOREIGN KEY (id_documento) 
   REFERENCES Documento (id_documento) 
); 
 
CREATE TABLE Atividade ( 
  id_atividade INTEGER NOT NULL, 
  nome_atividade VARCHAR(25) NOT NULL, 
  tipo VARCHAR(120) NULL, 
  carga_horaria INTEGER NOT NULL, 
   
  CONSTRAINT pk_atividade 
   PRIMARY KEY (id_atividade) 
); 
 
CREATE TABLE Frequencia ( 
  cpf_adolescente BIGINT NOT NULL, 
  id_atividade INTEGER NOT NULL, 
  data_presenca DATE NOT NULL, 
  status_presenca VARCHAR(10) NOT NULL, 
  horas_cumpridas INTEGER NULL, 
   
  CONSTRAINT pk_frequencia 
   PRIMARY KEY (cpf_adolescente, id_atividade, data_presenca), 
   
  CONSTRAINT fk_frequenciaAdolescente 
   FOREIGN KEY (cpf_adolescente) 
   REFERENCES Adolescente(cpf_adolescente), 
   
  CONSTRAINT fk_frequenciaAtividade 
   FOREIGN KEY (id_atividade) 
   REFERENCES Atividade(id_atividade) 
); 
 
