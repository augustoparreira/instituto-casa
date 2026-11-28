param([string]$Destino)
$ErrorActionPreference = 'Stop'
$projeto = Split-Path -Parent $PSScriptRoot
if (-not $Destino) { $Destino = Join-Path $projeto ('target/distribuicao-casa-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
if (Test-Path -LiteralPath $Destino) { throw "Destino já existe: $Destino" }
$java = Get-Command java -ErrorAction Stop
$javac = Get-Command javac -ErrorAction Stop
if ((& $java.Source -version 2>&1 | Select-Object -First 1) -notmatch 'version "21\.') { throw 'JDK 21 é obrigatório.' }
$repositorio = Join-Path $env:USERPROFILE '.m2/repository'
$arquivos = @(
    'org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-win.jar',
    'org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-win.jar',
    'org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-win.jar',
    'org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-win.jar',
    'org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar'
)
$dependencias = @($arquivos | ForEach-Object { Join-Path $repositorio $_ })
foreach ($arquivo in $dependencias) {
    if (-not (Test-Path -LiteralPath $arquivo)) { throw "Dependência do pom.xml ausente: $arquivo" }
}
$fontes = @(Get-ChildItem -LiteralPath (Join-Path $projeto 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
if ($fontes.Count -eq 0) { throw 'Nenhuma fonte Java encontrada.' }
$destinoCompleto = [IO.Path]::GetFullPath($Destino)
$classes = Join-Path $destinoCompleto 'classes'
$bibliotecas = Join-Path $destinoCompleto 'lib'
New-Item -ItemType Directory -Path $classes, $bibliotecas -Force | Out-Null
& $javac.Source --release 21 -encoding UTF-8 -classpath ($dependencias -join ';') -d $classes @fontes
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação Java.' }
Copy-Item -Path (Join-Path $projeto 'src/main/resources/*') -Destination $classes -Recurse -Force
foreach ($arquivo in $dependencias) { Copy-Item -LiteralPath $arquivo -Destination $bibliotecas }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'iniciar-casa.ps1') -Destination $destinoCompleto
Write-Output "Distribuição criada em $destinoCompleto"
