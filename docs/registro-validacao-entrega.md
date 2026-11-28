# Registro de validação da entrega

## Integração local (27/11/2026)

- Código integrado validado antes do commit de documentação
  `8fea8872654a41b4f4a657e0a4403c90f990e651`.
- Base `1fb176bb38bb55b6a7cddcf1c91feb6c7c602a3f`. Fast-forward de
  Relatórios até `4c0fbbf` e merge de `1986a0c` sem conflitos.
- Árvore do código validado `d2ef242c6cae9165e6c0d1f84921d6a1ed753f49`,
  idêntica à aprovada na revisão independente.

| Ensaio | Resultado local |
| --- | --- |
| Aplicação após fast-forward e novamente após o merge | Em cada etapa, 218 verificações gerais e 78 dirigidas passaram no Java 21 com PostgreSQL 18.6 isolado. Incluem login, logout e permissões dos três perfis, cadastro, PSC e LA, frequência, Agenda, PIA e usuários. |
| Relatórios e autorização após cada etapa | 172 verificações com JavaFX e monitor reais e JDBC simulado passaram em cada etapa. A verificação de autorização sem sessão confirmou a negação de todas as operações protegidas. |
| Carga fictícia | 10 adolescentes, 10 responsáveis, 5 frequências e 147 restrições. Nos destinos recusados e na falha provocada no meio da carga, dados e esquema permaneceram iguais com `ON_ERROR_STOP=0` e `ON_ERROR_ROLLBACK` desligado e ligado. |
| Backup e restauração | URI e string de conexão recusadas. Catálogo e SHA-256 conferidos. A base com função preexistente foi preservada. Restauração na base nova com 20 tabelas, duas sequências, 147 restrições e dados iguais. |
| Distribuição sem IDE | Pacote reconstruído com Java 21, iniciado em novo caminho com espaços e com JDBC configurado externamente. Login, Painel, Agenda, Relatórios e logout exercitados por automação JavaFX usando os artefatos distribuídos e o banco restaurado. |

As suítes automatizadas conferiram a prévia A4 curta e longa com margens e
paginação. A revisão independente informou teste físico do X, Alt+F4,
cancelamento do descarte e abertura e cancelamento do seletor de exportação.
O Microsoft Print to PDF chegou ao seletor de saída naquele ensaio, sem
arquivo gerado. Permanecem pendentes a verificação física do PDF gravado,
a revogação ou queda JDBC com diálogo nativo aberto e a execução em
outro Windows. Todos os ensaios locais de banco usaram dados fictícios.

## Histórico da branch independente (25/11/2026)

Base: `feature/integracao-final` em `1fb176b`. Trabalho local:
`codex/estabilizacao-entrega`. Data: 25/11/2026. Todo PostgreSQL usado nos
ensaios foi um cluster local temporário na porta 55493, com bases novas e
dados fictícios. Nenhum banco principal ou dado real foi utilizado.

| Verificação executada | Resultado |
| --- | --- |
| `scripts/verificar.ps1 -Interface` com Java 21 e PostgreSQL isolado | 211 verificações concluídas; saída 0. Inclui JavaFX, JDBC, autorização por perfis, cadastro, frequência, PSC, Agenda, PIA e Relatórios da base `1fb176b`. |
| Script de carga em `casa_proibida_teste` com `psql -v ON_ERROR_STOP=0` | Recusado; cliente continuou até `ROLLBACK`; esquema e dados iguais antes/depois, sentinela fictícia preservada. |
| Carga em `casa_apresentacao` marcada | `COMMIT`; 10 adolescentes, 10 responsáveis, 5 frequências e 147 restrições. |
| Falha forçada na inserção intermediária | `ROLLBACK`; esquema e dados iguais antes/depois. |
| Compilação e distribuição sem IDE | Java 21 compilou; FXML, CSS, imagem, JavaFX e JDBC empacotados em pasta com espaços; configuração ausente recusada; a cópia em outro caminho deste PC iniciou sem erro imediato. |
| Backup customizado PostgreSQL 18.6 | `pg_dump` saída 0, arquivo não vazio, catálogo legível e SHA-256 registrado. |
| Restauração em base nova | `pg_restore --single-transaction` saída 0; dados idênticos em dump de 311 linhas normalizadas; 20 tabelas, 2 sequências, 147 restrições em ambas. |
| Falhas de backup/restauração | Recusados diretório inexistente, origem inexistente, destino não vazio ou fora do padrão, ausência de confirmação e arquivo inválido. |

