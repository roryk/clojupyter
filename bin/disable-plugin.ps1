
########################################################################################
#  ARGUMENTS
########################################################################################

Param (
    [Alias('k')][string] $kernel,
    [Parameter(Mandatory=$true, Position=0)][string] $target
)

if (!$kernel) {
    $kernel = "clojupyter-$(bin/version)"
}

$kt = bin/list ^${kernel}$

########################################################################################
#  DEPENDENCIES
########################################################################################

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-Dependencies {
    Param (
        $jar
    )
    $jarfile = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    $manifest = $jarfile |
                Select-Object -ExpandProperty Entries |
                Where-Object {$_.Name -eq "MANIFEST.MF"}
    $file = New-TemporaryFile
    $file.Delete()
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($manifest, $file.FullName)
    $jarfile.Dispose()
    $CP_key = $false
    $deps = foreach ($line in $(Get-Content $file)) {
                Switch -Regex ($line) {
                    "^Class-Path: (.+)$" {
                        $CP_key = $true
                        Write-Output $matches[1]
                    }
                    "^[^:]+$" {
                        if ($CP_key) {
                                Write-Output $line.Substring(1)
                            }
                    }
                    default {
                        $CP_key = $false
                    }
                }
            }
    foreach ($dep in $($deps -join '').split(" ")) {
        $file = Get-ChildItem "$($jar.Directory)\$dep"
        if ($file.target) {
            Get-ChildItem $file.target
        }
        else {
            $file
        }
    }
}

function disable {
    Param (
        $plugin
    )
    Write-Host "Disabling plugin $plugin"
    Push-Location "$($kt.LibPath)\plugins\enabled"
    Remove-Item "$plugin.jar" | Out-Null
    Pop-Location
}

$match = @()
foreach ($plugin in $kt.EnabledPlugins) {
    if ($plugin -match $target) {
        $match += $plugin
    }
    elseif (Get-Dependencies $(Get-ChildItem "$($kt.LibPath)\plugins" "$plugin.jar") |
            Where-Object { $kt.EnabledPlugins -contains $_.Basename } |
            Where-Object { $_.Basename -match $target }) {
        $match += $plugin
    }
}

if (!$match) {
    Write-Error "No plugin found that matches $target"
    exit 31
}

########################################################################################
#  DISABLING
########################################################################################

foreach ($m in $($match | Sort-Object | Get-Unique)) {
    disable $m
}
