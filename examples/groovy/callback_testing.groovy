#!/usr/bin/env groovy

def test_a(Map a=[:], Map w=[:], Closure callback = {aa=[:],bb=[:] -> })
{
    println "test_a(a,w,callback):"

    Map a_local = a.getClass().newInstance(a)   // Deep copy a to make it immutable.

    println "  --- a: ${a}"
    println "  --- w: ${w}"
    callback(a_local, w)
    println "  --- a: ${a}"
    println "  --- w: ${w}"
    println ""
}


String exclamation = "Zoinks!"


Closure cb = {
    Map aa=[:], Map ww=[:] ->

    println "  callback(a,w):"

    println "    --- CB (aa): ${aa}"
    println "    --- CB (ww): ${ww}"

    aa["bif"] = "zoop!"      // changes a_local in test_a but won't affect args in the global scope
    ww["baz"] = exclamation  // modifies workspace in the global scope.

    println "    --- CB (aa): ${aa}"
    println "    --- CB (ww): ${ww}"

}




Map args = [:]
Map workspace = [:]

args["foo"] = "A"

println "args: " + args
println "wksp: " + workspace

println ""
exclamation = "Skol!"
//test_a(args, workspace, cb)
//test_a(args, workspace)
//test_a(args)
test_a()

println "args: " + args
println "wksp: " + workspace

println ""
exclamation = "Parlay!"     // will change workspace['baz'] inside the cb closure that is called by test_a.
test_a(args, workspace, cb)

println "args: " + args
println "wksp: " + workspace
