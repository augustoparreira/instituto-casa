# Manual rápido para a equipe do Instituto C.A.S.A.

Este manual descreve a versão integrada com os ajustes finais de usabilidade
e sua matriz provisória de permissões. As contas e dados de exemplos devem ser fictícios. O técnico
responsável entrega o atalho ou a pasta de execução e o login individual.

## Abrir, entrar e sair

1. Abra o aplicativo pelo atalho preparado pelo técnico ou execute
   `iniciar-casa.ps1` na pasta distribuída. Digite seu **login pessoal** e sua
   senha na tela inicial. Não compartilhe a senha.
2. Para mudar sua senha, use **Alterar minha senha** no menu lateral. Informe a
   senha atual e a nova senha quando solicitado. Entre novamente se a sessão
   for encerrada após a troca.
3. Salve o trabalho antes de escolher **Sair do sistema**. Em Relatórios, se
   houver alterações pendentes, confirme o descarte ou cancele a saída para
   voltar ao rascunho. Uma sessão encerrada exige novo login.

Use **Mostrar** para conferir a senha digitada e **Ocultar** para voltar à
exibição protegida. Cada formulário abre com a senha oculta. É possível colar
pelos atalhos ou pelo menu do campo. Copiar e recortar ficam bloqueados nos
dois modos. Ao criar, redefinir ou alterar uma senha, use de 12 a 128 unidades
UTF-16 e repita o valor. Alguns emojis ocupam duas unidades. Frases-senha e
espaços são permitidos. A senha deve ter algum conteúdo além de espaços e
ficar em uma única linha. Maiúsculas, números e símbolos são opcionais.

## Painel de controle

O cartão **Adolescentes em acompanhamento** mostra até cinco adolescentes na
ordem da lista. Em uma janela mais baixa, são exibidas somente as linhas que
couberem inteiras. Em uma janela estreita, nome, medida e progresso se ajustam
à largura da linha. Clique em uma linha para abrir o perfil ou em **Ver todos**
para consultar a lista completa. Os indicadores numéricos consideram o
conjunto completo de registros.

## O que cada perfil pode fazer

| Perfil | Uso principal nesta versão |
| --- | --- |
| Administrador | Gerencia contas e cadastro completo; pode consultar e editar dados protegidos, medidas, frequência, Agenda, PIA e Relatórios. |
| Equipe Técnica | Consulta e edita o cadastro operacional, família permitida, educação, medidas abertas, frequência e Agenda; gera o relatório operacional. Não acessa saúde, renda, PIA ou relatórios completos protegidos. |
| Educador | Consulta e registra frequência. Não corrige nem exclui lançamentos existentes e não registra horas de PSC. |

Se um botão não aparece ou a operação é recusada, peça apoio ao Administrador.
Não use a conta de outra pessoa. A política ainda precisa de confirmação da ONG.

## Adolescentes e medidas

No menu **Adolescentes**, procure a pessoa pelo nome ou filtros da tela e abra
seu perfil. Confira os dados e a medida antes de alterar algo. O Administrador
pode fazer o cadastro completo e editar as fichas protegidas. A Equipe Técnica
pode criar e editar o cadastro operacional; campos sociais e de saúde não são
preenchidos nesse fluxo. O Educador não faz cadastro.

No perfil, consulte as medidas PSC e LA conforme o acesso exibido. A Equipe
Técnica pode cadastrar ou editar medidas abertas; o encerramento e alterações
restritas cabem ao Administrador. As regras de encerramento ainda dependem de
decisão institucional. Não trate uma previsão de prazo como encerramento
automático.

## Frequência e Agenda

Abra a frequência pelo adolescente. Confira atividade, data e status antes de
**Salvar frequência**. O Educador registra presença ou falta, com justificativa
quando solicitada, mas não corrige nem exclui o lançamento depois. A Equipe
Técnica pode corrigir frequência e registrar horas de PSC dentro dos limites
do sistema. O Administrador possui as ações de gestão. Quando uma correção
envolver texto protegido, solicite apoio ao Administrador.

Em **Agenda**, Administrador e Equipe Técnica podem consultar, criar e editar
compromissos permitidos. Confira data e horário antes de salvar. O Educador
não usa esse módulo nesta matriz.

## Relatórios e documentos

O Administrador acessa **Relatórios** para revisar a competência, salvar o
rascunho e usar **Imprimir / salvar PDF**. Antes de sair ou fechar a janela,
salve o texto necessário. A Equipe Técnica usa o **relatório operacional**
disponível em sua planilha; ele não substitui o relatório completo protegido.
O Educador não gera relatórios nesta versão.

Ao tentar sair de Relatórios, trocar de tela ou fechar a janela com X ou
Alt+F4, o aplicativo pede confirmação se houver alterações não salvas.
Se a conexão falhar durante a edição, o formulário fica oculto e as ações
protegidas ficam bloqueadas. O texto permanece em memória e volta à tela
quando a conexão retorna e a mesma sessão é validada. Se a sessão terminar,
o login avisa sobre as alterações perdidas. O texto também é perdido se o
aplicativo for encerrado à força. Use **Salvar rascunho** para persistir o
trabalho. Em outros formulários, salve antes de sair ou trocar de tela.

Na janela de impressão do Windows, escolha a impressora ou **Microsoft Print
to PDF** e confirme o destino. O arquivo PDF salvo pelo driver ainda precisa
de verificação física antes de uso institucional.

## Mensagens e ajuda

- **Acesso negado**: sua conta não tem permissão ou a sessão precisa ser
  renovada. Entre novamente; se persistir, procure o Administrador.
- **Falha de conexão**: avise o técnico. Não repita um salvamento sem conferir
  se o registro foi gravado.
- **Dados inválidos ou obrigatórios**: revise os campos indicados e tente
  novamente.

Procure suporte técnico quando o aplicativo não abrir, o banco ficar
indisponível, a senha não funcionar após conferência do login, ou um registro
aparecer diferente do esperado. Não envie dados sigilosos por mensagens
informais. A rotina de backup das funcionárias ainda não foi definida.
