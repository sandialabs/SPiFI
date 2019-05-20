#!/usr/bin/env groovy

def list = ["Daffy", "Bugs", "Elmer", "Tweety", "Sylvester", "Yosemite"]

println list

println "Test list.first() == 'Daffy'"
assert list.first() == "Daffy"

println "Test list.last()  == 'Yosemite'"
assert list.last()  == "Yosemite"

println "Test list[-1]     == 'Yosemite'"
assert list[-1] == "Yosemite"

println "Test list[-2]     == 'Sylvester'"
assert list[-2] == "Sylvester"


