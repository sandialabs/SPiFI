#!/usr/bin/env groovy
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





