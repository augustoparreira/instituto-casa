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

O arquivo é uma única instrução `DO` no servidor. O bloco fixa o
`search_path`, confere o nome e o marcador antes de qualquer exclusão e
executa toda a carga na mesma unidade atômica. Uma falha na validação ou em uma
inserção reverte o bloco inteiro, mesmo que o cliente continue e use pontos
de salvamento automáticos.
O marcador não substitui a conferência humana do servidor e da base.

As credenciais ficam fora do Git. Use `PGPASSWORD` somente em processo local
temporário ou um arquivo `pgpass` protegido. Não registre senhas no terminal,
em capturas ou na documentação.

## Linha experimental de permissões

Use a nova branch de correções para demonstrar a proposta com dados fictícios.
A versão publicada de integração e a linha exclusiva de usabilidade mantêm
a matriz anterior. A proposta experimental ainda depende de aprovação da ONG.

Use contas de teste dos três perfis. O Administrador abre a administração de
contas. A Equipe Técnica abre o cadastro completo, as fichas protegidas, a
confirmação do PIA e os Relatórios. O Educador continua com o registro geral
de frequência, sem vincular medida ou creditar horas. Para testar um CPF
compartilhado, prepare um registro legado só no ambiente descartável e
verifique a edição dos dados exclusivos do adolescente.

## Revalidação na versão integrada (27/11/2026)

Em PostgreSQL 18.6 descartável, o arquivo inteiro completou a carga na base
marcada e recusou tanto uma base com nome proibido quanto a base correta
sem marcador. As recusas e uma falha forçada no meio da carga preservaram
dados e esquema com `ON_ERROR_STOP=0` e `ON_ERROR_ROLLBACK` ligado ou
desligado. O cliente executou uma consulta posterior em todos esses cenários.

## Ensaio anterior à correção (25/11/2026)

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

Esse ensaio anterior não cobria a continuação após erro com pontos de salvamento
automáticos. Os resultados da correção estão no [registro de validação](registro-validacao-entrega.md).
O backend da Query Tool do pgAdmin aceitou a base marcada e recusou a proibida
em perfil descartável. A execução pela interface gráfica ainda precisa ser
conferida.
