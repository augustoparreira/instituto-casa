# Preparação da entrega

## Integração final de 24/11/2026

O commit `0fdb8b2` reúne `feature/administrador` (`1ad2b95`) e `feature/preparacao-entrega` (`1d06963`). A configuração das contas está em [ADMINISTRACAO.md](../ADMINISTRACAO.md) e a matriz está no [contrato de autorização](../CONTRATO_AUTORIZACAO.md).

- A suíte principal passou em 178 verificações no JDK 21, com contas fictícias autenticadas no PostgreSQL isolado. Inclui login e logout dos três perfis, vigência e bloqueio de reabertura de medida pela Equipe Técnica, edição de frequência e preservação de informações reservadas.
- O cenário de preparação passou em 18 verificações de formulários e propagação de alterações. A prévia e a paginação A4 foram exercitadas, inclusive com texto distribuído em sete páginas na suíte principal.
- A migração de segurança e a conversão de senhas legadas foram verificadas, incluindo reexecução, login após a conversão e preservação do vínculo com o técnico de referência. O utilitário de conversão também foi executado em terminal com uma conta fictícia.
- O PDF produzido pelo driver do Windows ainda requer a conferência manual abaixo. A ONG ainda precisa definir o alcance do Educador, o acesso técnico a campos sigilosos, a inclusão completa com campos sociais obrigatórios e o tratamento de descumprimento marcado manualmente.

## Histórico da preparação anterior à integração

A branch feature/preparacao-entrega parte de 8e09fe9 e inclui a correção posterior de consultas familiares, originalmente publicada em 2609f14.

Foram aprovadas 18 verificações dirigidas. A compilação utilizou --release 21 com o JDK 25 instalado neste computador.

O cenário VerificacaoPreparacaoEntrega exercita formulários reais JavaFX com dados fictícios em schema descartável. Cobre máscaras, datas inexistentes e bissextas, correção da data do responsável, cancelamento de familiar, renda não finita, identificação da sessão e propagação de frequência corrigida até o relatório reaberto. Também prepara a prévia de impressão.

O código utiliza datas estritas em DD/MM/AAAA. A data digitada do responsável é confirmada antes da validação. A renda rejeita NaN e infinito ainda no formulário. Os módulos operacionais consultam a identidade da sessão para exibição e substituem os nomes demonstrativos. Login, contas e matriz de permissões continuam sob responsabilidade do desenvolvedor principal.

## Conferência manual do PDF

1. Na base de demonstração, prepare um relatório fictício, preencha o processo e revise as duas seções.
2. Confira a prévia e use Imprimir / salvar PDF.
3. Escolha Microsoft Print to PDF, papel A4 e orientação retrato. Mantenha escala de 100% quando o driver oferecer essa opção.
4. Salve o arquivo na pasta desejada e abra o PDF.
5. Confira o tamanho A4 nas propriedades, as margens de 36 pontos e a numeração de todas as páginas.
6. Repita com textos suficientes para várias páginas e uma palavra extensa. Verifique início, fim e trechos nas quebras, procurando cortes ou omissões.

O arquivo final do driver ainda depende dessa conferência manual. Nenhuma janela do sistema operacional foi controlada nesta etapa. As condições de login e troca de sessão devem ser conferidas após a integração do Administrador.
