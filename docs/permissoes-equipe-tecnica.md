# Proposta experimental de permissões da Equipe Técnica

Esta linha parte da proposta `feda43a` e incorpora por merge as correções de
usabilidade. A política é experimental e depende de aprovação da ONG.
Use a versão em ambiente fictício para demonstrar a proposta.

| Área | Equipe Técnica na matriz anterior | Equipe Técnica no experimento | Administrador e Educador |
| --- | --- | --- | --- |
| Painel | Consulta o painel | Consulta o painel com as mesmas melhorias de usabilidade | Administrador mantém o painel. Educador entra diretamente na planilha. |
| Adolescente, família e referência | Cadastro operacional, educação e trabalho, consulta e edição familiar limitadas | Cadastro completo, endereço, bairro, situação social, saúde, observações, referência, alteração de situação, inativação, dados de responsáveis compartilhados e remoção de vínculos | Administrador mantém acesso total. Educador continua sem acesso ao prontuário. |
| PSC, LA e frequência | Edita medidas abertas, registra e corrige frequência e horas, com justificativas protegidas | Histórico infracional, encerramento e reabertura, justificativas, exclusão de lançamentos e gestão de atividades | Administrador mantém acesso total. Educador continua consultando e lançando frequência sem corrigir, excluir ou lançar horas de PSC. |
| Agenda | Cria, edita, cancela e conclui | Mantém o fluxo e passa a poder excluir e acessar observações protegidas | Administrador mantém o acesso. Educador continua sem acesso. |
| PIA, documentos e relatórios | Relatório operacional da frequência | Confirma envio do PIA, acessa operações existentes de metadados de documentos e usa o relatório completo, incluindo prévia, impressão, exportação e exclusão de rascunhos | Administrador mantém o acesso. Educador continua sem acesso. |
| Contas | Troca a senha própria | Troca a senha própria | Administrador retém listagem de contas, criação, alteração de perfil, ativação, desativação e redefinição de senha de terceiros. Educador continua podendo alterar a própria senha. |

As operações de acompanhamento incluem cadastro, vínculos, medidas, frequência e Agenda.
As concessões sobre informações protegidas abrangem as fichas sociais e de saúde,
o histórico infracional, justificativas, observações, PIA, documentos e relatório completo.
Entre as operações de maior impacto estão a alteração de situação, a inativação,
o encerramento e a reabertura de medidas, as remoções de vínculos, as exclusões e
a saída de relatórios do sistema. `SENHA_PROPRIA_ALTERAR` é de uso pessoal.
`EQUIPE_CONSULTAR` e `USUARIO_GERENCIAR` permanecem administrativas.

O seletor de referência e o da Agenda recebem CPF para vincular, nome e
cargo. O seletor de referência mostra nome e cargo. A consulta omite login,
perfil, senha e demais dados de conta.
Quando um cadastro legado de adolescente compartilha CPF com uma conta, a
Equipe Técnica pode editar os dados do adolescente sem regravar `Pessoa`.
Se a mesma solicitação tenta editar também os dados pessoais compartilhados,
o aplicativo recusa a gravação inteira. Alterações em vínculos familiares
continuam autorizadas e os dados pessoais de contas continuam protegidos.
O utilitário de instalação e
migração continua sendo um procedimento externo com
credenciais PostgreSQL e console. A migração de senhas dentro de uma sessão
autenticada exige a permissão administrativa.

## Demonstração à ONG

1. **Administrador.** Abra o Painel e entre no menu **Equipe Técnica** para
   mostrar a administração de contas.
2. **Equipe Técnica.** Mostre o cadastro completo de um adolescente fictício
   e abra seu perfil. Confira endereço e bairro, família, renda e saúde.
   Mostre a seleção do técnico de referência, PSC e LA, frequência, Agenda
   e confirmação do PIA. Prepare um relatório, abra a prévia e mostre as
   opções de saída. A administração de contas fica indisponível.
3. **Educador.** Entre diretamente pela planilha de frequência e registre
   um lançamento fictício. Confirme as restrições anteriores no editor.

## Decisões para a ONG

- Confirmar quem pode consultar e editar saúde, SPA, renda, benefícios,
  NIS, histórico infracional, observações e documentos protegidos.
- Confirmar consulta, edição, impressão e exportação do relatório completo.
- Confirmar quem pode alterar situação, inativar cadastros, encerrar ou
  reabrir medidas, excluir registros e remover vínculos.
- Confirmar se um profissional com conta desativada pode receber novas
  atribuições como referência. O seletor mantém o comportamento existente e as
  referências históricas.

A interface existente do PIA registra a confirmação de envio, sem geração de
documento. As operações de documentos usam os DAOs existentes e não há tela
nova. O relatório completo mantém as regras de rascunho ligadas à sessão.

## Validação da proposta

- Java 21, JavaFX e PostgreSQL descartável com dados fictícios. A suíte principal
  passou com 262 verificações, incluindo login e logout dos três perfis, cadastro
  completo, relatórios, exclusão de frequência, alterações de Agenda e bloqueio
  administrativo na navegação e nos DAOs.
- Passaram as 78 verificações de preparação, as 338 de usabilidade e a negação
  sem sessão. A matriz foi conferida com login real. A suíte verificou também
  expiração, revogação, mudança de perfil e troca da própria senha.
- Passaram 172 verificações de Relatórios para cada um dos perfis Administrador
  e Equipe Técnica, com tela e monitor reais e falhas JDBC simuladas.
- A revisão de `feda43a` conferiu a distribuição Windows, o PDF nativo com
  Administrador e a revogação durante impressão e exportação. Nesta linha,
  uma conta fictícia da Equipe Técnica gerou um PDF A4 de uma página pelo
  Microsoft Print to PDF. A impressão em papel ficou fora dos ensaios.
