#!/usr/bin/env groovy
/**
 *  Shell.groovy
 *
 *  Shell script execution wrapper for SPiFI
 *
 *  Jenkins Pipeline Usage:
 *
 *     def shell = new gov.sandia.sems.spifi.Shell()
 *     def output = shell.execute(env: this, command: 'ls -l -t -r [, <optional params>]')
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
 * execute
 *
 * Execute a command and capture stdout and exit status.
 *
 * Params:
 *   env            [REQUIRED] Jenkins environment (use 'this' in a jenkins pipeline)
 *   command        [REQUIRED] The command + arguments to run as a single string (i.e., "ls -l -t")
 *
 *   path           [OPTIONAL] Defaults to ${env.WORKSPACE}.  If provided, assumes a path relative to current directory.
 *                             (Probably env.WORKSPACE unless this is called inside a dir(){ } block.)
 *   retries        [OPTIONAL] Number of retries.  Default: 0
 *   retry_delay    [OPTIONAL] Retry delay (seconds).  Default: 10
 *   timeout        [OPTIONAL] Timeout for the job to execute.  Default 600
 *   timeout_units  [OPTIONAL] Timeout units for the job. Valid options are {SECONDS, MINUTES, HOURS} Default: SECONDS
 *   verbose        [OPTIONAL] If present and set to true, some extra debugging information will be printed.
 *
 *   dry_run        [OPTIONAL] If true, then execute a 'dry run' mode operation.  Print out info with a delay but don't execute.
 *                             Default: false
 *   dry_run_delay  [OPTIONAL] If dry_run is true, this is the delay (seconds) to append when running.  Default: 5
 *   dry_run_status [OPTIONAL] If dry_run is true, this is the exit status to be returned.  Default: 0
 *   dry_run_stdout [OPTIONAL] If dry_run is true, this is the stdout that will be returned. Default: ""
 */
def execute(Map params)
{
    Map output = [:]

    // Set up default values
    String  path              = "${env.WORKSPACE}"
    Integer retries           = 0
    Integer retry_delay       = 10
    Integer timeout           = 600
    String  timeout_units     = "SECONDS"
    // Dry run defaults.
    Boolean dry_run           = false
    Integer dry_run_delay     = 5
    Integer dry_run_status    = 0
    String  dry_run_stdout    = ""

    // Process required parameters.
    if(!params.containsKey("env"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: env")
    }
    def env = params.env

    if(!params.containsKey("command"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: command")
    }
    String command = params.command

    // Update optional parameters
    if(params.containsKey("path"))           path = params.path
    if(params.containsKey("retries"))        retries = params.retries
    if(params.containsKey("retry_delay"))    retry_delay = params.retry_delay
    if(params.containsKey("timeout"))        timeout = params.timeout
    if(params.containsKey("timeout_units"))  timeout_units = params.timeout_units
    // Set dry-run parameters
    if(params.containsKey("dry_run"))        dry_run        = params.dry_run
    if(params.containsKey("dry_run_delay"))  dry_run_delay  = params.dry_run_delay
    if(params.containsKey("dry_run_status")) dry_run_status = params.dry_run_status
    if(params.containsKey("dry_run_stdout")) dry_run_stdout = params.dry_run_stdout

    // Validate parameters
    assert timeout_units=="SECONDS" || timeout_units=="MINUTES" || timeout_units=="HOURS"

    // Optionally print out some debugging output
    if(params.containsKey("verbose") && params.verbose == true)
    {
        env.println "[SPiFI]> Shell.execute()\n" +
                    "[SPiFI]> -  path          : ${path}\n" +
                    "[SPiFI]> -  command       : ${command}\n" +
                    "[SPiFI]> -  retries       : ${retries}\n" +
                    "[SPiFI]> -  retry_delay   : ${retry_delay}\n" +
                    "[SPiFI]> -  timeout       : ${timeout}\n" +
                    "[SPiFI]> -  timeout_units : ${timeout_units}\n" +
                    "[SPiFI]> -  dry_run       : ${dry_run}\n" +
                    "[SPiFI]> -  dry_run_delay : ${dry_run_delay}\n" +
                    "[SPiFI]> -  dry_run_status: ${dry_run_status}\n" +
                    "[SPiFI]> -  dry_run_stdout: ${dry_run_stdout}"
        env.println "[SPiFI]> Environment:\n" +
                    "[SPiFI]> -  workspace: ${env.WORKSPACE}"
        env.println "[SPiFI]> Raw Params:\n${params}"
    }

    if(dry_run)
    {
        env.println "[SPiFI]> DRY RUN!"
        env.sleep dry_run_delay

        output.status = dry_run_status
        output.stdout = dry_run_stdout

        return output
    }

    // If not in dry-run mode, then we continue...

    // Initialize output variables
    String  stdout = ""
    Integer status = -1

    // # of attempts is one more than number of retries (i.e., 0 retries means there should be 1 attempt only)
    Integer attempts = retries + 1

    while(0 != status && attempts > 0)
    {
        Boolean retry_exception = false

        String temp_filename = "__output_" + randomString(30) + ".txt"

        env.timeout(time: timeout, unit: timeout_units)
        {
            env.dir("${path}")
            {
                try
                {
                    // Execute the command
                    status = env.sh(script: "${command} > ${temp_filename}", returnStatus: true)

                    // Read in the temp file and remove it.
                    stdout = env.readFile(temp_filename).trim()

                    // Delete the temp file
                    env.sh(script: "rm ${temp_filename}", returnStatus: true)
                }
                catch(e)
                {
                    env.sh(script: "rm ${temp_filename}", returnStatus: true)

                    // If something threw an error and not our final attempt, clean up for retry
                    if(attempts > 1)
                    {
                        env.println "[SPiFI]> Command Failed due to thrown exception\n" +
                                    "[SPiFI]> - status = ${status}\n" +
                                    "[SPiFI]> - stdout:\n${stdout}\n" +
                                    "[SPiFI]> - exception:\n${e}"
                        env.println "[SPiFI]> Retrying in ${retry_delay} seconds."

                        // Reset values
                        status = -1
                        stdout = ""
                        retry_exception = true

                        // Sleep for retry delay (seconds)
                        env.sleep retry_delay
                    }
                }
            }
        }

        // Reset for next attempt if not the final attempt and we're not retryign due to an exception.
        if(0 != status && attempts > 1 && !retry_exception)
        {
            env.println "[SPiFI]> Command Failed due to nonzero exit status\n" +
            env.println "[SPiFI]> - status = ${status}\n" +
                        "[SPiFI]> - stdout:\n${stdout}"
            env.println "[SPiFI]> Retrying in ${retry_delay} seconds."

            // Reset values
            stdout = ""
            status = -1

            // Sleep for retry delay (seconds)
            env.sleep retry_delay
        }

        // Decrement attempts counter.
        attempts--
    }

    // Print out the results to console log
    env.println "[SPiFI]> Command returned:\n" +
                "[SPiFI]> - status = ${status}\n" +
                "[SPiFI]> - stdout:\n${stdout}"

    // Save the results
    output["status"] = status
    output["stdout"] = stdout

    return output
}

return this
