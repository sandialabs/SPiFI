#!/usr/bin/env groovy

def M = [:]

M["key1"] = "foo"

println "M    : ${M}" 


def M_tmp = M.getClass().newInstance(M)
println "M_tmp: ${M_tmp}"

M_tmp.key1 = "bar"
println "-----------"
println "M    : ${M}" 
println "M_tmp: ${M_tmp}"

