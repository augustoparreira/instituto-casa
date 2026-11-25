# Registro de verificação da branch independente

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

## Ainda não executado

- Uso físico do X/Alt+F4 e impressão via Microsoft Print to PDF; os testes
  JavaFX programáticos da revisão de Relatórios são registrados à parte.
- Login e navegação visual na distribuição portátil, instalação em outro
  Windows e conta Windows sem privilégios de
  administrador.
- Falha real no meio do `pg_restore`, servidor indisponível, diretório sem
  permissão, teste da rotina no pgAdmin e criptografia de backup.
- Operação completa da aplicação com conta PostgreSQL de privilégios mínimos,
  restauração em outra máquina e recuperação de acesso na instalação real.

Nenhuma dessas lacunas é tratada como teste aprovado. As decisões de
implantação e backup pertencem à ONG.
