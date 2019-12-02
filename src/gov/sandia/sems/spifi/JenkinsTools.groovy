#!/usr/bin/env groovy
/**
 * JenkinsTools.groovy
 *
 * Provides some helper functions for some common Jenkins actions
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
 * @param env [REQUIRED] Object - Jenkins environment (use 'this' from the Jenkins pipeline).
 */
// Parameters:
// - args     [REQUIRED] - jenkins job params object (params in a jenkins job)
// - key      [REQUIRED] - parameter key to search for
// - default  [OPTIONAL] : Default=""
// - required [OPTIONAL] : Default=false
// TODO: Add this to SPiFI
def checked_get_parameter(Map params)
{
    def default_value = null
    def is_required   = false

    if(!args.containsKey("env"))
    {
        throw new Exception("[SPiFI]> ERROR: Missing required parameter: env")
    }
    def env = args.env
    env.println "[SPiFI]> checked_get_parameter(${args})" // 268F

    if( !args.containsKey("params") )
    {
        throw new Exception("ERROR: Missing required parameter 'args' to check_parameter_and_exit_if_missing")
    }
    if( !args.containsKey("key") )
    {
        throw new Exception("ERROR: Missing required parameter 'key' to check_parameter_and_exit_if_missing")
    }
    if( args.params.containsKey(args.default) )
    {
        default_value = args.default
    }
    if( args.params.containsKey(args.required) )
    {
        is_required = parmas.required
    }

    if( args.params.containsKey(args.key) )
    {
        try
        {
            return args.params[args.key]
        }
        catch(ex)
        {
            throw new Exception("An error occurred trying to retrieve `${args.key}` in checked_get_parameter()")
        }
    }
    else
    {
        if( is_required )
        {
            throw new Exception("Missing required parameter '${args.key}'.")
        }
        return default_value
    }
}


return this
