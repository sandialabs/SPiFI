#!/usr/bin/env groovy
// Example showing how you can use a closure as
// the last parameter in a function to do interesting
// stuff.
//
// See https://groovy-lang.org/closures.html for more information
// on closures.
///////////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////
//
// V A R I A T I O N   1 :   B A S I C 
//
//////////////////////////////////////////////////////////////////////

// Simple function taking arguments as a map and the 
// Closure as last parameter lets you call the function
// like this:
// 
// my_function(arg1: val1, arg2: val2, ... argN: valN)
// {
//    /* this is the closure */
// }
def my_function_1(Map args, Closure c)
{
    Integer count = 10
    count = args.containsKey("count") ? args.count : count

    for(i=0; i<count; i++)
    {
        c()
    }
}

println "Variant 1:"
my_function_1(count: 4)
{ 
    println "Hello!"
}


//////////////////////////////////////////////////////////////////////
//
// V A R I A T I O N   2 :   P O S I T I O N A L   P A R A M E T E R S
//
//////////////////////////////////////////////////////////////////////

// You can also pass parameters into the closure
def my_function_2(Map args, Closure c)
{
    Integer count = 10
    count = args.containsKey("count") ? args.count : count

    for(i=0; i<count; i++)
    {
        c(i, count)
    }
}

println ""
println "Variant 2:"
my_function_2(count: 4)
{ a,b ->                          // <----- Here's how you get the parameters
    println "Hello! ${a} / ${b}"
}


//////////////////////////////////////////////////////////////////////
//
// V A R I A T I O N   3 :   P A R A M E T E R S   A S   M A P
//
//////////////////////////////////////////////////////////////////////

// The parameter to a closure can also be a map...
def my_function_3(Map args, Closure c)
{
    Integer count = 10
    count = args.containsKey("count") ? args.count : count

    for(i=0; i<count; i++)
    {
        c(index: i, max: count)
    }
}

println ""
println "Variant 3:"
my_function_3(count: 4)
{ m ->
    println "Hello! ${m.index} / ${m.max}"
}







