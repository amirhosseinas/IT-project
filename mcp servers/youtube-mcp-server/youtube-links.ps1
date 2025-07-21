# YouTube Links Generator PowerShell Script

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "YouTube Links Generator" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host

# Check if Node.js is installed
$nodeExe = $null
try {
    $nodeExe = Get-Command node -ErrorAction Stop
    $nodeExe = "node"
} catch {
    Write-Host "Node.js is installed but not in PATH." -ForegroundColor Yellow
    Write-Host "Using direct path to Node.js executable." -ForegroundColor Yellow
    $nodeExe = "C:\Program Files\nodejs\node.exe"
}

$continueSearching = $true

while ($continueSearching) {
    # Get search query from arguments or prompt user
    $searchQuery = $args -join " "
    if ([string]::IsNullOrEmpty($searchQuery) -or $args.Count -eq 0) {
        $searchQuery = Read-Host "Enter search query"
    } else {
        # Clear args after first use so we prompt for new queries in subsequent loops
        $args = @()
    }

    Write-Host
    Write-Host "Generating links for: `"$searchQuery`"" -ForegroundColor Green
    Write-Host

    # Run the Node.js script
    & $nodeExe youtube-links.js "$searchQuery"

    Write-Host
    $choice = Read-Host "Would you like to search again? (Y/N)"
    
    if ($choice -notmatch "^[Yy]") {
        $continueSearching = $false
    }
}

Write-Host "Thank you for using YouTube Links Generator!" -ForegroundColor Cyan 