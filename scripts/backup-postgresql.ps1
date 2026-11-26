param(
    [Parameter(Mandatory)][string]$HostBanco,
    [Parameter(Mandatory)][ValidateRange(1,65535)][int]$Porta,
    [Parameter(Mandatory)][string]$Banco,
    [Parameter(Mandatory)][string]$Usuario,
    [Parameter(Mandatory)][string]$Diretorio
)
$ErrorActionPreference = 'Stop'
if ($Banco -cnotmatch '\A[A-Za-z_][A-Za-z0-9_-]{0,62}\z') {
    throw 'Informe apenas um nome literal de banco (até 63 letras ASCII, dígitos, sublinhados ou hífens, começando com letra ou sublinhado).'
}
if (-not (Test-Path -LiteralPath $Diretorio -PathType Container)) { throw 'Diretório de backup inexistente.' }
$destino = (Resolve-Path -LiteralPath $Diretorio).Path
$dump = (Get-Command pg_dump -ErrorAction Stop).Source
$restore = (Get-Command pg_restore -ErrorAction Stop).Source
Write-Output (& $dump --version)
Write-Output (& $restore --version)
$identificador = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$identificador += '-' + [Guid]::NewGuid().ToString('N').Substring(0,8)
$final = Join-Path $destino "casa-$identificador.backup"
$temporario = "$final.partial"
$hostaddrAnterior = [Environment]::GetEnvironmentVariable('PGHOSTADDR', 'Process')
$servicoAnterior = [Environment]::GetEnvironmentVariable('PGSERVICE', 'Process')
try {
    # libpq pode obter outro endereço de PGHOSTADDR ou de um serviço herdado.
    if (Test-Path Env:PGHOSTADDR) { Remove-Item -LiteralPath Env:PGHOSTADDR }
    if (Test-Path Env:PGSERVICE) { Remove-Item -LiteralPath Env:PGSERVICE }
    & $dump -h $HostBanco -p $Porta -U $Usuario -d $Banco -Fc -f $temporario
    if ($LASTEXITCODE -ne 0) { throw 'pg_dump falhou.' }
    $item = Get-Item -LiteralPath $temporario -ErrorAction Stop
    if ($item.Length -eq 0) { throw 'Backup vazio.' }
    & $restore --list $temporario | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Catálogo do backup inválido.' }
    $hash = (Get-FileHash -LiteralPath $temporario -Algorithm SHA256).Hash
    [IO.File]::Move($temporario,$final)
    Write-Output "BACKUP_VERIFICADO=$final"
    Write-Output "SHA256=$hash"
} finally {
    if ($null -ne $hostaddrAnterior) { $env:PGHOSTADDR = $hostaddrAnterior }
    if ($null -ne $servicoAnterior) { $env:PGSERVICE = $servicoAnterior }
}
