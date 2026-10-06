-- PostgreSQL. Executar uma vez antes de usar as telas atualizadas.
-- Não apaga tabelas nem registros existentes. Reexecução permitida.
BEGIN;
ALTER TABLE Pessoa ALTER COLUMN contato TYPE varchar(30);
ALTER TABLE Adolescente ALTER COLUMN naturalidade TYPE varchar(120);
ALTER TABLE Adolescente ALTER COLUMN cor_raca TYPE varchar(30);
ALTER TABLE Adolescente ADD COLUMN IF NOT EXISTS observacoes text NOT NULL DEFAULT '';
ALTER TABLE Adolescente ADD COLUMN IF NOT EXISTS imm boolean NOT NULL DEFAULT false;
ALTER TABLE Adolescente ADD COLUMN IF NOT EXISTS vale_transporte boolean NOT NULL DEFAULT false;
ALTER TABLE Adolescente ADD COLUMN IF NOT EXISTS medida_protetiva boolean NOT NULL DEFAULT false;
-- Confirma envio externo sem exigir elaboração do PIA no sistema.
-- Inicializa apenas valores ainda não definidos, preservando confirmações em reexecuções.
ALTER TABLE Adolescente ADD COLUMN IF NOT EXISTS pia_enviado boolean;
UPDATE Adolescente a SET pia_enviado=EXISTS (
    SELECT 1 FROM PIA p WHERE p.cpf_adolescente=a.cpf_adolescente AND p.documento_enviado=true
) WHERE a.pia_enviado IS NULL;
ALTER TABLE Adolescente ALTER COLUMN pia_enviado SET DEFAULT false;
ALTER TABLE Adolescente ALTER COLUMN pia_enviado SET NOT NULL;
ALTER TABLE SituacaoSocial ALTER COLUMN numero_nis TYPE bigint;
ALTER TABLE SituacaoSocial ALTER COLUMN telefone TYPE varchar(30);
ALTER TABLE SituacaoSocial ADD COLUMN IF NOT EXISTS cras_nome varchar(120) NOT NULL DEFAULT '';
UPDATE SituacaoSocial SET cras_nome=cras_referencia::text WHERE cras_nome='' AND cras_referencia IS NOT NULL;
ALTER TABLE Saude ALTER COLUMN ubs_referencia TYPE varchar(120);
ALTER TABLE EducacaoTrabalho ALTER COLUMN escola TYPE varchar(120);
ALTER TABLE EducacaoTrabalho ALTER COLUMN ano_serie TYPE varchar(40);
ALTER TABLE EducacaoTrabalho ALTER COLUMN funcao TYPE varchar(120);
ALTER TABLE EducacaoTrabalho ALTER COLUMN vinculo_empregaticio TYPE varchar(80);
ALTER TABLE Responsavel ALTER COLUMN parentesco TYPE varchar(80);
ALTER TABLE Responsabiliza ADD COLUMN IF NOT EXISTS parentesco_vinculo varchar(80);
ALTER TABLE Responsabiliza ADD COLUMN IF NOT EXISTS principal boolean;
UPDATE Responsabiliza v SET parentesco_vinculo=r.parentesco,principal=COALESCE(r.contato_principal,false)
 FROM Responsavel r WHERE r.cpf_responsavel=v.cpf_responsavel AND v.parentesco_vinculo IS NULL;
ALTER TABLE ComposicaoFamiliar ALTER COLUMN escolaridade TYPE varchar(80);
ALTER TABLE ComposicaoFamiliar ALTER COLUMN profissao TYPE varchar(120);
ALTER TABLE MedidaSocioeducativa ADD COLUMN IF NOT EXISTS data_fim date;
ALTER TABLE Frequencia ADD COLUMN IF NOT EXISTS id_medida integer REFERENCES MedidaSocioeducativa(id_medida);
ALTER TABLE Frequencia ADD COLUMN IF NOT EXISTS observacoes text NOT NULL DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_frequencia_medida_data ON Frequencia(id_medida,data_presenca);
-- Só atribui horas antigas quando existe uma única PSC compatível com a data.
-- Ambiguidades permanecem sem vínculo, visíveis para revisão pela interface.
UPDATE Frequencia f SET id_medida=m.id_medida FROM MedidaSocioeducativa m
 WHERE f.id_medida IS NULL AND m.cpf_adolescente=f.cpf_adolescente
 AND upper(m.tipo_medida)='PSC' AND f.data_presenca>=m.data_inicio
 AND (m.data_fim IS NULL OR f.data_presenca<=m.data_fim)
 AND (SELECT count(*) FROM MedidaSocioeducativa x WHERE x.cpf_adolescente=f.cpf_adolescente
      AND upper(x.tipo_medida)='PSC' AND f.data_presenca>=x.data_inicio
      AND (x.data_fim IS NULL OR f.data_presenca<=x.data_fim))=1;
COMMIT;
