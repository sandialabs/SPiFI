#!/usr/bin/env groovy
/**
 * Shell.groovy
 *
 * Shell script execution wrapper for SPiFI
 *
 * Jenkins Pipeline Usage:
 *
 *     def shell = new gov.sandia.sems.spifi.Shell()
 *     def output = shell.execute(env: this, command: 'ls -l -t -r [, <optional params>]')
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-04
 */
package gov.sandia.sems.spifi

import gov.sandia.sems.spifi.Utility



/**
 * Execute a command and capture stdout and exit status.
 *
 * @param env            [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
 * @param command        [REQUIRED] String  - The command + arguments to run as a single string (i.e., "ls -l -t")
 * @param path           [OPTIONAL] String  - Path to the working directory, assumes a path relative to the currnet directory
 *                                            (Probably env.WORKSPACE unless this is called inside a dir(){ } block.)
 *                                            Default: Jenkins Workspace Root - `env.WORKSPACE`
 * @param retries        [OPTIONAL] Integer - Number of retries.
 *                                            Default: 0
 * @param retry_delay    [OPTIONAL] Integer - Retry delay (seconds).
 *                                            Default: 60
 * @param timeout        [OPTIONAL] Integer - Timeout for the job to execute. The default is 90 minutes.
 *                                            Default 90
 * @param timeout_units  [OPTIONAL] String    Timeout units for the job. Valid options are {SECONDS, MINUTES, HOURS}
 *                                            Default: "MINUTES"
 * @param verbose        [OPTIONAL] Boolean - If present and set to true, some extra debugging information will be printed.
 *                                            Default: false
 * @param dry_run        [OPTIONAL] Boolean - If true, then execute a 'dry run' mode operation.
 *                                            Print out info with a delay but don't execute.
 *                                            Default: false
 * @param dry_run_delay  [OPTIONAL] Integer - If dry_run is true, this is the delay (seconds) to append when running.
 *                                            Default: 5
 * @param dry_run_status [OPTIONAL] Integer - If dry_run is true, this is the exit status to be returned.
 *                                            Default: 0
 * @param dry_run_stdout [OPTIONAL] String  - If dry_run is true, this is the stdout that will be returned.
 *                                            Default: ""
 * @param status_values_ok [OPTIONAL] List  - List of Integers. This is a list of valid status values that will be
 *                                            ok, along with the standard 0 return status.
 *                                            (i.e., if the command returns any of these OR 0 then the command will not
 *                                            trigger a retry.)
 *                                            Default: []
 *
 * @return Map containing two keys: {stdout, status, retries} which contain the stdout and exit status of the command
 *             that was run.
 *             If there were multiple retries due to nonzero exit status, then the output from the LAST run is returned.
 *             and retries is set to the number of additional attempts.  Note, if successful on the first attempt this
 *             will be 0.
 *             If the last attempt results in an exception thrown then status = -1, stdout = ""
 *             If the routine is run in dry-run mode, then status = dry_run_status, stdout = dry_run_stdout.
 */
