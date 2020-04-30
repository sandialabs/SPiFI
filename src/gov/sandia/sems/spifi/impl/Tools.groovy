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
 * Tools.groovy
 *
 * Utility functions used internally within SPiFI.
 *
 * Ordinarily these will not be exposed to users of the library.
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2020-01-24
 */
package gov.sandia.sems.spifi.impl



/**
 * Utility to provide a basic parameter check for functions.
 *
 * Typical use pattern:
 *
 *   ```groovy
 *   def my_function(Map args)
 *   {
 *       Map args_expected = [ "env":     [ option: "R" ],
 *                             "command": [ option: "R" ],
 *                             "dry_run": [ option: "O" ],
 *                             "yeet":    [ option: "D" ]
 *                           ]
 *       if( !args.containsKey("env") )
 *       {
 *           throw new Exception("ERROR: Missing required parameter, env")
 *       }
 *
 *       Boolean OK = parameter_check(env: args.env, params_received: args, params_expected: args_expected)
 *       if (!OK)
 *       {
 *           throw new Exception("ERROR: parameter check failed in my_function")
 *       }
 *
 *       // Do stuff
 *
 *   }
 *   ```
 *
 * Parameters
 * ----------
 *
 * @param env             [REQUIRED] Object  - Pass in the Jenkins environment ('this') object.
 * @param params_expected [REQUIRED] Map     - Expected paramters. A Map of Maps:
 *                                             parameter_name: [ option: "<Option Type>" ]
 *                                             Where <Option Type> is one of:
 *                                             - "R": Required parameter
 *                                             - "O": Optional parameter
 *                                             - "D": Deprecated parameter
 * @param params_received [REQUIERD] Map     - Standard parameter list containing Key: value pairs.
 * @param verbose         [OPTIONAL] Boolean - Print out verbose messages (for debugging). Default: false
 *
 * @return Boolean value that if true signals parameter check passed, false if otherwise.
 *
 */
static Boolean spifi_parameter_check(Map args)
{
    Boolean output  = true
    Boolean verbose = false

    assert args.containsKey("env"), "Missing required parameter: env"

    if( !args.containsKey("params_received") )
    {
        args.env.println "Missing required parameter: params_received"
        output = false
    }
    if( !args.containsKey("params_expected") )
    {
        args.env.println "Missing required parameter: params_expected"
        output = false
    }
    if( args.containsKey("verbose") )
    {
        verbose = args.verbose
    }

    if( output )
    {
        args.params_expected.each
        { key,value ->

            if(verbose)
            {
                args.env.println "[SPiFI]> Checking expected parameter: (${value.option}) ${key}"
            }

            if( "R" == value.option )
            {
                if(!args.params_received.containsKey(key))
                {
                    args.env.println "[SPiFI ERROR]> Missing required parameter: ${key}"
                    output = false
                }
            }
            else if ("O" == value.option )
            {
                // no-op
            }
            else if ("D" == value.option )
            {
                if(args.params_received.containsKey(key))
                {
                   args.env.println "[SPiFI WARNING]> Parameter '${key}' is marked for DEPRECATION and will be removed in the next major release"
                }
            }
            else
            {
                throw new Exception("SPiFI ERROR: Unknown parameter type provided. ${value.option} is not one of [ R, O, D ]")
            }
        }
        // Print out warning for unexpected parameters
        args.params_received.keySet().each
        { key ->
            if( !args.params_expected.containsKey(key) )
            {
                args.env.println "[SPiFI WARNING]> Unexpected parameter: '${key}'"
            }
        }
    }
    return output
}  // end parameter_check



static String spifi_convert_seconds_to_hms(def duration)
{
    String output = "";
    Float seconds = duration as Float

    Integer days    = duration / 86400 as Integer
    duration -= days * 86400
    Integer hours   = duration / 3600  as Integer
    duration -= hours * 3600
    Integer minutes = duration / 60    as Integer
    duration -= minutes * 60

    if(days>0)
    {
        output = sprintf("%02d:", [days])
    }
    if(output != "" || hours>0)
    {
        output += sprintf("%02d:", [hours])
    }
    if(output != "" || minutes>0)
    {
        output += sprintf("%02d:", [minutes])
    }
    output += sprintf("%05.2f", [duration])

    return output
}

