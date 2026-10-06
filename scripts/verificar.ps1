param([switch]$Integracao, [switch]$Interface)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$repoMaven = Join-Path $env:USERPROFILE '.m2/repository'
$dependencias = @(
    'org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-win.jar',
    'org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-win.jar',
    'org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-win.jar',
    'org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-win.jar',
    'org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar'
) | ForEach-Object { Join-Path $repoMaven $_ }
foreach ($arquivo in $dependencias) {
    if (!(Test-Path -LiteralPath $arquivo)) { throw "Dependência já declarada no pom não encontrada: $arquivo. Execute mvn compile primeiro." }
}
$classpath = $dependencias -join ';'
New-Item -ItemType Directory -Path 'target/classes','target/test-classes' -Force | Out-Null
Copy-Item -Path 'src/main/resources/*' -Destination 'target/classes' -Recurse -Force
$fontes = @(Get-ChildItem -LiteralPath 'src/main/java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -classpath $classpath -d target/classes @fontes
if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
$testes = @(Get-ChildItem -LiteralPath 'src/test/java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 21 -encoding UTF-8 -classpath "target/classes;$classpath" -d target/test-classes @testes
if ($LASTEXITCODE -ne 0) { throw 'A compilação dos testes falhou.' }
$opcoes = @()
if ($Integracao -or $Interface) { $opcoes += '--integracao' }
if ($Interface) { $opcoes += '--interface' }
& java -ea -classpath "target/test-classes;target/classes;src/main/resources;$classpath" br.edu.unespar.trabalho.VerificacaoCadastroFrequencia @opcoes
if ($LASTEXITCODE -ne 0) { throw 'A verificação falhou.' }
