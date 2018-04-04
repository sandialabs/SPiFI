#!/usr/bin/env groovy


class parameters
{
  String label  = "__REQUIRED__"
  List   params = null
}


Map m = [:]

m.label = "foobar"
m.params = ["A","B","C"]

println m

def p = m as parameters

println p.label
println p.params


