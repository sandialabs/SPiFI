#!/usr/bin/env groovy

Integer a = 100
Integer b =  50
Integer c =  33
Float   d =  33.3

println "a: ${a}"
println "b: ${b}"
println "c: ${c}"
println "d: ${d}"

println "a/b: ${a/b}"
println "a/c: ${a/c}"
println "a/d: ${a/d}"

Float cc = a/c

println "cc: ${cc}"
println sprintf("%.2f", [cc])
printf("%.2f\n", [cc])

