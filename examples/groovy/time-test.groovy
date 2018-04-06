#!/usr/bin/env groovy

Float t_seconds = 60


String stringifyTime(Float t_seconds)
{
    List<String> t_list = []

    if(t_seconds >= 3600)
    {
        t_list << sprintf( "%dh", [(int)(t_seconds/3600)])
    }
    if(t_seconds >= 60)
    {
        t_list << sprintf("%dm", [(int)(t_seconds%3600/60)])
    }
    t_list << sprintf( "%ds", [(int)(t_seconds%60)])

    return t_list.join(" ")
}

println "t_seconds = ${t_seconds}"
println "t_hms     = ${stringifyTime(t_seconds)}"
