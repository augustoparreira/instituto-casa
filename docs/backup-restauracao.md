# Backup e restauração técnica do PostgreSQL

Estes scripts usam `pg_dump`, `pg_restore` e `psql` oficiais. Exigem ferramentas
PostgreSQL no `PATH`, acesso ao servidor e uma conta apropriada. Informe sempre
host, porta, banco e usuário; não há destino padrão. Configure autenticação
fora do Git, preferencialmente com `PGPASSFILE` protegido ou prompt local. Não
passe senhas como argumentos nem as inclua em logs.

## Gerar e verificar

Crie previamente um diretório com acesso restrito, **fora do repositório**.
Exemplo com nomes fictícios:

```powershell
./scripts/backup-postgresql.ps1 -HostBanco 127.0.0.1 -Porta 5432 `
  -Banco casa_teste_exemplo -Usuario operador_backup -Diretorio C:\BackupsCasa
```

O script exige o diretório existente, imprime versões das ferramentas, grava
primeiro `*.backup.partial` com nome único, exige código de saída zero do
`pg_dump`, tamanho não nulo e catálogo legível por `pg_restore --list`. Só então
move o arquivo para `*.backup` e informa o SHA-256. Um parcial após falha **não
é backup válido**; investigue antes de removê-lo. Backups anteriores nunca são
substituídos nem apagados automaticamente.

Informe em `-Banco` um nome literal de até 63 caracteres, começando com letra
ASCII ou sublinhado. Também são aceitos dígitos e hífens nas posições
seguintes. URIs e strings de conexão são recusadas. Os scripts usam o
host, a porta, o usuário e o banco indicados na chamada. As variáveis
`PGHOSTADDR` e `PGSERVICE` são ignoradas durante a operação.

O catálogo legível prova apenas que o arquivo é reconhecido. Confirme a
recuperação por restauração periódica em uma base nova.

## Restaurar somente em base descartável

Confirme o servidor de testes e crie manualmente uma base **nova e vazia** com
nome `casa_restore_*`. Não use banco operacional, principal ou de outro
desenvolvedor. Exemplo:

```powershell
./scripts/restaurar-postgresql-teste.ps1 -HostBanco 127.0.0.1 -Porta 5432 `
  -Banco casa_restore_ensaio -Usuario operador_teste `
  -Arquivo C:\BackupsCasa\casa-exemplo.backup -ConfirmarBancoDescartavel
```

O script exige confirmação explícita, padrão de nome, arquivo não vazio e
catálogo reconhecido. Confere o nome da base, a ausência de objetos grandes
e de configurações de papéis específicas da base, além do inventário de
esquema gerado pelo `pg_dump`. Recusa destinos com objetos
de usuário, incluindo esquemas, funções, procedimentos, tipos, tabelas, visões,
sequências e extensões. Se a conferência falhar, a restauração é recusada.
Use um endpoint direto em um servidor isolado e mantenha a base exclusiva
para o ensaio, sem alterações concorrentes entre a conferência e a restauração.

Restaure apenas backups de origem confiável. O `pg_restore` pode executar
instruções contidas no arquivo. Um catálogo legível e o SHA-256 informado
pelo script não comprovam a procedência do conteúdo.

A restauração usa `--exit-on-error --single-transaction --no-owner --no-privileges`,
sem `--clean` ou `DROP DATABASE`. Depois, compare dados, relacionamentos,
restrições e sequências com a origem. A conta usada precisa de privilégios
para criar os objetos na base descartável. Um erro de restauração impede a
mensagem de sucesso.

## Ensaio local com dados fictícios (25/11/2026)

No cluster PostgreSQL 18.6 isolado da porta 55493, a origem descartável
`casa_apresentacao` foi copiada para a base nova `casa_restore_validacao_251125`.
O backup customizado passou por tamanho, catálogo e SHA-256. O arquivo foi
restaurado em transação. Os dumps de dados da origem e destino ficaram
idênticos (311 linhas normalizadas apenas para remover o token aleatório do
pg_dump). Ambos tinham 20 tabelas, 2 sequências e 147 restrições. O destino
continha 10 adolescentes, 10 responsáveis, 12 medidas e 5 frequências.

Foram recusados: diretório inexistente, origem inexistente, destino fora do
padrão `casa_restore_*`, destino não vazio e arquivo inválido. A tentativa
com origem inexistente não criou arquivo `*.backup` válido. Testes de servidor
indisponível, falha real no meio do `pg_restore`, diretório sem permissão,
restauração em outro Windows e login no banco restaurado ainda estão pendentes.

O dump preserva os dados e o esquema do banco e pode conter dados sigilosos
e hashes das senhas das contas **da aplicação**, armazenados nas tabelas.
Preserve separadamente a configuração do PostgreSQL e do JDBC, a versão do
aplicativo e seus arquivos externos. Papéis globais do PostgreSQL e a
autenticação do servidor também exigem procedimento separado. O dump pode
registrar proprietários e concessões dos objetos, mas o script de teste usa
`--no-owner --no-privileges` para ignorá-los. Em uma recuperação definitiva,
reconfigure e valide os papéis, proprietários e privilégios antes do uso.
O `.gitignore` reduz o risco de adicionar dumps e chaves
ao Git, mas não protege arquivos já versionados. Backups com dados reais exigem
acesso restrito e criptografia mantida por ferramenta apropriada; o dump puro
do ensaio não é modelo para mídia externa. Não há ainda decisão sobre HD,
agendamento, nuvem, retenção, responsabilidade operacional ou implantação.
