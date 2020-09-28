Select-String -Pattern '\(defproject clojupyter "(.+)"' -Path project.clj | % { $_.Matches.Groups[1].Value }
