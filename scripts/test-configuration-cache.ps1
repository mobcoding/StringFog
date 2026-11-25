param(
    [Parameter(Mandatory)] [string] $ProjectDir,
    [Parameter(Mandatory)] [string] $AsmTask
)

$ErrorActionPreference = 'Stop'
$gradleArgs = @($AsmTask, '--configuration-cache', '--no-daemon', '--rerun-tasks',
    '--no-build-cache', '--stacktrace', '--console=plain')
Push-Location -LiteralPath $ProjectDir
try {
    for ($run = 1; $run -le 2; $run++) {
        $output = & .\gradlew.bat @gradleArgs 2>&1
        $exitCode = $LASTEXITCODE
        $output | Write-Output
        if ($exitCode -ne 0) {
            throw "Gradle run $run failed with exit code $exitCode."
        }
        if ($run -eq 2) {
            $lines = $output | ForEach-Object { $_.ToString().Trim() }
            if ('Configuration cache entry reused.' -notin $lines) {
                throw 'The second build did not reuse the configuration cache.'
            }
            if ("> Task $AsmTask" -notin $lines) {
                throw "The second build did not execute $AsmTask."
            }
        }
    }
} finally {
    Pop-Location
}
