# Pontos de integração da Equipe Técnica

A matriz e o contrato de autorização serão fornecidos pelo desenvolvedor responsável pela camada central. Os itens abaixo identificam operações existentes e a posição das futuras chamadas. A presença de uma operação nesta lista não concede permissão ao perfil.

| Controller | Consultas anteriores à exibição | Gravações e outras ações |
| --- | --- | --- |
| AdolescentesController | initialize carrega listarResumoDTO; clicarAdolescente abre o perfil | abrirTelaCadastro inicia inclusão |
| CadastroAdolescenteController | carregarParaEdicao consulta cadastro, situação social, saúde, educação e trabalho | salvarAdolescente distingue inclusão e edição; também grava o indicador de envio do PIA |
| DetalhesAdolescenteController | carregarDados consulta saúde, escola, trabalho, observações, família, renda, PIA, medidas e frequência antes de selecionar uma aba | salvarMedida distingue inclusão e edição e recebe encerramento; definirTecnico altera referência; formulários de responsável e familiar incluem e editam; botões dinâmicos desvinculam responsável e excluem familiar |
| FrequenciaController | carregarMes consulta a planilha; inclui medida, situação mensal e indicador PIA | registrar e duplo clique abrem o editor |
| RegistroFrequenciaController | carregar e carregarDia consultam adolescente, atividades, medidas e histórico com justificativas | salvar distingue inclusão/correção e registra horas; excluir remove; novaAtividade cadastra atividade |
| AgendaController | atualizarAgenda consulta compromissos e observações; abrirFormulario consulta adolescentes e equipe | callback de confirmação em abrirFormulario inclui/edita; alterarStatus conclui/cancela; excluir remove |
| RelatoriosController | initialize e atualizarLista consultam adolescentes e relatórios; gerarNovoRelatorio lê família, situação social e frequência; exibir e visualizarRelatorio apresentam o conteúdo | salvarAtualizacaoRelatorio grava; excluirRelatorio remove; exportarTexto e exportarPara exportam |

Consultar a autorização antes das leituras indicadas, antes de carregar dados em diálogos e novamente no ponto de gravação, exclusão ou exportação. A consulta para apresentar botões complementa a verificação da ação.

PIA e Relatórios terão integração após a atualização funcional do desenvolvedor principal. Para PIA, conferir os futuros pontos equivalentes a carregarAbaPia e marcarPiaEnviado. O fluxo atual também oferece editarPia, salvarPia e exportarTextoPia. As assinaturas devem ser conferidas na versão entregue por ele.

O acesso a saúde, renda, informações familiares, histórico infracional, justificativas, observações da agenda e conteúdo dos relatórios exige avaliação da matriz central. A política de sigilo, delegações, alcance das consultas e autorização para corrigir/excluir/exportar continuam provisórios. O vínculo de referência não é usado como restrição de consulta.

## Integração com 69afe76

As guardas utilizam AutorizacaoService e Operacao da base publicada. Saúde, social, PIA, referência, encerramento, exclusões e exportações seguem a matriz central. Consultas operacionais omitem bairro, renda e confirmação do PIA conforme a permissão. Frequência possui projeções específicas para Educador, sem carregar prontuário ou histórico infracional.

A edição técnica de cadastro preserva social, saúde e PIA em uma transação própria para dados pessoais e educação. A inclusão completa continua exigindo SOCIAL_ALTERAR e SAUDE_ALTERAR porque o cadastro atual exige endereço/bairro e grava as fichas juntas. Essa operação precisa de definição conjunta antes de liberar a inclusão pelo técnico. Não foram criados valores sociais fictícios.

A autenticação, o direcionamento inicial por perfil, a revalidação/expiração da sessão e o gerenciamento de contas dependem da integração do desenvolvedor principal. O Educador dispõe das entradas de frequência, mas o login atual ainda o direciona ao Dashboard, cuja consulta sua matriz nega. Os menus e guardas operacionais não substituem essa integração.
