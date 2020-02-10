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
 * Execute a command and capture output and exit status.
 *
 * @param env              [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
 * @param command          [REQUIRED] String  - The command + arguments to run as a single string (i.e., "ls -l -t")
 * @param path             [OPTIONAL] String  - Path to the working directory, assumes a path relative to the currnet directory
 *                                              (Probably env.WORKSPACE unless this is called inside a dir(){ } block.)
 *                                              Default: Jenkins Workspace Root - `env.WORKSPACE`
 * @param retries          [OPTIONAL] Integer - Number of retries.
 *                                              Default: 0
 * @param retry_delay      [OPTIONAL] Integer - Retry delay (seconds).
 *                                              Default: 60
 * @param timeout          [OPTIONAL] Integer - Timeout for the job to execute. The default is 90 minutes.
 *                                              Default 90
 * @param timeout_units    [OPTIONAL] String    Timeout units for the job. Valid options are {SECONDS, MINUTES, HOURS}
 *                                              Default: "MINUTES"
 * @param verbose          [OPTIONAL] Boolean - If present and set to true, some extra debugging information will be printed.
 *                                              Default: false
 * @param dry_run          [OPTIONAL] Boolean - If true, then execute a 'dry run' mode operation.
 *                                              Print out info with a delay but don't execute.
 *                                              Default: false
 * @param dry_run_delay    [OPTIONAL] Integer - If dry_run is true, this is the delay (seconds) to append when running.
 *                                              Default: 5
 * @param dry_run_status   [OPTIONAL] Integer - If dry_run is true, this is the exit status to be returned.
 *                                              Default: 0
 * @param dry_run_output   [OPTIONAL] String  - If dry_run is true, this is the output that will be returned.
 *                                              Default: ""
 * @param status_values_ok [OPTIONAL] List    - List of Integers. This is a list of valid status values that will be
 *                                              ok, along with the standard 0 return status.
 *                                              (i.e., if the command returns any of these OR 0 then the command will not
 *                                              trigger a retry.)
 *                                              Default: []
 * @param output_type      [OPTIONAL] String  - Output Type, stdout, stderr, etc. to capture.
 *                                              Options are: {stdout,stderr,stdout+stderr}
 *                                              Default: stdout+stderr
 *
 * @return Map containing the keys: {console, status, retries} which contain the console output and exit status of the command
 *             that was run.
 *             If there were multiple retries due to nonzero exit status, then the console output from the LAST run is returned.
 *             and retries is set to the number of additional attempts.  Note, if successful on the first attempt this
 *             will be 0.
 *             If the last attempt results in an exception thrown then status = -1, console = ""
 *             If the routine is run in dry-run mode, then status = dry_run_status, console = dry_run_output.
 *             DEPRECATION NOTICE: the 'stdout' is still provided but will go away in version 2.0, please use 'console' instead.
 */
def execute(Map params)
{
    def utility = new gov.sandia.sems.spifi.Utility()

    //
    // Begin parameter validation
    //

    // Check and set env first
    if(!params.containsKey("env"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: env")
    }
    def env = params.env

    env.println "[SPiFI]> Shell.execute()"
    env.println "[SPiFI]> Shell.execute(): parameter check begin"
    Map params_expected = [ "env":              [ option: "R" ],
                            "command":          [ option: "R" ],
                            "path":             [ option: "O" ],
                            "retries":          [ option: "O" ],
                            "retry_delay":      [ option: "O" ],
                            "timeout":          [ option: "O" ],
                            "timeout_units":    [ option: "O" ],
                            "verbose":          [ option: "O" ],
                            "dry_run":          [ option: "O" ],
                            "dry_run_delay":    [ option: "O" ],
                            "dry_run_status":   [ option: "O" ],
                            "dry_run_output":   [ option: "O" ],
                            "status_values_ok": [ option: "O" ],
                            "output_type":      [ option: "O" ]
                          ]
    Boolean params_ok = gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check(env: env,
                                                                               params_expected: params_expected,
                                                                               params_received: params,
                                                                               verbose: params.containsKey("verbose") && params.verbose
                                                                               )
    if( !params_ok )
    {
        throw new Exception("SPiFI ERROR: parameter check failed for Shell.execute()")
    }
    env.println "[SPiFI]> Shell.execute(): parameter check complete"

    //
    // Completed parameter validation
    //

    // Create a map to put output data into
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
    String  dry_run_output = ""

    // Valid Nonzero Status Values (i.e., these won't trigger a retry)
    List status_values_ok = [0]

    // What kind of output do we capture?
    String output_type = "stdout+stderr"

    // The command to execute
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
    if(params.containsKey("dry_run_output")) dry_run_output = params.dry_run_output

    // Process status value whitelist
    if(params.containsKey("status_values_ok"))
    {
        assert params.status_values_ok instanceof List

        // Union the lists... this should keep 0 in the list always
        def i = status_values_ok.intersect(params.status_values_ok)
        status_values_ok = ((status_values_ok + params.status_values_ok)-i)+i
    }
    // Set the output_type if a parameter is provided.
    if(params.containsKey("output_type"))
    {
        if(params.output_type in ["stdout+stderr","stdout","stderr"])
        {
            output_type = params.output_type
        }
        else
        {
            env.println "[SPiFI]> ERROR: Invalid value given for parameter 'output_type' in gov.sandia.sems.spifi.Shell.execute()\n" +
                        "[SPiFI]>        Allowed values: 'stdout+stderr', 'stdout', 'stderr'. \n" +
                        "[SPiFI]>        Value entered : '${params.output_type}''"
            throw new Exception("[SPiFI] ERROR: Invalid value given for parameter 'output_type' in gov.sandia.sems.spifi.Shell.execute()")
        }
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
                    "[SPiFI]> -  dry_run_output     : ${dry_run_output}\n" +
                    "[SPiFI]> -  valid status values: ${status_values_ok}\n" +
                    "[SPiFI]> -  output_type        : ${output_type}\n" +
                    "[SPiFI]> Environment:\n" +
                    "[SPiFI]> -  workspace: ${env.WORKSPACE}\n" +
                    "[SPiFI]> Raw Params:\n${params}"
    }

    if(dry_run)
    {
        env.println "[SPiFI]> DRY RUN!"
        env.sleep dry_run_delay

        output.status  = dry_run_status
        output.console = dry_run_output
        output.retries = 0
        // BEGIN DEPRECATION
        output.stdout  = "***SPiFI DEPRECATION WARNING***\n" +
                         "*** switch to output.console for console output before version 2.0 in Shell::execute() ***\n" +
                         dry_run_output +
                         "\n" +
                         "*** SPiFI DEPRECATION WARNING ***\n" +
                         "*** switch to output.console for console output before version 2.0 in Shell::execute() ***"
        // END DEPRECATION

        return output
    }

    // If not in dry-run mode, then we continue...

    // Initialize output variables
    String  console = ""
    Integer status  = -1
    Integer retries_performed = 0

    // # of attempts is one more than number of retries (i.e., 0 retries means there should be 1 attempt only)
    Integer attempts = retries + 1

    while(!(status in status_values_ok) && attempts > 0)
    {
        Boolean retry_exception = false

        String temp_filename = "__output_" + utility.randomString(30) + ".txt"
        if(params.containsKey("verbose") && params.verbose == true)
        {
            env.println "[SPiFI]> -  temp_file: ${temp_filename}"
        }

        env.timeout(time: timeout, unit: timeout_units)
        {
            // Error out if the path doesn't actually exist.
            if( !fileExists("${path}") )
            {
                // TODO: Check this println -- it seems to be failing in docker images. Maybe need to
                //       specifically check for directory over file?
                //env.println "[SPiFI]> WARNING in Shell::execute(): ${path} does not exist!"

                /*  This can break some things - revisit when going to version 1.2.0 and doing DEPRECATION work
                    Perhaps a warning is the right thing... or error out (but that will change how testing is done
                    Since the tests rely on doing things like "ls <directory that doesn't exist>" to get stderr output.
                output.status  = 1
                output.console = "An error occurred: ${path} provided to SPiFI Shell::execute() does not exist."
                output.retries = 0
                // BEGIN DEPRECATION
                output.stdout  = "***SPiFI DEPRECATION WARNING***\n" +
                                 "*** switch to output.console for console output before version 2.0 in Shell::execute() ***\n" +
                                output.console +
                                 "\n" +
                                 "*** SPiFI DEPRECATION WARNING ***\n" +
                                 "*** switch to output.console for console output before version 2.0 in Shell::execute() ***"
                // END DEPRECATION
                return output
                */
            }

            env.dir("${path}")
            {
                try
                {
                    // Set the redirect operator for output
                    String redirect1 = "1>"
                    String redirect2 = "2>&1"
                    if("stdout"==output_type)
                    {
                        redirect1 = "1>"
                        redirect2 = ""
                    }
                    else if("stderr"==output_type)
                    {
                        redirect1 = "2>"
                        redirect2 = ""
                    }

                    // Print / Echo the actual command
                    env.println "[SPiFI]>\n" +
                                "[SPiFI]> Execute: ${command} ${redirect1} ${temp_filename} ${redirect2}\n" +
                                "[SPiFI]>"

                    // Execute the command
                    status = env.sh(script: "${command} ${redirect1} ${temp_filename} ${redirect2}", returnStatus: true)

                    // Read / load the temp file and remove it.
                    console = env.readFile(temp_filename).trim()

                    // If in verbose mode, print out the console output we loaded in.
                    if(params.containsKey("verbose") && params.verbose == true)
                    {
                        env.println "[SPiFI]> temp_file: ${temp_filename}"
                        env.println "[SPiFI]> console output:\n${console}"
                    }

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
                                    "[SPiFI]> - output:\n${console}\n" +
                                    "[SPiFI]> - exception:\n${e}\n" +
                                    "[SPiFI]> Retrying in ${retry_delay} seconds."

                        // Reset values
                        console = ""
                        status  = -1
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
                        "[SPiFI]> - output:\n${console}\n" +
                        "[SPiFI]> Retrying in ${retry_delay} seconds."

            // Reset values
            console = ""
            status  = -1
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
                "[SPiFI]> - output :\n${console}\n"

    // Save the results
    output.status  = status
    output.console = console
    output.retries = retries_performed
    // BEGIN DEPRECATION
    output.stdout  = "*** SPiFI DEPRECATION WARNING ***\n" +
                     "*** switch to output.console for console output before version 2.0 in Shell::execute() ***\n" +
                     output.console +
                     "\n" +
                     "*** SPiFI DEPRECATION WARNING ***\n" +
                     "*** switch to output.console for console output before version 2.0 in Shell::execute() ***"

    // END DEPRECATION

    return output
}

return this
