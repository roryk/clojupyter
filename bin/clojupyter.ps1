
if ((Get-Command clojure) -and (Test-Path "deps.edn")) {
    $env:CLASSPATH = $(foreach ($dep in $(clojure -Spath).split(";")) {
                            Resolve-Path "$dep"
                     }) -join ";"
}
elseif (Get-Command lein) {
    $env:CLASSPATH = lein classpath
}
else {
    Write-Error "Can't find lein or clojure on PATH."
    exit 2
}

jupyter $args
