
Param (
    [Alias('i')][string]$identity,
    [Parameter(Mandatory=$true)][string]$libdir
)

if (!$identity) {
  $version = bin\version.ps1
  $identity = "clojupyter-$version"
}

$libdir = $libdir.Replace("\", "/")

Write-Output @"
{"env": {"CLASSPATH": "$libdir/$identity/$identity.jar;$libdir/$identity/plugins/enabled/*;`${CLASSPATH}"},
 "argv": ["java", "clojupyter.kernel.core", "{connection_file}"],
 "display_name": "Clojure ($identity)",
 "language": "clojure",
 "interrupt_mode": "message"}
"@
