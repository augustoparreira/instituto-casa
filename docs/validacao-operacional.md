# Validação operacional

Base c37b9cc, em feature/validacao-operacional.

## Correções e evidências

- O DAO de edição operacional exige telefone, assim como o cadastro completo.
- A consulta social usa o contato atual de Pessoa, evitando mostrar a cópia antiga após uma edição operacional. A gravação completa já trata esse telefone como o contato do adolescente. SOCIAL_CONSULTAR continua obrigatório.
- A exclusão de frequência informa quando o lançamento já foi removido e atualiza a lista e os totais.
- Prévia e impressão usam o mesmo conteúdo, distribuição das páginas e rodapés. A prévia representa A4 com margens de 36 pontos.

Passaram 150 verificações em PostgreSQL descartável, incluindo cadastro, família, dados complementares, medidas simultâneas e sucessivas, frequência, agenda, PIA, relatórios e autorizações. A compilação usa --release 21, com o JDK 25 instalado neste computador. A paginação extensa foi conferida em sete páginas, com acentos, palavra longa, preservação integral e numeração. A prévia foi inspecionada visualmente. As imagens ficam em target/verificacao, fora dos commits.

## Pendências

O teste pelo driver Microsoft Print to PDF foi tentado. A revisão automática de permissões rejeitou o controle da janela Java com a mensagem “Computer Use was not approved to use java”. O programa temporário foi encerrado. O arquivo PDF produzido pelo sistema operacional continua sem validação.

A frequência não distingue no banco a origem manual ou automática de EM_DESCUMPRIMENTO. É necessária uma decisão da ONG sobre uma marcação manual nessa categoria após corrigir faltas. Inativo e Em análise para extinção foram testados e são preservados.

LA pode ultrapassar o período previsto sem encerramento automático. A previsão não confirma decisão judicial. O calendário mensal considera as medidas que abrangem a competência consultada.

Login, sessões e gerenciamento de contas continuam sob responsabilidade do desenvolvedor principal. A inclusão completa pelo técnico depende dos campos sociais obrigatórios e da matriz. Revalidar o direcionamento das telas por perfil e mudanças de sessão na futura versão integrada.

## Continuação automatizada

As consultas de responsáveis e composição familiar agora propagam falhas SQL ao tratamento de erro da tela. Foram aprovadas 94 verificações de regras e integração JDBC, sem abrir a interface, incluindo indisponibilidade das consultas, listas vazias válidas e preservação da renda familiar em edição técnica. A validação manual da impressão PDF fica com o usuário. Computer Use permanece suspenso a pedido dele.
