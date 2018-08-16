#!/usr/bin/env groovy
/**
 * Git.groovy
 *
 * Git plugin function wrapper for SPiFI
 * Mainly the use of this is as a convenience function for pipelines since it's
 * fairly common that github, gitlab, etc. might fail to clone if the network
 * is experiencing a lot of lag, etc. so some retries are handy to have.
 *
 * Example Usage:
 *
 *    def git_spifi = new gov.sandia.sems.spifi.Git()
 *    git_spifi.clone(env: this, url: "git@github.com:trilinos/Trilinos.git")
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-04
 *
 */
package gov.sandia.sems.spifi;



/**
 * Clone a git repository with retries, timeout, and directory location provided.
 *
 * @param env            [REQUIRED] Object  The jenkins environment in the jenkins file.
 *                                          This should be the `this` object.
 * @param url            [REQUIRED] String  The URL to the git repository.
 *                                          i.e., git@github.com:trilinos/Trilinos.git
 * @param branch         [OPTIONAL] String  The branch name to check out.
 *                                          Default: master
 * @param credentialsId  [OPTIONAL] String  A Jenkins credentialsID if required.
 *                                          Default: null
 * @param dir            [OPTIONAL] String  The directory to clone into, relative to
 *                                          dir at the current scope.
 *                                          Default: ""
 * @param retries        [OPTIONAL] Integer Number of retries to attempt.
 *                                          Default: 0
 * @param retry_delay    [OPTIONAL] Integer Number of seconds to wait before retrying.
 *                                          Default: 60
 * @param timeout        [OPTIONAL] Integer How long is the timeout to be per attempt?
 *                                          The default here is 30 minutes -but- in practice the Jenkins Git plugin
 *                                          doesn't expose the timeout option in the pipeline call so Jenkins will end
 *                                          up using the default, which is 10 minutes. Fixing this is a high priority
 *                                          for SPiFI development.
 *                                          Default: 30
 * @param timeout_units  [OPTIONAL] String  Units to use for the timeout.
 *                                          Allowed values are: {HOURS|MINUTES|SECONDS}.
 *                                          Default: MINUTES
 * @param verbose        [OPTIONAL] Boolean Print out verbose information to the console.
 *                                          Default: false
 *
 * @return true if clone was successful, false if it failed.
 *
 * @TODO Find out if there is a way in a pipeline job to change Jenkins' default timeout for git requests.
 *       - Unfortunately, the Jenkins attitude is that no repository should take more than 10 minutes to clone... that's cute, but
 *         we see failures on Trilinos frequently because either Github or SNL Proxy throttles the download.
 */
static def clone(Map params)
{
    // Set up default values
    String  dir           = "."
    String  url           = ""
    String  branch        = "master"
    String  credentialsId = null
    Integer retries       = 0
    Integer retry_delay   = 60
    Integer timeout       = 30
    String  timeout_units = "MINUTES"

    // Process required parameters.
    if(!params.containsKey("env"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: env")
    }
    def env = params.env

    if(!params.containsKey("url"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: url")
    }
    assert params.url instanceof String
    url = params.url

    // Update optional parameters
    if(params.containsKey("branch"))         branch = params.branch
    if(params.containsKey("credentialsId"))  credentialsId = params.credentialsId
    if(params.containsKey("dir"))            dir = params.dir
    if(params.containsKey("retries"))        retries = params.retries
    if(params.containsKey("retry_delay"))    retry_delay = params.retry_delay
    if(params.containsKey("timeout"))        timeout = params.timeout
    if(params.containsKey("timeout_units"))  timeout_units = params.timeout_units

    // Validate parameters
    assert timeout_units=="SECONDS" || timeout_units=="MINUTES" || timeout_units=="HOURS"

    // Optionally print out some debugging output
    if(params.containsKey("verbose") && params.verbose == true)
    {
        env.println "[SPiFI]> Git.clone()\n" +
                    "[SPiFI]> -  url           : ${url}\n" +
                    "[SPiFI]> -  branch        : ${branch}\n" +
                    "[SPiFI]> -  dir           : ${dir}\n" +
                    "[SPiFI]> -  credentialsId : ${credentialsId}\n" +
                    "[SPiFI]> -  retries       : ${retries}\n" +
                    "[SPiFI]> -  retry_delay   : ${retry_delay}\n" +
                    "[SPiFI]> -  timeout       : ${timeout}\n" +
                    "[SPiFI]> -  timeout_units : ${timeout_units}"
        env.println "[SPiFI]> Environment:\n" +
                    "[SPiFI]> -  workspace: ${env.WORKSPACE}"
        env.println "[SPiFI]> Raw Params:\n${params}"
    }

    // Initialize output variables
    Boolean output = true
    Integer attempts = 1

    try
    {
        env.retry(retries)
        {
            try
            {
                env.timeout(time: timeout, unit: timeout_units)
                {
                    env.dir("${dir}")
                    {
                        env.git url: url, branch: branch, credentialsId: credentialsId
                        env.println "[SPiFI]> git successfully cloned: ${url}"
                    }
                }
            }
            catch(e)
            {
                env.println "[SPiFI]> git failed to clone: ${url}\n" +
                            "[SPiFI]> retrying in ${retry_delay} seconds."
                attempts++
                sleep retry_delay
                throw e
            }
        }
    }
    catch(e)
    {
        env.println "[SPiFI]> git failed to clone: ${url}\n" +
                    "[SPiFI]> -  Attempts made: ${attempts}"
        output = false
    }
    return output
}

return this
