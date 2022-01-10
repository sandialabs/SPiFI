#!/usr/bin/env groovy
/*
 *  SEMS Pipeline Framework Infrastructure (SPiFI)
 *
 * Copyright 2020 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
 * Under the terms of Contract DE-NA0003525 with NTESS, the U.S. Government retains
 * certain rights in this software.
 *
 * LICENSE (3-Clause BSD)
 * ----------------------
 * Copyright 2020 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
 * Under the terms of Contract DE-NA0003525 with NTESS, the U.S. Government retains
 * certain rights in this software.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact
 * -------
 * William C. McLendon III (wcmclen@sandia.gov)
 */

/**
 * JenkinsTools.groovy
 *
 * Provides some helper functions for common Jenkins items and tools
 *
 * Jenkins Pipeline Usage:
 *
 *     def jtools = new gov.sandia.sems.spifi.JenkinsTools()
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2019-12-02
 */
package gov.sandia.sems.spifi



/**
 * Pretty print a Groovy exception StackTrace
 *
 * @param env       [REQUIRED] Object - Jenkins environment (use 'this' from the Jenkins pipeline).
 * @param exception [REQUIRED] Exception - Groovy exception object.
 *
 * @return String containing beautified stacktrace.
 */
static def spifi_get_exception_stacktrace_pretty(Map args)
{
    if( !args.containsKey("env") )
    {
        throw new Exception("[SPiFI ERROR]> Missing required parameter 'env' to spifi_get_exception_stackgrace_pretty()")
    }
    if( !args.containsKey("exception") )
    {
        throw new Exception("[SPiFI ERROR]> Missing required parameter 'exception' to spifi_get_exception_stackgrace_pretty()")
    }

    def env = args.env
    def ex  = args.exception

    String output = ""

    def strace = ex.getStackTrace()
    strace.each
    {
        output += "[SPiFI]> \u2757 " +
                  sprintf("%-70s %s\n", [it.getClassName().toString(), it.getMethodName().toString()])
    }
    return output.trim()
}



/**
 * Extracts a value from a Map if it exists
 *
 * If a value exists in a Groovy map (params) then we return it. Otherwise, we can optionally
 * throw an error if its missing or set it with a default value
 *
 * @param env      [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline).
 * @param params   [REQUIRED] Object  - Jenkins job params object (params in a jenkins job).
 * @param key      [REQUIRED] String  - Name of the parameter that we're searching for.
 * @param default  [OPTIONAL] Object  - Default value to return if parameter does not exist. Default="".
 * @param required [OPTIONAL] Boolean - If true, this parameter is required and an error will be thrown
 *                                      if it's not found. Default=false
 * @param in_place [OPTIONAL] Boolean - If true AND the parameter value is missing, adds the default value to params.
 *                                      Default: false
 *
 * @return Object containing the value of the parameter returned.
 */
static def spifi_checked_get_parameter(Map args)
{
    if( !args.containsKey("env") )
    {
        throw new Exception("[SPiFI ERROR]> Missing required parameter 'env' to checed_get_parameter()")
    }

    def     env           = args.env
    def     default_value = null
    Boolean is_required   = false
    Boolean in_place      = false

    env.println "[SPiFI]> checked_get_parameter()"

    if( !args.containsKey("params") )
    {
        throw new Exception("[SPiFI ERROR]> Missing required parameter 'params' to checked_get_parameter()")
    }
    if( !args.containsKey("key") )
    {
        throw new Exception("[SPiFI ERROR]> Missing required parameter 'key' to checked_get_parameter()")
    }
    if( args.containsKey("default") )
    {
        default_value = args.default
    }
    if( args.containsKey("required") )
    {
        is_required = args.required
    }
    if( args.containsKey("in_place") )
    {
        in_place = args.in_place
    }
    env.println "[SPiFI]> - key      : ${args.key}\n" +
                "[SPiFI]> - default  : ${default_value}\n" +
                "[SPiFI]> - required : ${is_required}\n" +
                "[SPiFI]> - in_place : ${in_place}"

    if( args.params.containsKey(args.key) )
    {
        try
        {
            return args.params[args.key]
        }
        catch(ex)
        {
            throw new Exception("[SPiFI ERROR]> An error occurred trying to retrieve `${args.key}` in checked_get_parameter()\n${ex}")
        }
    }
    else
    {
        if( is_required )
        {
            throw new Exception("[SPiFI ERROR]> Missing required parameter '${args.key}'.")
        }
        if(in_place)
        {
            args.params[args.key] = default_value
        }
        return default_value
    }
}




return this

