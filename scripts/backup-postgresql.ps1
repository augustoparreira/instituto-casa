param(
    [Parameter(Mandatory)][string]$HostBanco,
    [Parameter(Mandatory)][ValidateRange(1,65535)][int]$Porta,
    [Parameter(Mandatory)][string]$Banco,
    [Parameter(Mandatory)][string]$Usuario,
    [Parameter(Mandatory)][string]$Diretorio
)
$ErrorActionPreference = 'Stop'
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
try {
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
} catch {
    # O arquivo parcial nunca recebe a extensão de backup válido.
    throw
}
