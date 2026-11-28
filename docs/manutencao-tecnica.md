# Guia técnico de manutenção

## Projeto e versão

Aplicação Java 21 com JavaFX 21.0.2, JDBC PostgreSQL 42.7.2, FXML e CSS.
`pom.xml` declara dependências; `src/main/java` contém controladores, serviços,
DAOs e modelos; `src/main/resources/View` contém telas, estilos e imagem;
`sql` contém migrações e carga fictícia; `scripts` contém verificações,
distribuição e backup. Registre o hash Git e a versão das ferramentas junto
com qualquer distribuição entregue. Os ajustes finais de usabilidade partem
de `add761c92e5e39d4a7e6cefb3dad673cb561a311` na `feature/integracao-final`.
O código integrado dessa base foi validado em
`8fea8872654a41b4f4a657e0a4403c90f990e651`.

## Preparar e atualizar uma base

Use um servidor e banco explicitamente identificados. Antes de qualquer
migração, confirme o destino, interrompa as sessões do aplicativo e produza
um backup recuperável. Para uma base nova, aplique o esquema base e, em ordem,
`sql/ajustes_cadastro_frequencia.sql`, `sql/agenda.sql`,
`sql/relatorios_acompanhamento.sql` e `sql/usuarios_seguranca.sql`. O esquema
base utilizado nos ensaios está em `src/test/resources/schema_original.sql`;
um técnico deve conferir se corresponde à instalação institucional antes de
usá-lo fora de testes. Use `psql -v ON_ERROR_STOP=1` nas migrações.

`ADMINISTRACAO.md` descreve a migração de senhas legadas e a criação do
primeiro Administrador em terminal interativo. Não inclua senha em argumentos.
Se já existirem contas sem Administrador ativo, a recuperação exige análise do
responsável pelo banco; não recrie uma conta por edição direta. O Administrador
gerencia contas no menu **Equipe Técnica** e cada pessoa troca sua própria
senha no menu lateral. Revogação e expiração de sessão protegem operações.

Os formulários de senha compartilham `CampoSenha`, `FormularioNovaSenha`
e a política de `Senhas`. Novas senhas exigem de 12 a 128 unidades UTF-16,
como na política já usada pelo Java. Um caractere suplementar, como certos
emojis, ocupa duas unidades. Uma senha precisa conter pelo menos um caractere
que não seja espaço em branco. A interface confirma a repetição e recusa
edições acima do limite ou com controles que seriam removidos pelo campo
JavaFX. O DAO mantém a validação final. O limite existente para autenticar
contas legadas e o formato PBKDF2 permanecem preservados.

Relatórios vincula a edição pendente à sessão desde a primeira alteração.
Se a conexão falhar, o acesso fica bloqueado até a revalidação da mesma
sessão. Expiração, revogação, inativação ou mudança de perfil invalidam a
sessão e limpam o rascunho, com aviso no login. A proteção é em memória.
Cadastros, formulários do perfil, Agenda e registro de frequência podem
perder edições não salvas quando a sessão termina.

## Iniciar e diagnosticar

Configure `CASA_DB_URL`, `CASA_DB_USER` e `CASA_DB_PASSWORD` no ambiente local
seguro ou as propriedades JVM equivalentes, sem arquivos versionados com
segredos. Veja `docs/execucao-sem-ide.md` para criar e executar a distribuição
Windows. Para verificar o código, `scripts/verificar.ps1` compila com Java 21;
`-Interface` executa cenários JavaFX/JDBC e requer um PostgreSQL **isolado**.
Falha ao abrir: confira Java 21, JARs, FXML e CSS. Falha ao entrar: confira
servidor, banco, migração de segurança, conta ativa e configuração externa;
não tente senhas históricas em bancos reais. Falha de autorização: revise a
sessão e `CONTRATO_AUTORIZACAO.md`, sem ampliar a matriz por conveniência.

O `nbactions-release-profile.xml` contém caminho absoluto antigo do NetBeans;
use a distribuição independente da IDE. FXML, estilos e imagem estão no
classpath da distribuição. O banco e arquivos exportados pelo usuário ficam
fora dela; preserve esses itens conforme a política de informação da ONG.

## Backup, restauração e continuidade

`docs/backup-restauracao.md` traz os comandos, verificações e limites. O dump
customizado preserva dados e esquema, incluindo os hashes de senha das contas
da aplicação. Preserve à parte papéis globais e configuração do PostgreSQL,
configuração e credenciais JDBC, arquivos exportados e a versão da aplicação.
A restauração de teste também ignora proprietários e privilégios do dump.
Para outro computador: instale Java 21 e PostgreSQL compatível, prepare contas
e banco com um técnico, restaure primeiro **em base nova de teste**, compare
dados e só depois planeje a mudança operacional. Nunca restaure sobre a base
principal para testar. Proteja backups reais com controles de acesso e
criptografia mantida; o ensaio local usou apenas dados fictícios.

As opções institucionais continuam abertas:

| Alternativa | Trabalho ainda necessário |
| --- | --- |
| Backup manual em HD externo | Definir responsável, frequência, mídia, custódia, criptografia e ensaios de recuperação. |
| Backup automático local | Definir computador, conta de serviço, horários, retenção, monitoramento e recuperação. |
| Backup automático com nuvem | Além dos itens locais, decidir fornecedor, conta institucional, região, criptografia, custo e acesso. |

Os scripts técnicos servem como base para qualquer uma delas. Nenhuma opção
foi configurada aqui.

## Segurança e limites verificados

Na versão atual, `ConnectionFactory` lê configuração externa; a inspeção das
fontes atuais não encontrou senha JDBC literal. `.gitignore` ignora arquivos
comuns de credenciais e dumps novos; confira também os arquivos já versionados
antes de publicar. Uma credencial exposta em histórico antigo deve ser
invalidada **se ainda estiver ativa**, pelo responsável pelo ambiente. Não
teste essa credencial contra um banco real e não reescreva o histórico sem
um plano separado.

Separe uma conta administrativa de instalação/migração da conta cotidiana do
aplicativo; uma conta de backup precisa apenas de leitura suficiente para
`pg_dump`. O desenho de privilégios mínimos, o teste com usuário sem poder
administrativo e a custódia das contas reais dependem da instalação escolhida
e **não foram implementados ou validados** neste ensaio. A matriz de perfis da
aplicação está em `CONTRATO_AUTORIZACAO.md` e depende de confirmação da ONG.

## Evidência e pendências

Em 27/11/2026 a versão integrada passou nas suítes JavaFX/JDBC, nos cenários
dirigidos de Relatórios, na proteção SQL e no ciclo de backup e restauração.
Os resultados e as pendências de validação física estão no
[registro de validação](registro-validacao-entrega.md). A implantação, o
número de computadores, a política de backup,
as regras de encerramento PSC/LA e permissões definitivas seguem em aberto.
