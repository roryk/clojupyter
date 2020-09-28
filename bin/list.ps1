
########################################################################################
#  DEFAULTS
########################################################################################

$UserDataPath = if ($env:JUPYTER_DATA_DIR) {
                    "$env:JUPYTER_DATA_DIR\kernels"
                 }
                 else {
                    "$env:AppData\jupyter\kernels"
                 }

$UserLibPath = "$env:LocalAppData\Programs"

$SystemDataPath = "$env:ProgramData\jupyter\kernels"

$SystemLibPath = "$env:ProgramFiles"

########################################################################################
#  ARGUMENTS & FAIL CONDITIONS
########################################################################################

if ($args.count -gt 1) {
    Write-Error ("Wrong number of arguments (" + $args.count + ") passed to program")
    exit 1
}

$all = @()
foreach ($dir in $(Get-ChildItem -ErrorAction SilentlyContinue $UserDataPath, $SystemDataPath)) {
    if ($(Get-Content -ErrorAction SilentlyContinue  "$($dir.FullName)\kernel.json" | ConvertFrom-JSON | Select-Object -ExpandProperty display_name) -match '^Clojure \((.+)\)$') {
        $Plugins = @()
        $EnabledPlugins = @()
        $LibPath = ""
        $name = $matches[1]
        if (Test-Path "$UserLibPath\$dir\plugins") {
            $LibPath = "$UserLibPath\$dir"
        }
        elseif (Test-Path "$SystemLibPath\$dir\plugins") {
            $LibPath = "$SystemLibPath\$dir"
        }
        if ($LibPath) {
            $Plugins = Get-ChildItem -File "$LibPath\plugins" | Select-Object -ExpandProperty Basename
            $EnabledPlugins = Get-ChildItem -File "$LibPath\plugins\enabled" | Select-Object -ExpandProperty Basename
        }
        $all += @{Identity = $name; DataPath = $dir.FullName; Plugins = $Plugins; EnabledPlugins = $EnabledPlugins; LibPath = $LibPath}
    }
}

foreach ($res in $all) {
    if ($args[0]) {
        if ($res["Identity"] -match $args[0]) {
            New-Object PSObject -Property $res
        }}
    else {
        New-Object PSObject -Property $res
    }
}
