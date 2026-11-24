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

Administrador: todas as operações, inclusive sigilo. Equipe Técnica: matriz operacional
provisória no EnumSet TECNICA do serviço. Educador: consulta/registro de frequência e troca
da própria senha; sem edição/exclusão ou horas de PSC. Sem sessão/perfil válido: negar tudo.
Saúde, renda/social, PIA, documentos e relatórios completos ficam restritos ao Administrador
até existir classificação segura das informações e matriz aprovada. Autoria não libera sigilo.

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

Não implementar outra matriz, `pode` ou `exigir` nos módulos. Não alterar lógica de PIA/PDF
para integrar este contrato. Expiração e revalidação da conta serão adicionadas mantendo as assinaturas.

`VerificacaoAutorizacao` verifica a recusa sem sessão com `-ea`. O cenário integrado com PostgreSQL verifica a matriz e o contexto sigiloso após login real dos três perfis.


Na política provisória, a Equipe Técnica cria o cadastro operacional sem gravar fichas
sociais ou de saúde, edita medidas abertas e corrige frequência sem ler ou sobrescrever
justificativas protegidas. Justificativas novas podem ser registradas quando não há
texto reservado no lançamento. Remoções familiares, alterações manuais de situação
e mudanças nos dados pessoais de responsáveis compartilhados ficam com o Administrador.

A geração operacional na planilha usa a competência, a identificação do adolescente
e os lançamentos com medida e horas autorizadas. A prévia interna omite justificativas
e independe dos rascunhos do relatório completo. A ONG ainda precisa validar o acesso
técnico a campos protegidos, a autorização para confirmar o envio do PIA e o alcance
da frequência do Educador.
