#!/usr/bin/env groovy

def list = ["Daffy", "Bugs", "Elmer", "Tweety", "Sylvester", "Yosemite"]

println list

println 'Bugs' == list.find { it == 'Bugs' }
println 'Buts' == list.find { it == 'Bugs' }


def f = "ASCII"

assert f == ['ASCII','HTML','MARKDOWN'].find { it == f }



String s = sprintf("%-10s!", ["txt"])

println "[[${s}]]"


try
{
  throw new Exception("TEST")
} catch (e) {
  println "Caught: ${e}"
}

