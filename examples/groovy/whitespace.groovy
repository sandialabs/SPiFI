#!/usr/bin/env groovy

String s = "This is a \n" +
           "multiline\n" +
           "string...\n"

println "---"
println s
println "---"
println s.replaceAll("\\s\$", "")
println "---"

String f = """
           <H1>Header</H1>
           Profound message goes here
           """.stripIndent()

println f

