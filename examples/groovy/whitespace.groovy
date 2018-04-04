#!/usr/bin/env groovy

String s = "This is a \n" +
           "multiline\n" +
           "string...\n"

println "---"
println s
println "---"
println s.replaceAll("\\s\$", "")
println "---"

