# Contrato compartilhado de autorização

Java 21, JavaFX, JDBC e DAO/MVC; sem dependência nova. Esta base fornece o contrato;
os pontos de aplicação nos módulos serão integrados nas branches de desenvolvimento.

- `br.edu.unespar.trabalho.model.Operacao`: enum completo das operações; nomes são o contrato entre branches.
- `br.edu.unespar.trabalho.service.AutorizacaoService`: métodos estáticos
  `boolean pode(Operacao)`, `void exigir(Operacao)`,
  `boolean pode(Operacao, ContextoAcesso)`, `void exigir(Operacao, ContextoAcesso)`.
- `br.edu.unespar.trabalho.service.AcessoNegadoException extends SecurityException`:
  lançada por `exigir`; apresentar aviso e interromper a ação.
- `br.edu.unespar.trabalho.model.ContextoAcesso(Long cpfAdolescente, Integer idDocumento,
  Long cpfProfissional, boolean sigiloso)`; fábrica `adolescente(long cpf)`.
  Obter classificação de dados confiáveis, nunca de checkbox ou parâmetro manipulável.
  Os identificadores permitem futuras regras contextuais, mas NÃO concedem acesso por autoria/vínculo.
- Sessão existente em `util.Sessao`. O DTO agora distingue `getLogin()`, `getNomeCompleto()`
  e `getPerfil()` (enum). O getter textual e construtor antigo permanecem compatíveis.

Administrador: todas as operações, inclusive sigilo. Equipe Técnica: concessões
operacionais e de sigilo listadas explicitamente no EnumSet TECNICA. A gestão
e a listagem administrativa de contas permanecem exclusivas do Administrador.
Educador: consulta e registro de frequência e troca da própria senha, com as
projeções limitadas e sem acesso a correções, exclusões, horas de PSC ou dados sigilosos.
As operações protegidas exigem sessão válida. Autoria e vínculo por si sós
continuam sem conceder permissões. A ampliação desta branch é uma
[proposta experimental](docs/permissoes-equipe-tecnica.md) pendente de aprovação.

Usar `pode` na apresentação e `exigir` ANTES das consultas e das mutações. Uma tela que
mistura dados comuns e sigilosos precisa separar as consultas; nunca carregar tudo e apenas
ocultar a aba. Aplicar guarda também no DAO/entrada de negócio para impedir chamadas diretas.
Operações compostas exigem todas as permissões envolvidas. Frequência com horas de PSC
exige adicionalmente HORAS_PSC_REGISTRAR; edição e exclusão têm permissões próprias.

Operações disponíveis (ver enum para compilação): PAINEL_CONSULTAR; ADOLESCENTE_CONSULTAR,
CADASTRAR, EDITAR, EXCLUIR (com prefixo ADOLESCENTE_); ADOLESCENTE_SITUACAO_ALTERAR;
FAMILIA_CONSULTAR/ALTERAR/REMOVER; FAMILIA_PESSOA_COMPARTILHADA_ALTERAR;
SOCIAL_CONSULTAR/ALTERAR; SAUDE_CONSULTAR/ALTERAR; EDUCACAO_CONSULTAR/ALTERAR;
MEDIDA_CONSULTAR/CADASTRAR/EDITAR/ENCERRAR; FREQUENCIA_CONSULTAR/REGISTRAR/EDITAR/EXCLUIR;
HORAS_PSC_CONSULTAR/REGISTRAR; FREQUENCIA_JUSTIFICATIVA_CONSULTAR/ALTERAR;
ATIVIDADE_GERENCIAR; AGENDA_CONSULTAR/CADASTRAR/EDITAR/EXCLUIR;
PIA_CONSULTAR/ALTERAR; DOCUMENTO_CONSULTAR/ALTERAR; RELATORIO_OPERACIONAL_GERAR; RELATORIO_CONSULTAR/ALTERAR/EXCLUIR/EXPORTAR;
EQUIPE_CONSULTAR; USUARIO_GERENCIAR; REFERENCIA_DEFINIR; SIGILOSO_CONSULTAR/ALTERAR;
SENHA_PROPRIA_ALTERAR.

Use a matriz central e as verificações de `AutorizacaoService` nos módulos.
`Sessao` continua responsável pela expiração e pela revalidação da conta.

`VerificacaoAutorizacao` verifica a recusa sem sessão com `-ea`. O cenário integrado com PostgreSQL verifica a matriz e o contexto sigiloso após login real dos três perfis.


Na matriz anterior, a Equipe Técnica criava o cadastro operacional sem gravar fichas
sociais ou de saúde, editava medidas abertas e corrigia frequência com acesso
restrito a justificativas protegidas. A matriz permitia registrar justificativa se o
lançamento ainda estivesse sem texto reservado. Remoções familiares, alterações manuais
de situação e edição de responsáveis compartilhados ficavam com o Administrador.

A geração operacional na planilha usa a competência, a identificação do adolescente
e os lançamentos com medida e horas autorizadas. A prévia interna omite justificativas
e independe dos rascunhos do relatório completo. Na proposta experimental,
a Equipe Técnica pode usar os dois relatórios. O âmbito da frequência do
Educador continua sujeito à confirmação institucional.
