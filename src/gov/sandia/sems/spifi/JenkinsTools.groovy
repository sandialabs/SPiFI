#!/usr/bin/env groovy
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
 * Load a Jenkins parameter if it exists.
 * 
 * @param env      [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline).
 * @param params   [REQUIRED] Object  - Jenkins job params object (params in a jenkins job).
 * @param key      [REQUIRED] String  - Name of the parameter that we're searching for.
 * @param default  [OPTIONAL] String  - Default value to return if parameter does not exist. Default="".
 * @param required [OPTIONAL] Boolean - If true, this parameter is required and an error will be thrown
 *                                      if it's not found. Default=false
 *
 * @return Object containing the value of the parameter returned.
 */
def checked_get_parameter(Map args)
{
    if( !args.containsKey("env") )
    {
        throw new Exception("[SPiFI ERROR]> Missing required parameter 'env' to checed_get_parameter()")
    }

    def     env           = args.env
    def     default_value = null
    Boolean is_required   = false

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
    env.println "[SPiFI]> - key      : ${args.key}\n" +
                "[SPiFI]> - default  : ${default_value}\n" +
                "[SPiFI]> - required : ${is_required}"

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
        return default_value
    }   
}



return this
