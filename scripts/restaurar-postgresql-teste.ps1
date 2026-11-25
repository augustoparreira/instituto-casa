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
if ($Banco -cnotmatch '^casa_restore_[a-z0-9_]+$') { throw 'Destino deve ser uma base descartável casa_restore_*.' }
if (-not (Test-Path -LiteralPath $Arquivo -PathType Leaf)) { throw 'Arquivo de backup inexistente.' }
$arquivoCompleto = (Resolve-Path -LiteralPath $Arquivo).Path
if ((Get-Item -LiteralPath $arquivoCompleto).Length -eq 0) { throw 'Arquivo de backup vazio.' }
$restore = (Get-Command pg_restore -ErrorAction Stop).Source
$psql = (Get-Command psql -ErrorAction Stop).Source
Write-Output (& $restore --version)
& $restore --list $arquivoCompleto | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Arquivo não reconhecido pelo pg_restore.' }
$consulta = "SELECT current_database() || '|' || (SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%' AND c.relkind IN ('r','p','v','m','S','f'))"
$estado = & $psql -X -At -v ON_ERROR_STOP=1 -h $HostBanco -p $Porta -U $Usuario -d $Banco -c $consulta
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível validar a base de destino.' }
if ($estado -ne "$Banco|0") { throw 'Base de restauração não está vazia ou não corresponde ao destino.' }
& $restore --exit-on-error --single-transaction --no-owner --no-privileges -h $HostBanco -p $Porta -U $Usuario -d $Banco $arquivoCompleto
if ($LASTEXITCODE -ne 0) { throw 'Restauração falhou; a transação foi revertida.' }
Write-Output "RESTAURADO_EM=$Banco"
Write-Output "SHA256_ORIGEM=$((Get-FileHash -LiteralPath $arquivoCompleto -Algorithm SHA256).Hash)"
