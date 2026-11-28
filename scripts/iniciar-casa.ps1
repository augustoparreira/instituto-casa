$ErrorActionPreference = 'Stop'
$diretorio = $PSScriptRoot
foreach ($nome in @('CASA_DB_URL','CASA_DB_USER','CASA_DB_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($nome)) { throw "Configure $nome fora do Git antes de iniciar." }
}
$java = Get-Command java -ErrorAction Stop
if ((& $java.Source -version 2>&1 | Select-Object -First 1) -notmatch 'version "21\.') { throw 'Java 21 é obrigatório.' }
$bibliotecas = Join-Path $diretorio 'lib'
$classes = Join-Path $diretorio 'classes'
if (-not (Test-Path -LiteralPath (Join-Path $classes 'View/Login.fxml'))) { throw 'Recursos FXML ausentes.' }
foreach ($nome in @('javafx-base-21.0.2-win.jar','javafx-graphics-21.0.2-win.jar','javafx-controls-21.0.2-win.jar','javafx-fxml-21.0.2-win.jar','postgresql-42.7.2.jar')) {
    if (-not (Test-Path -LiteralPath (Join-Path $bibliotecas $nome))) { throw "Biblioteca ausente: $nome" }
}
$jdbc = Join-Path $bibliotecas 'postgresql-42.7.2.jar'
& $java.Source --module-path $bibliotecas --add-modules javafx.controls,javafx.fxml -classpath "$classes;$jdbc" br.edu.unespar.trabalho.Main
if ($LASTEXITCODE -ne 0) { throw "Aplicação encerrou com código $LASTEXITCODE" }