def execute(Map params)
{
    def utility = new gov.sandia.sems.spifi.Utility()


    Map output = [:]

    // Set up default values
    String  path              = "${env.WORKSPACE}"

    // Retry Defaults
    Integer retries           = 0
    Integer retry_delay       = 60

    // Timeout Defaults
    Integer timeout           = 90
    String  timeout_units     = "MINUTES"

    // Dry run Defaults.
    Boolean dry_run        = false
    Integer dry_run_delay  = 5
    Integer dry_run_status = 0
    String  dry_run_stdout = ""

    // Valid Nonzero Status Values (i.e., these won't trigger a retry)
    List status_values_ok = [0]

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

    // Process status value whitelist
    if(params.containsKey("status_values_ok"))
    {
        assert params.status_values_ok instanceof List

        // Union the lists... this should keep 0 in the list always
        def i = status_values_ok.intersect(params.status_values_ok)
        status_values_ok = ((status_values_ok + params.status_values_ok)-i)+i
    }

    // Validate parameters
    assert timeout_units=="SECONDS" || timeout_units=="MINUTES" || timeout_units=="HOURS"

    // Optionally print out some debugging output
    if(params.containsKey("verbose") && params.verbose == true)
    {
        env.println "[SPiFI]> Shell.execute()\n" +
                    "[SPiFI]> -  path               : ${path}\n" +
                    "[SPiFI]> -  command            : ${command}\n" +
                    "[SPiFI]> -  retries            : ${retries}\n" +
                    "[SPiFI]> -  retry_delay        : ${retry_delay}\n" +
                    "[SPiFI]> -  timeout            : ${timeout}\n" +
                    "[SPiFI]> -  timeout_units      : ${timeout_units}\n" +
                    "[SPiFI]> -  dry_run            : ${dry_run}\n" +
                    "[SPiFI]> -  dry_run_delay      : ${dry_run_delay}\n" +
                    "[SPiFI]> -  dry_run_status     : ${dry_run_status}\n" +
                    "[SPiFI]> -  dry_run_stdout     : ${dry_run_stdout}\n" +
                    "[SPiFI]> -  Valid Status Values: ${status_values_ok}"
        env.println "[SPiFI]> Environment:\n" +
                    "[SPiFI]> -  workspace: ${env.WORKSPACE}"
        env.println "[SPiFI]> Raw Params:\n${params}"
    }

    if(dry_run)
    {
        env.println "[SPiFI]> DRY RUN!"
        env.sleep dry_run_delay

        output.status  = dry_run_status
        output.stdout  = dry_run_stdout
        output.retries = 0

        return output
    }

    // If not in dry-run mode, then we continue...

    // Initialize output variables
    String  stdout = ""
    Integer status = -1
    Integer retries_performed = 0

    // # of attempts is one more than number of retries (i.e., 0 retries means there should be 1 attempt only)
    Integer attempts = retries + 1

    while(!(status in status_values_ok) && attempts > 0)
    {
        Boolean retry_exception = false

        String temp_filename = "__output_" + utility.randomString(30) + ".txt"

        env.timeout(time: timeout, unit: timeout_units)
        {
            env.dir("${path}")
            {
                try
                {
                    // Print / Echo the actual command
                    env.println "[SPiFI]> Execute: ${command} &> ${temp_filename}"

                    // Execute the command
                    status = env.sh(script: "${command} &> ${temp_filename}", returnStatus: true)

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
                                    "[SPiFI]> - exception:\n${e}\n" +
                                    "[SPiFI]> Retrying in ${retry_delay} seconds."

                        // Reset values
                        status = -1
                        stdout = ""
                        retries_performed++
                        retry_exception = true

                        // Sleep for retry delay (seconds)
                        env.sleep retry_delay
                    }
                }
            }
        }

        // Reset for next attempt if not the final attempt and we're not retryign due to an exception.
        if(!(status in status_values_ok) && attempts > 1 && !retry_exception)
        {
            env.println "[SPiFI]> Command Failed due to exit status\n" +
                        "[SPiFI]> - valid status values = ${status_values_ok}\n" +
                        "[SPiFI]> - status = ${status}\n" +
                        "[SPiFI]> - output:\n${stdout}\n" +
                        "[SPiFI]> Retrying in ${retry_delay} seconds."

            // Reset values
            stdout = ""
            status = -1
            retries_performed++

            // Sleep for retry delay (seconds)
            env.sleep retry_delay
        }

        // Decrement attempts counter.
        attempts--
    }

    // Print out the results to console log
    env.println "[SPiFI]> Command returned:\n" +
                "[SPiFI]> - retries: ${retries_performed}\n" +
                "[SPiFI]> - status : ${status}\n" +
                "[SPiFI]> - output :\n${stdout}\n"

    // Save the results
    output["status"]  = status
    output["stdout"]  = stdout
    output["retries"] = retries_performed

    return output
}

return this
