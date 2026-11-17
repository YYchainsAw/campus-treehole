$ErrorActionPreference = 'Stop'

$backendDir = Split-Path -Parent $PSScriptRoot
$localConfig = Join-Path $backendDir 'application-local.properties'
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'

if (Test-Path -LiteralPath $localConfig) {
    throw 'application-local.properties already exists. Nothing was changed.'
}
if (-not (Test-Path -LiteralPath $mysql)) {
    throw 'MySQL 8.4 client was not found at the expected installation path.'
}

function ConvertTo-JavaPropertyValue([string] $value) {
    $builder = [System.Text.StringBuilder]::new()
    # Spring resolves ${...} in property values; preserve a literal prefix.
    $value = $value.Replace('${', '\${')
    foreach ($character in $value.ToCharArray()) {
        $code = [int] $character
        if ($character -eq '\') {
            [void] $builder.Append('\\')
        } elseif ($code -lt 32 -or $code -gt 126) {
            [void] $builder.AppendFormat('\u{0:X4}', $code)
        } elseif (' =:#!'.Contains([string] $character)) {
            [void] $builder.Append('\').Append($character)
        } else {
            [void] $builder.Append($character)
        }
    }
    return $builder.ToString()
}

$securePassword = Read-Host 'Enter the local MySQL root password (input is hidden)' -AsSecureString
$passwordPointer = [IntPtr]::Zero
try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    if ([string]::IsNullOrEmpty($password)) {
        throw 'Password must not be empty.'
    }

    # The password is passed to the client through this process environment,
    # never through the command line or the script output.
    $env:MYSQL_PWD = $password
    & $mysql --no-defaults --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --connect-timeout=10 --execute='CREATE DATABASE IF NOT EXISTS campus_treehole CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
    if ($LASTEXITCODE -ne 0) {
        throw 'MySQL connection or database creation failed. Local config was not created.'
    }

    $escapedPassword = ConvertTo-JavaPropertyValue $password
    $content = "# Local only. This file contains a password and must not be committed.`r`nspring.datasource.username=root`r`nspring.datasource.password=$escapedPassword`r`nspring.profiles.active=local`r`nserver.address=127.0.0.1`r`n"
    [System.IO.File]::WriteAllText($localConfig, $content, [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Database is ready and local configuration was created.'
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    Remove-Variable password, escapedPassword, content -ErrorAction SilentlyContinue
}
