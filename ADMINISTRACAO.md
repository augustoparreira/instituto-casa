# Instalação das contas e sessões

Use o JDK 21 e as dependências já declaradas no projeto. Configure a conexão local
com `CASA_DB_URL`, `CASA_DB_USER` e `CASA_DB_PASSWORD` ou com as propriedades JVM
`casa.db.url`, `casa.db.user` e `casa.db.password`. Mantenha os valores fora do Git.

## Atualizar uma instalação

1. Feche a aplicação em todas as máquinas e faça backup. Proteja também backups antigos,
   pois podem conter senhas no formato legado.
2. Na base escolhida, aplique `sql/usuarios_seguranca.sql` com interrupção em erro.
   O script adiciona estado, versão de sessão e formato da senha. Se encontrar logins
   equivalentes ou perfis inválidos, a transação é revertida. Revise os registros antes de tentar novamente.
3. Num terminal interativo, execute a classe
   `br.edu.unespar.trabalho.util.AdministracaoInicial` com o argumento `migrar-senhas`.
   Use o classpath do projeto compilado com o driver JDBC. A conversão preserva as senhas
   existentes, usa salt individual e é transacional e reexecutável. O login só aceita o formato convertido.
4. Confira o acesso com uma conta administrativa ativa e distribua a versão atualizada.

Para uma instalação sem contas, execute a mesma classe com `criar-administrador`, após
aplicar a migração. A senha é solicitada sem eco. A criação inicial exige uma tabela de
contas vazia. Se a base existente tiver contas, mas nenhum administrador ativo, a
recuperação deve ser conduzida pelo responsável pelo banco.

O administrador gerencia as contas pelo menu **Equipe Técnica**. Desativação preserva
os vínculos. A senha própria é alterada pelo menu lateral e requer novo login.
`-Dcasa.sessao.minutos=15` configura o prazo de inatividade, com 15 minutos por padrão.

O serviço central aplica a matriz de `CONTRATO_AUTORIZACAO.md`. O Administrador
gerencia as contas e o cadastro completo. A Equipe Técnica consulta e edita os
dados operacionais, com projeções específicas e preservação dos campos protegidos.
Saúde, renda, PIA, relatórios e observações livres de cadastro e agenda ficam
restritos ao Administrador até confirmação da ONG. O Educador entra diretamente
na frequência para registrar presenças e faltas, com justificativa quando exigida.