Os dumps completos de origem e destino da restauração apresentaram diferenças
somente na normalização textual de três expressões `CHECK` pelo PostgreSQL;
as restrições e os dados foram preservados. O teste de igualdade dos dados
removeu apenas o token aleatório `\\restrict`/`\\unrestrict` do pg_dump 18.

### Pendências no ensaio de 25/11/2026

- Uso físico do X/Alt+F4 e impressão via Microsoft Print to PDF; os testes
  JavaFX programáticos da revisão de Relatórios são registrados à parte.
- Login e navegação visual na distribuição portátil, instalação em outro
  Windows e conta Windows sem privilégios de
  administrador.
- Falha real no meio do `pg_restore`, servidor indisponível, diretório sem
  permissão, teste da rotina no pgAdmin e criptografia de backup.
- Operação completa da aplicação com conta PostgreSQL de privilégios mínimos,
  restauração em outra máquina e recuperação de acesso na instalação real.

As decisões de implantação e backup pertencem à ONG.

## Revalidação das guardas (26/11/2026)

Branch `codex/estabilizacao-guardas`, a partir de `fe48ab5`. Ensaios no
Windows com Java 21 e clusters PostgreSQL 18.6 descartáveis em loopback
(portas 15442 e 15439), com dados fictícios.

| Verificação | Resultado |
| --- | --- |
| Carga fictícia em destino autorizado e marcado, no psql padrão | 10 adolescentes, 10 responsáveis, 5 frequências e 147 restrições. |
| Recusa por nome de banco ou ausência de marcador | Com `ON_ERROR_STOP=0` e `ON_ERROR_ROLLBACK` ligado e desligado, o cliente executou outra consulta após tentar confirmar a transação. Os dumps de dados e esquema ficaram iguais antes e depois. |
| Falha forçada por trigger na inserção de saúde após a limpeza | Com ambas as configurações de rollback do cliente, a transação não confirmou carga parcial. Os dumps de dados e esquema ficaram iguais. |
| Query Tool do pgAdmin instalado, via backend e perfil temporário | O arquivo inteiro foi aceito na base marcada, com 10 adolescentes, e recusado na base proibida, com dados e esquema preservados. A operação pela interface gráfica não foi ensaiada. |
| Origem do backup | Nome literal aceito. URI, string de conexão, parâmetros embutidos e nomes acima do limite recusados sem ecoar os valores ou criar arquivos. `PGHOSTADDR` e `PGSERVICE` não redirecionaram a conexão. |
| Proteção da restauração | Tabela, visão, sequência, esquema, função, procedimento, tipo, extensão, objeto grande e configuração específica da base foram recusados. Os destinos foram preservados. Destino inexistente, nome proibido e arquivo inválido também foram recusados. |
| Ciclo de backup e restauração | Catálogo reconhecido e SHA-256 conferido. Restauração em base nova preservou os dados e valores de sequências. As contagens de tabelas, sequências e restrições coincidiram. |
| Suíte JavaFX/JDBC com `scripts/verificar.ps1 -Interface` | 211 verificações passaram, incluindo autenticação e acesso dos três perfis. |
| Distribuição Windows fora da IDE | O pacote foi compilado, movido para outra pasta com espaços e iniciado de lá via PowerShell. O processo permaneceu aberto por sete segundos. Login manual e uso em outro Windows não foram testados. |

Nos ensaios de igualdade, o único filtro dos dumps foi a remoção dos tokens
aleatórios `\restrict` e `\unrestrict` do PostgreSQL 18.
