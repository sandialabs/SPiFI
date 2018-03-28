#!/usr/bin/env groovy
/**
 *  Shell.groovy
 *
 *  Shell script execution wrapper for SPiFI
 *
 *  Jenkins Pipeline Usage:
 *
 *     shell = new gov.sandia.sems.spifi.Shell()
 *     def output = shell.execute(env: this, command: 'ls -l -t -r' [, <optional params>])
 * 
 */
package gov.sandia.sems.spifi;

import org.apache.commons.lang.RandomStringUtils



/**
 * Generate a random alphanumeric string
 * 
 * @param length Integer Length of string to generate.
 */
def randomString(Integer length)
{
  String charset = (('A'..'Z') + ('0'..'9')).join()
  String randomString = RandomStringUtils.random(length, charset.toCharArray())
  return randomString
}



/**
 * Execute a command and capture stdout and exit status.
 *
 * Params:
 *   env           [REQUIRED] Jenkins environment (use 'this' in a jenkins pipeline)
 *   command       [REQUIRED] The command + arguments to run as a single string (i.e., "ls -l -t")
 *   path          [OPTIONAL] Defaults to ${env.WORKSPACE}.  If provided, assumes a path relative to current directory.
 *                            (Probably env.WORKSPACE unless this is called inside a dir(){ } block.)
 *   retries       [OPTIONAL] Number of retries.  Default: 1
 *   retry_delay   [OPTIONAL] Retry delay (seconds).  Default: 10
 *   timeout       [OPTIONAL] Timeout for the job to execute.  Default 300
 *   timeout_units [OPTIONAL] Timeout units for the job. Valid options are {SECONDS, MINUTES, HOURS} Default: MINUTES
 */
def execute(Map params)
{
    Map output = [:]

    String  path              = "${env.WORKSPACE}"
    Integer retries           = 1
    Integer retry_delay       = 10
    Integer timeout           = 300
    String  timeout_units     = "SECONDS"

    // Process required parameters.
    if(!params.containsKey("env"))
    {
      error "Missing required param: env"
    }
    def env = params.env

    if(!params.containsKey("command"))
    {
      error "Missing required param: command"
    }
    String command = params.command

    // Update optional parameters
    if(params.containsKey("path"))           path = params.path
    if(params.containsKey("retries"))        retries = params.retries
    if(params.containsKey("retry_delay"))    retry_delay = params.retry_delay
    if(params.containsKey("timeout"))        timeout = params.timeout
    if(params.containsKey("timeout_units"))  timeout_units = params.timeout_units

    // Validate parameters
    assert timeout_units=="SECONDS" || timeout_units=="MINUTES" || timeout_units=="HOURS"

    /*
    // Some DEBUGGING output
    env.println "execute()\n${params}\n"
    env.println "State:\n" +
                "- path          : ${path}\n" +
                "- retries       : ${retries}\n" +
                "- retry_delay   : ${retry_delay}\n" +
                "- timeout       : ${timeout}\n" +
                "- timeout_units : ${timeout_units}"
    env.println "ENV:\n" +
                "- workspace: ${env.WORKSPACE}"
    */

    // output variables for result capture
    String stdout = ""
    Integer status = -1

    try
    {
        retry(retries)
        {
            try
            {
                env.timeout(time: timeout, unit: timeout_units)
                {
                    String temp_filename = "__output_" + randomString(30) + ".txt"

                    // Execute the command
                    status = env.sh(script: "${command} > ${temp_filename}", returnStatus: true)

                    // Read in the temp file and remove it.
                    stdout = env.readFile(temp_filename).trim()

                    // Delete the temp file
                    env.sh(script: "rm ${temp_filename}", returnStatus: true)
                }
            }
            catch(e)
            {
                env.sleep retry_delay
                throw e
            }
        }
    }
    catch(e)
    {
        // If the job failed, this should catch it... 
        throw e
    }

    // Save the results
    output["status"] = status
    output["stdout"] = stdout

    return output
}

return this
