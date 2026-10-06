# Cadastro e frequência dos adolescentes

Implementação dentro da estrutura existente: modelos em `model`, persistência JDBC em `dao`, controladores em `controller` e telas FXML em `resources/View`. As telas reutilizam `adolescentes-style.css`. O `pom.xml` não recebeu dependências novas.

## Preparação do banco

Execute `sql/ajustes_cadastro_frequencia.sql` no banco PostgreSQL configurado em `ConnectionFactory`, antes de abrir as telas atualizadas. É possível usar o console SQL da IDE ou o psql:

```powershell
psql -U SEU_USUARIO -d SEU_BANCO -v ON_ERROR_STOP=1 -f sql/ajustes_cadastro_frequencia.sql
```

O script pressupõe as tabelas do projeto já criadas. Ele roda em transação e pode ser executado novamente. Acrescenta os campos utilizados pelas telas e amplia campos existentes; não exclui tabelas nem registros. Não é executado automaticamente ao iniciar o programa.

A frequência agora guarda o ID da PSC à qual as horas pertencem. Para dados antigos, o script vincula somente os lançamentos com uma única PSC compatível com a data. Horas sem vínculo permanecem registradas, são sinalizadas na tela e precisam de revisão antes de integrar o saldo de uma medida. Havendo medidas antigas sobrepostas, revise primeiro os períodos na aba de medidas.

Parentesco e contato principal passam a ser guardados no vínculo entre adolescente e responsável. Assim, uma mesma pessoa pode ser responsável por mais de um adolescente com relações diferentes.

## Fluxo de cadastro

1. Em **Adolescentes**, use **Novo adolescente**.
2. Preencha identificação, situação social, saúde, escola e trabalho. Idade e mês de aniversário são calculados pelo nascimento. A renda familiar é informada em salários mínimos, por exemplo `1,5`.
3. Ao salvar, o perfil é aberto. Em **Família e Social**, inclua, edite ou desvincule responsáveis e cadastre os demais integrantes da família. A renda individual do familiar continua em reais, conforme o campo existente.
4. Em **Medida Socioeducativa**, cadastre PSC (horas) e/ou LA (meses), reincidência e técnico de referência. Use **Editar / encerrar** para corrigir a medida ou informar a data de encerramento confirmada.
5. Use **Editar cadastro completo** para alterar os demais dados. O CPF permanece como chave do cadastro. Para desativar o cadastro, escolha **Inativo**; o histórico é preservado.

Dados pessoais, sociais, de saúde e de educação/trabalho são gravados na mesma transação. Falha em uma parte reverte o cadastro inteiro. Os vínculos e as medidas possuem suas próprias operações no perfil, como no fluxo existente.

## Fluxo de frequência

- Abra **Frequência mensal** na lista de adolescentes ou **Frequência e Horas** no perfil.
- Escolha o mês e pesquise pelo nome/CPF. A tabela tem uma coluna por dia do mês e rolagem horizontal para consultar os totais, medidas, IMM, vale-transporte e envio do PIA já cadastrado.
- Dê duplo clique no dia, ou selecione o adolescente e use **Registrar / corrigir**. Escolha atividade, situação e horas. **+ Atividade** cadastra uma atividade com a carga horária padrão do encontro.
- Para corrigir, selecione o lançamento na lista do dia. Para trocar sua data ou atividade, exclua o lançamento e registre novamente. A exclusão pede confirmação.
- Presença com horas exige uma PSC do adolescente vigente na data. Acompanhamento sem PSC permite presença com zero horas. LA é acompanhada em meses completos.
- Ausência injustificada aparece como **A** e justificada como **J**; presença aparece como **P**. A justificativa exige uma observação. Se houver situações diferentes em atividades do mesmo dia, a célula mostra todas, como `A/P`.

## Regras utilizadas

- Dias sem lançamento ficam em branco e não contam como falta.
- A situação mensal fica **Irregular** a partir de duas **datas distintas** com ausência injustificada no mês. Faltas justificadas não entram nesse cálculo. Essa interpretação está explícita nas telas para revisão com a ONG. Ela não altera automaticamente o status cadastral nem declara descumprimento da medida.
- Somente presenças creditam horas. Corrigir ou excluir uma presença recalcula o saldo consultando os registros persistidos.
- As horas são inteiras, seguindo o modelo SQL existente, com limite total de 24 horas por dia entre as atividades. Datas futuras são rejeitadas.
- Os totais são acumulados por ID de PSC até o fim do mês selecionado, limitado à data atual. Horas de uma PSC anterior não são transferidas para outra, inclusive quando as duas aparecem no mesmo mês. O saldo nunca fica negativo.
- LA e PSC podem coexistir. Medidas do mesmo tipo não podem ter períodos sobrepostos. Uma alteração de período não pode deixar lançamentos já vinculados fora da vigência.
- A tela mensal exibe meses decorridos/previstos de LA; alcançar as horas ou meses previstos não encerra automaticamente uma medida.
- Inativos sem lançamentos no mês ficam ocultos por padrão na planilha. A opção de exibição permite consultá-los.

Relatórios, perfis de acesso e geração de PIA não foram ampliados nesta etapa.

## Verificação local

O executável de teste usa o próprio JDK, JavaFX e o driver JDBC que já constam no projeto. Não utiliza framework de testes.

```powershell
# Compilação e regras de frequência, sem acessar o banco:
./scripts/verificar.ps1

# Inclui DAOs, transações, ajuste SQL e integração das telas:
./scripts/verificar.ps1 -Integracao -Interface
```

O script utiliza Java 21 ou superior e os JARs já baixados no repositório Maven local do Windows. O teste de integração requer PostgreSQL disponível e permissão para criar schema. Ele cria um schema aleatório `casa_teste_<uuid>`, usa exclusivamente dados fictícios e remove somente esse schema ao terminar. Não aplica o ajuste no schema principal. As imagens das telas verificadas ficam em `target/verificacao`.

Os testes cobrem cadastro e edição, CPF duplicado, rollback de cadastro incompleto, responsável compartilhado, composição familiar, medidas simultâneas e seus períodos, duplicidade de frequência, correção e exclusão, saldo por PSC, faltas justificadas, limite diário, ano bissexto e gravação pela tela JavaFX.
