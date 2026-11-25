# Carga fictícia de demonstração

`sql/popular_banco_apresentacao.sql` substitui dados e só pode rodar em uma base
PostgreSQL descartável chamada exatamente `casa_apresentacao`. Nunca use a base
operacional, um banco com dados reais ou a base pessoal de outro desenvolvedor.

1. Crie uma base nova com esse nome em um servidor local de testes, depois de
   confirmar que ela não existe. A criação da base e do esquema exige uma conta
   administrativa local. Não reutilize uma base existente.
2. Aplique `src/test/resources/schema_original.sql` e, nesta ordem,
   `sql/ajustes_cadastro_frequencia.sql`, `sql/agenda.sql`,
   `sql/relatorios_acompanhamento.sql` e `sql/usuarios_seguranca.sql`.
3. Crie o marcador **somente nessa base**:

   ```sql
   CREATE TABLE public.casa_autorizacao_demonstracao (
       finalidade text PRIMARY KEY
           CHECK (finalidade = 'DADOS_FICTICIOS_DESCARTAVEIS')
   );
   INSERT INTO public.casa_autorizacao_demonstracao (finalidade)
   VALUES ('DADOS_FICTICIOS_DESCARTAVEIS');
   ```

4. Confirme host, porta, usuário e destino com `SELECT current_database();` e
   execute o arquivo inteiro, sem extrair comandos. No psql, informe a conexão
   explicitamente:

   ```powershell
   psql -X -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U conta_teste -d casa_apresentacao -f sql/popular_banco_apresentacao.sql
   ```

   No pgAdmin, selecione essa base na Query Tool e execute o arquivo inteiro
   como uma única sessão.

O script inicia `BEGIN`, fixa `search_path` em `public`, confere nome e marcador
antes de qualquer exclusão e termina com `COMMIT`. Uma falha deixa a transação
abortada: mesmo um cliente que continue enviando instruções não executa as
exclusões seguintes. Erro em qualquer inserção também desfaz toda a carga.
O marcador não substitui a conferência humana do servidor e da base.

As credenciais ficam fora do Git. Use `PGPASSWORD` somente em processo local
temporário ou um arquivo `pgpass` protegido. Não registre senhas no terminal,
em capturas ou na documentação.

## Ensaio em PostgreSQL 18 local e isolado (25/11/2026)

- `casa_proibida_teste`: base nova, esquema completo e uma pessoa fictícia
  sentinela. O arquivo **inteiro** foi enviado por `psql -v ON_ERROR_STOP=0`.
  A validação recusou o destino; o cliente continuou até `ROLLBACK`.
  Dumps SQL do esquema e dos dados, normalizados apenas para remover o token
  aleatório `\\restrict`/`\\unrestrict` do pg_dump 18, ficaram idênticos nas
  908 linhas. A sentinela permaneceu.
- `casa_apresentacao`: base nova com marcador. O mesmo arquivo terminou em
  `COMMIT`, com 10 adolescentes, 10 responsáveis e 5 frequências. As 147
  restrições do esquema continuaram presentes.
- Uma falha forçada por trigger na inserção de `Saude`, após o `TRUNCATE`,
  terminou em `ROLLBACK`; os dumps anterior e posterior de esquema e dados
  ficaram idênticos nas 1.061 linhas normalizadas. O trigger de ensaio foi
  removido da base descartável.

Este ensaio comprova os fluxos executados no cliente psql. A execução manual
no pgAdmin ainda precisa ser conferida no ambiente em que ele será usado.
