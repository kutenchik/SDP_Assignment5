$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $sources = Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object FullName
    New-Item target/classes -ItemType Directory -Force | Out-Null
    & javac --release 17 -d target/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
    & java -cp target/classes org.example.Main
    if ($LASTEXITCODE -ne 0) { throw 'Application failed' }
} finally {
    Pop-Location
}
