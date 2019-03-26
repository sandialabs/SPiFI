#!/usr/bin/env groovy

// Test the FIND closure
List<String> L = ["Apple", "Orange", "Pear", "Mango"]

String key = "Pear"


println "List: ${L}"

Boolean found = L.find 
{ line ->
    println "-> " + line
    if( (line =~ /${key}/).getCount() > 0 )
        return true
    return false
} 
found = found == true

println "Found? ${found}"


