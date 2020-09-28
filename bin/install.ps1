
########################################################################################
# ARGUMENTS
########################################################################################

Param (
    [switch] $system,
    [Alias('i')][string] $identity,
    [Alias('j')][string] $jar
)

########################################################################################
#  DEFAULTS
########################################################################################

if (!$identity) {
    $identity = "clojupyter-$(bin\version)"
}

if (!$jar) {
    $jar = "target\clojupyter-$(bin\version).jar"
}

if ($system) {
    $datadir = "$env:ProgramData\jupyter\kernels"
    $libdir = "$env:ProgramFiles"
}
elseif ($env:JUPYTER_DATA_DIR) {
    $datadir = "$env:JUPYTER_DATA_DIR\kernels"
    $libdir = "$env:LocalAppData\Programs"
}
else {
    $datadir = "$env:AppData\jupyter\kernels"
    $libdir = "$env:LocalAppData\Programs"
}

#########################################################################################
# FAIL CONDITIONS
#########################################################################################

if (! $(Test-Path -PathType Leaf $jar)) {
    Write-Error "Can't find $jar"
    exit 10
}

if (Test-Path -PathType Container "$datadir\$identity") {
    Write-Error "$identity is already installed in $datadir"
    exit 30
}

if (Test-Path -PathType Container "$libdir\$identity") {
    Write-Error "$identity is already installed in $libdir"
    exit 31
}

#########################################################################################
# INSTALLING
#########################################################################################

Write-Host Installing $identity
mkdir $libdir\$identity\plugins\enabled | Out-Null
mkdir $datadir\$identity | Out-Null
Copy-Item $jar $libdir\$identity\$identity.jar
Copy-Item -Recurse target\lib $libdir\$identity

Push-Location $libdir\$identity\lib
New-Item -ItemType SymbolicLink -Path "$identity.jar" -Target "..\$identity.jar" | Out-Null
Pop-Location

Copy-Item resources\clojupyter\assets\logo-64x64.png $datadir\$identity
$kernel = bin/kernel.json.ps1 -i $identity -libdir $libdir
$UTF8Only = New-Object System.Text.Utf8Encoding($false)
[System.IO.File]::WriteAllLines("$datadir\$identity\kernel.json", $kernel, $UTF8Only)
