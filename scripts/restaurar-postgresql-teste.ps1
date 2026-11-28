param(
    [Parameter(Mandatory)][string]$HostBanco,
    [Parameter(Mandatory)][ValidateRange(1,65535)][int]$Porta,
    [Parameter(Mandatory)][string]$Banco,
    [Parameter(Mandatory)][string]$Usuario,
    [Parameter(Mandatory)][string]$Arquivo,
    [switch]$ConfirmarBancoDescartavel
)
$ErrorActionPreference = 'Stop'
if (-not $ConfirmarBancoDescartavel) { throw 'Confirme explicitamente que o destino é exclusivamente de testes.' }
if ($Banco -cnotmatch '\Acasa_restore_[a-z0-9_]{1,50}\z') { throw 'Destino deve ser uma base descartável casa_restore_* (até 63 caracteres).' }
if (-not (Test-Path -LiteralPath $Arquivo -PathType Leaf)) { throw 'Arquivo de backup inexistente.' }
$arquivoCompleto = (Resolve-Path -LiteralPath $Arquivo).Path
if ((Get-Item -LiteralPath $arquivoCompleto).Length -eq 0) { throw 'Arquivo de backup vazio.' }
$restore = (Get-Command pg_restore -ErrorAction Stop).Source
$psql = (Get-Command psql -ErrorAction Stop).Source
$dump = (Get-Command pg_dump -ErrorAction Stop).Source
Write-Output (& $restore --version)
& $restore --list $arquivoCompleto | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Arquivo não reconhecido pelo pg_restore.' }
$conexao = @('-h', $HostBanco, '-p', $Porta, '-U', $Usuario, '-d', $Banco)
$inventario = [IO.Path]::GetTempFileName()
$hostaddrAnterior = [Environment]::GetEnvironmentVariable('PGHOSTADDR', 'Process')
$servicoAnterior = [Environment]::GetEnvironmentVariable('PGSERVICE', 'Process')
try {
    if (Test-Path Env:PGHOSTADDR) { Remove-Item -LiteralPath Env:PGHOSTADDR }
    if (Test-Path Env:PGSERVICE) { Remove-Item -LiteralPath Env:PGSERVICE }
    # O inventário de esquema não verifica objetos grandes nem configurações locais de papéis.
    $consulta = @'
SELECT datname || '|' || (SELECT count(*) FROM pg_catalog.pg_largeobject_metadata)
       || '|' || (SELECT count(*) FROM pg_catalog.pg_db_role_setting WHERE setdatabase = d.oid)
FROM pg_catalog.pg_database d
WHERE datname = current_database()
'@
    $estado = & $psql -X -At -v ON_ERROR_STOP=1 @conexao -c $consulta
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível validar a base de destino.' }
    if ($estado -ne "$Banco|0|0") { throw 'Base de restauração não está vazia ou não corresponde ao destino.' }

    & $dump @conexao --schema-only --format=custom --file=$inventario
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível inventariar o esquema da base de destino.' }
    $catalogo = & $restore --list $inventario
    if ($LASTEXITCODE -ne 0 -or -not $catalogo) { throw 'Não foi possível validar o inventário do destino.' }
    if ($catalogo | Where-Object { $_ -notmatch '^\s*(;|$)' }) {
        throw 'A base de restauração já contém objetos ou alterações em seu esquema.'
    }

    & $restore --exit-on-error --single-transaction --no-owner --no-privileges @conexao $arquivoCompleto
    if ($LASTEXITCODE -ne 0) { throw 'Restauração falhou; a transação foi revertida.' }
} finally {
    if ($null -ne $hostaddrAnterior) { $env:PGHOSTADDR = $hostaddrAnterior }
    if ($null -ne $servicoAnterior) { $env:PGSERVICE = $servicoAnterior }
    Remove-Item -LiteralPath $inventario -Force
}
Write-Output "RESTAURADO_EM=$Banco"
Write-Output "SHA256_ORIGEM=$((Get-FileHash -LiteralPath $arquivoCompleto -Algorithm SHA256).Hash)"
