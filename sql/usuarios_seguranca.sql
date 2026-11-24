-- Executar com a aplicação fechada e backup prévio. Não converte senhas.
-- Depois, executar AdministracaoInicial migrar-senhas no ambiente autorizado.
BEGIN;
LOCK TABLE EquipeTecnica IN ACCESS EXCLUSIVE MODE;
ALTER TABLE EquipeTecnica ADD COLUMN IF NOT EXISTS ativo boolean NOT NULL DEFAULT true;
ALTER TABLE EquipeTecnica ADD COLUMN IF NOT EXISTS versao_sessao bigint NOT NULL DEFAULT 0;
ALTER TABLE EquipeTecnica ADD COLUMN IF NOT EXISTS senha_formato varchar(15) NOT NULL DEFAULT 'LEGADO';
ALTER TABLE EquipeTecnica ALTER COLUMN senha_formato SET DEFAULT 'PBKDF2';
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM EquipeTecnica GROUP BY lower(btrim(login)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Existem logins equivalentes duplicados. Resolva antes de migrar.';
    END IF;
    IF EXISTS (SELECT 1 FROM EquipeTecnica WHERE btrim(login) = '' OR
               nivel_acesso NOT IN ('ADMINISTRADOR','EQUIPE_TECNICA','EDUCADOR')) THEN
        RAISE EXCEPTION 'Existem logins vazios ou perfis inválidos. Revise antes de migrar.';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uq_equipe_login_normalizado ON EquipeTecnica (lower(btrim(login)));
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'equipetecnica'::regclass AND conname = 'ck_equipe_perfil') THEN
        ALTER TABLE EquipeTecnica ADD CONSTRAINT ck_equipe_perfil
            CHECK (nivel_acesso IN ('ADMINISTRADOR','EQUIPE_TECNICA','EDUCADOR'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'equipetecnica'::regclass AND conname = 'ck_equipe_senha_formato') THEN
        ALTER TABLE EquipeTecnica ADD CONSTRAINT ck_equipe_senha_formato CHECK
            (senha_formato = 'LEGADO' OR (senha_formato = 'PBKDF2' AND senha LIKE 'pbkdf2-sha256$%'));
    END IF;
END $$;
COMMIT;
