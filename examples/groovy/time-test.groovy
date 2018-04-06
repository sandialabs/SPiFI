#!/usr/bin/env groovy

/**
 * Convert a time string (in seconds) into "xxh xxm xxs"
 *
 */
String stringifyTime(Float t_seconds)
{
    List<String> t_list = []

    if(t_seconds >= 604800)
    {
        Integer num_weeks = t_seconds/604800
        t_seconds -= num_weeks * 604800
        t_list << sprintf("%dw", [num_weeks])
    }
    if(t_seconds >= 86400)
    {
        Integer num_days = t_seconds / 86400
        t_seconds -= num_days * 86400
        t_list << sprintf("%dd", [num_days])
    }
    if(t_seconds >= 3600)
    {
        Integer num_hours = t_seconds / 3600
        t_seconds -= num_hours * 3600
        t_list << sprintf( "%dh", [num_hours])
    }
    if(t_seconds >= 60)
    {
        Integer num_minutes = t_seconds / 60
        t_seconds -= num_minutes * 60
        t_list << sprintf("%dm", [num_minutes])
    }
    // t_list << sprintf( "%ds", [(Integer)(t_seconds%60)])
    t_list << sprintf("%.2fs", [t_seconds])

    return t_list.join(" ")
}

Float t_seconds = 604800 + 86400 + 2*3600 + 65 + 0.2
println "t_seconds = ${t_seconds}"
println "t_hms     = ${stringifyTime(t_seconds)}"


