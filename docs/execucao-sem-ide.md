# Execução no Windows sem IDE

Requer JDK 21 e PostgreSQL acessível. O projeto declara JavaFX 21.0.2 e
PostgreSQL JDBC 42.7.2 em `pom.xml`. As bibliotecas precisam estar no cache
Maven do usuário que **prepara** a distribuição; na máquina que a executa,
esse cache e a IDE não são necessários. Se faltar uma biblioteca, obtenha as
dependências declaradas no `pom.xml` com Maven antes da preparação.

No PowerShell, na raiz do projeto:

```powershell
./scripts/preparar-distribuicao.ps1
```

O comando compila as fontes com `javac --release 21`, copia FXML, CSS e imagens
para `classes`, e coloca JavaFX Windows e o driver JDBC em `lib`. O resultado
fica em `target/distribuicao-casa-<data-hora>`. Um destino explícito pode ser
passado por `-Destino`; ele deve **não existir**, para evitar sobrescrita.
Copie a pasta inteira para o local desejado. Na máquina de execução, configure
`CASA_DB_URL`, `CASA_DB_USER` e `CASA_DB_PASSWORD` fora do repositório, para
um banco preparado conforme `ADMINISTRACAO.md`. Execute, dentro da pasta
distribuída:

```powershell
./iniciar-casa.ps1
```

O script exige Java 21, as três variáveis, FXML e todos os JARs. Inicia a classe
`br.edu.unespar.trabalho.Main` com módulos JavaFX e driver JDBC; não contém
senha. Não precisa de privilégio de administrador para executar a aplicação.
O banco, seus dados e as credenciais não acompanham a pasta distribuída.

O arquivo versionado `nbactions-release-profile.xml` contém um diretório
absoluto de outro computador. Ele afeta a ação de execução desse perfil no
NetBeans, mas não esta distribuição. Nenhuma alteração nele é necessária para
iniciar sem IDE.

## Validação local de 25/11/2026

- Java 21 e `javac --release 21`: compilação aprovada.
- Distribuição criada em `target/CASA teste portatil`, em caminho com espaços;
  FXML, CSS, imagem, módulos JavaFX e JDBC foram copiados.
- O script recusou `CASA_DB_URL` vazia antes de iniciar Java. Com as variáveis
  apontando explicitamente para o PostgreSQL fictício, permaneceu em execução
  sem erro imediato até interrupção controlada, fora da IDE. A validação visual do login e da navegação na pasta
  distribuída e a execução em outro computador estão pendentes. A pasta também
  foi copiada para outro caminho com espaços neste PC; o inicializador tornou
  a permanecer em execução sem erro imediato. Os testes JavaFX/JDBC da revisão
  separada passaram no mesmo PC.
- A impressão física via Microsoft Print to PDF não foi executada nesta etapa.

Esta pasta é uma distribuição técnica simples, não uma instalação institucional.
A escolha da base, do servidor e dos computadores continua em aberto.
