$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$versionTag = "latest"
$baseImageName = "order-orchestrator"
$imageName = "$baseImageName`:$versionTag"

Write-Host "Building Docker image: $imageName ..."
docker build -t $imageName -f (Join-Path $repoRoot "order-orchestrator\Dockerfile") $repoRoot

Write-Host "Done. Image built:"
docker images $baseImageName

Write-Host ""
Write-Host "Run container with:"
Write-Host "docker run -it $imageName"
