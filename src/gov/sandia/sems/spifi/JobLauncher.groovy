#!/usr/bin/env groovy
/**
 * JobLauncher.groovy
 *
 * Class to handle organizing and launching Jenkins jobs in parallel with convenience
 * features to specify a 'dry-run' mode which prints out the job it would have launched
 * with a parameterized delay without actually launching the job in Jenkins.  A useful
 * feature for debugging pipelines.
 *
 * Other parameterizable features are timeout settings and whether or not to launch each job
 * within its own node{} block (Jenkins pipelines recommend using one of these
 * 'monitor nodes' as a 'best practice' but in our experience the monitor node consumes
 * an executor from a build slave which can become problematic when launching many jobs.)
 *
 * Roadmap
 * -------
 * - [ ] : Add multiple run modes for JobLauncher: [Sequential, Parallel]
 * - [ ] : Add a Dependency order run mode (essentially a DAG)
 * - [ ] : Enhance DAG mode to allow conditions on dependency edges to dictate execution.
 *
 * @author  William McLendon
 * @version 1.2
 * @since   2018-04-04
 *
 */
package gov.sandia.sems.spifi

import gov.sandia.sems.spifi.DelayedRetry
import gov.sandia.sems.spifi.DelayedRetryOnRegex
import gov.sandia.sems.spifi.JenkinsTools
import gov.sandia.sems.spifi.Utility



/**
 * Job launcher class to simplify the options in a Jenkins pipeline for launching
 * multiple Jenkins jobs in parallel.  This class provides the ability to set
 * timeouts, dry-run operations for debugging, use of a monitor node, etc. on a
 * per-job basis as well as aggregation of results with summarization.  The goal
 * is to allow a simplified interface to a Jenkins pipeline and reduce repetition
 * in pipelines with many stages that launch numerous jobs so the pipeline developer
 * can focus more on the "what I want to do" and less on the "how do I do it".
 *
 * Usage Pattern from a Jenkins Pipeline:
 *
 *     def launcher = gov.sandia.sems.spifi.JobLauncher(this)
 *     launcher.appendJob("Job 1", "JENKINS_JOB_ALPHA")
 *     launcher.appendJob("Job 2", "JENKINS_JOB_BRAVO")
 *     launcher.appendJob("Job 2", "JENKINS_JOB_CHARLIE")
 *     def results = launcher.launchInParallel()
 *     def summary = launcher.getLastResultSummary()
 *     launcher.clearJobs()
 *
 */
class JobLauncher
{
    // helper subclass for dynamic parameters on appendJob
    class _parameters
    {
        String  label           = "__REQUIRED__"
        String  job_name        = "__REQUIRED__"
        List    parameters      = null
        Integer quiet_period    = 0
        Integer timeout         = 23
        String  timeout_unit    = "HOURS"
        Boolean propagate_error = false
        Boolean dry_run         = false
        String  dry_run_status  = "SUCCESS"
        Integer dry_run_delay   = 30
        String  monitor_node    = ""

        // expected_duration_max <= 0 implies infinity.
        Integer expected_duration_min   = 0
        Integer expected_duration_max   = 0
        String  expected_duration_units = "SECONDS"

        // Job Retry Parameters.
        Integer retry_lines_to_check = 200            // # of lines to pull from console log to check.
        Integer retry_max_limit      = 99             // Set a maximum # of allowable retries so things don't get too crazy.
        Integer retry_max_count      = 0              // Number of retries is for the job, regardless of the mix of 'matches'
                                                      //  on retry-criteria.  Note: num_attempts == num_retries+1
        List    retry_conditions     = null           // Retry conditions is a list of gov.sandia.sems.spifi.DelayedRetryOnRegex
                                                      // - data members are:  retry_delay, retry_delay_units, retry_regex
    }


    // Member Variables
    private static _env
    private Map<String,String> _jobList                   // The jobs + their parameters
    private Map<String,String> _lastResultSummary         // Status on the latest results

    private List _allowable_job_status_core               // Core exit status values from Jenkins (SUCCESS, FAILURE, UNSTABLE, ABORTED, NOT_BUILT)
    private List _allowable_job_status_spifi              // Extra exit status values from SPiFI (TIMEOUT)


    /**
     * Constructor for the job launcher.
     *
     * @param env [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
     *
     */
    JobLauncher(Map params)
    {
        // Validate parameters
        if(!params.containsKey("env"))
        {
            throw new Exception("[SPiFI]> Missing required parameter: 'env'")
        }

        // Set Parameter Default(s)
        this._env = params.env
        this._jobList = [:]
        this._lastResultSummary = [:]

        this._allowable_job_status_core  = ["SUCCESS","FAILURE","UNSTABLE","ABORTED","NOT_BUILT"]
        this._allowable_job_status_spifi = ["TIMEOUT"]
    }

    @Deprecated
    JobLauncher(env)
    {
        env.println "[SPiFI DEPRECATION NOTICE]>\n" +
                    "[SPiFI DEPRECATION NOTICE]> JobLauncher(this) will be deprecated in 2.0.0\n" +
                    "[SPiFI DEPRECATION NOTICE]> -  Please use JobLauncher(\"env\": this)\n" +
                    "[SPiFI DEPRECATION NOTICE]>"

        // Set Parameter Default(s)
        this._env = env
        this._jobList = [:]
        this._lastResultSummary = [:]
    }


    /**
     * Add a new job into the job list.
     *
     * Example:
     *    appendJob(label: "T1", job_name: "Jenkins-Job-To-Launch")
     *
     * @param label           [REQUIRED] String  - Job label name.  Must be unique.
     * @param job_name        [REQUIRED] String  - Name of Jenkins job to launch.
     * @param parameters      [OPTIONAL] List    - List of jenkins parameters to the job, example:
     *                                             [
     *                                                 (string(name:"PARAM_NAME_1", value:"PARAM_VALUE_1")),
     *                                                 (string(name:"PARAM_NAME_2", value:"PARAM_VALUE_2"))
     *                                             ]
     *                                             If there are no parameters, an empty list [] or null can be used.
     *                                             Default: null
     * @param quiet_period    [OPTIONAL] Integer - Quiet period (seconds).
     *                                             Default: 0
     * @param timeout         [OPTIONAL] Integer - Timeout duration.  If not provided, the default is 23 hours
     *                                             i.e., 1 less than a day as many jobs would be run as a nightly run
     *                                             and the job would be aborted before the next nightly set.  Most
     *                                             job timeouts are less than a day.
     *                                             Default: 23
     * @param timeout_unit    [OPTIONAL] String  - Timeout Unit {HOURS, MINUTES, SECONDS}.
     *                                             Default="HOURS"
     * @param propagate_error [OPTIONAL] Boolean - Propagate error to overall pipeline?  { true, false }
     *                                             Deault: false
     * @param dry_run         [OPTIONAL] Boolean - If true, then use dry-run mode (job will not be launched).
     *                                             Default: false
     * @param dry_run_status  [OPTIONAL] String  - If dry_run is true, set status to this value.
     *                                             Must be one of {SUCCESS, FAILURE, UNSTABLE, ABORTED, NOT_BUILT}
     *                                             Default: "SUCCESS"
     * @param dry_run_delay   [OPTIONAL] Integer - If dry_run is true, this introduces a delay (in seconds)
     *                                             for the 'simulated' job.
     *                                             Default: 30
     * @param monitor_node    [OPTIONAL] String  - Node expression for the Jenkins "node" where the job
     *                                             monitor will run.  Note: This does not affect where the
     *                                             job itself will run since that's controlled by the job
     *                                             itself (or should be).  If omitted then no 'monitor' job
     *                                             will be created.
     *                                             Default: ""
     *
     * @param expected_duration_min   [OPTIONAL] Integer - Add a minimum expected time for the job to run.
     *                                                     Only checked if job status == SUCCESS.
     *                                                     Set to <= 0 to ignore minimum expected time bound.
     *                                                     If > 0 and execution time is less, set job status to UNSTABLE.
     *                                                     Default: 0
     * @param expected_duration_max   [OPTIONAL] Integer - Add a maximum expected time for the job to run.
     *                                                     Only checked if job status == SUCCESS.
     *                                                     Set to <= 0 to ignore maximum expected time bound.
     *                                                     If > 0 and execution time is greater, set job status to UNSTABLE.
     *                                                     Default: 0
     * @param expected_duration_units [OPTIONAL] String  - Units of the expected time bounds.  Can be HOURS, MINUTES, or SECONDS.
     *                                                     Default: "SECONDS"
     *
     * @param retry_max_count         [OPTIONAL] Integer - Maximum number of retries to attempt. Default: 0
     *
     * @param retry_lines_to_check    [OPTIONAL] Integer - If retry_conditions are provided, this is the # of lines from the end of
     *                                                     the console used to scan for the presence of the retry condition.
     *                                                     If this is 0 then it overrides retry checking.
     *                                                     Default: 200
     *
     * @param retry_conditions        [OPTIONAL] List    - If provided, this is a List of retry conditions that if met, will trigger
     *                                                     a re-run of the job. Currently this must be a list of DelayedRetryOnRegex
     *                                                     objects.
     *                                                     Only the FINAL attempt's results will be reported.
     *
     * @return nothing
     */
    def appendJob(Map params)
    {
        def utility = new gov.sandia.sems.spifi.Utility()

        // Optional Debugging messages
        //this._env.println "[SPiFI] DEBUGGING> params:\n" + params
        //params.retry_conditions.each
        //{ rci->
        //    this._env.println "[SPiFI] DEBUGGING> params.retry_condition: ${rci.asString()}"
        //    this._env.println "[SPiFI] DEBUGGING> params.retry_condition:\n" +
        //                      "[SPiFI] DEBUGGING>    retry_delay       = ${rci.retry_delay}\n" +
        //                      "[SPiFI] DEBUGGING>    retry_delay_units = \"${rci.retry_delay_units}\"\n" +
        //                      "[SPiFI] DEBUGGING>    retry_regex       = \"${rci.retry_regex}\"\n"
        //}

        // Check for required parameters
        if(!params.containsKey("label"))
        {
            throw new Exception("gov.sandia.sems.spifi.JobLauncher::appendJob missing required parameter: label")
        }
        if(!params.containsKey("job_name"))
        {
            throw new Exception("gov.sandia.sems.spifi.JobLauncher::appendJob missing required parameter: job_name")
        }

        def job = new _parameters()

        job.label    = params.label
        job.job_name = params.job_name

        if(params.containsKey("parameters"))      { job.parameters      = params.parameters      }
        if(params.containsKey("quiet_period"))    { job.quiet_period    = params.quiet_period    }
        if(params.containsKey("timeout"))         { job.timeout         = params.timeout         }
        if(params.containsKey("timeout_unit"))    { job.timeout_unit    = params.timeout_unit    }
        if(params.containsKey("propagate_error")) { job.propagate_error = params.propagate_error }
        if(params.containsKey("dry_run"))         { job.dry_run         = params.dry_run         }
        if(params.containsKey("dry_run_status"))  { job.dry_run_status  = params.dry_run_status  }
        if(params.containsKey("dry_run_delay"))   { job.dry_run_delay   = params.dry_run_delay   }
        if(params.containsKey("monitor_node"))    { job.monitor_node    = params.monitor_node    }

        // Process expected duration parameters
        if(params.containsKey("expected_duration_min"))   { job.expected_duration_min   = params.expected_duration_min   }
        if(params.containsKey("expected_duration_max"))   { job.expected_duration_max   = params.expected_duration_max   }
        if(params.containsKey("expected_duration_units")) { job.expected_duration_units = params.expected_duration_units }

        // Process optional retry-conditions parameter(s)
        if(params.containsKey("retry_lines_to_check"))
        {
            if(params.retry_lines_to_check < 0) { job.retry_lines_to_check = 0 }
            else { job.retry_lines_to_check = params.retry_lines_to_check }
        }
        if(params.containsKey("retry_max_count"))
        {
            if(params.retry_max_count < 0)                        { job.retry_max_count = 0 }
            else if(params.retry_max_count > job.retry_max_limit) { job.retry_max_count = job.retry_max_limit }
            else                                                  { job.retry_max_count = params.retry_max_count }
        }
        if(params.containsKey("retry_conditions"))
        {
            // Validate the parameters
            params.retry_conditions.each
            { rci ->
                assert rci instanceof gov.sandia.sems.spifi.DelayedRetryOnRegex
            }
            // Got here, so the retry_conditions parameter list is ok.
            job.retry_conditions = params.retry_conditions
        }


        // Validate parameter value(s)
        if( !("SECONDS"==job.timeout_unit || "MINUTES"==job.timeout_unit || "HOURS"==job.timeout_unit) )
        {
            throw new Exception("gov.sandia.sems.spifi.JobLauncher::appendJob invalid parameter timeout_unit provided: ${job.timeout_unit}")
        }
        if( !("SECONDS"==job.expected_duration_units || "MINUTES"==job.expected_duration_units || "HOURS"==job.expected_duration_units) )
        {
            throw new Exception("gov.sandia.sems.spifi.JobLauncher::appendJob invalid parameter expected_duration_units provided: ${job.timeout_unit}")
        }
        if(job.expected_duration_max > 0 && job.expected_duration_min >= job.expected_duration_max)
        {
            throw new Exception("gov.sandia.sems.spifi.JobLauncher::appendJob expected_duration_min >= expected_duration_max")
        }
        // Note: don't need to check job.retry_conditions[i].retry_delay_units because this is checked by the DelayedRetry c'tor


        // normalize expected duration bounds to seconds.
        job.expected_duration_min   = utility.convertDurationToSeconds(job.expected_duration_min, job.expected_duration_units)
        job.expected_duration_max   = utility.convertDurationToSeconds(job.expected_duration_max, job.expected_duration_units)
        job.expected_duration_units = "SECONDS"  // set units AFTER they are normalized to seconds.

        this._env.println "[SPiFI]> Append job ${job.label}"

        this._jobList[job.label] = [ jenkins_job_name:        job.job_name,
                                     parameters:              job.parameters,
                                     quiet_period:            job.quiet_period,
                                     timeout:                 job.timeout,
                                     timeout_unit:            job.timeout_unit,
                                     propagate_error:         job.propagate_error,
                                     dry_run:                 job.dry_run,
                                     dry_run_status:          job.dry_run_status,
                                     dry_run_delay:           job.dry_run_delay,
                                     monitor_node:            job.monitor_node,
                                     expected_duration_min:   job.expected_duration_min,
                                     expected_duration_max:   job.expected_duration_max,
                                     expected_duration_units: job.expected_duration_units,
                                     retry_lines_to_check:    job.retry_lines_to_check,
                                     retry_max_limit:         job.retry_max_limit,
                                     retry_max_count:         job.retry_max_count,
                                     retry_conditions:        job.retry_conditions
                                   ]
    }



    /**
     * Clear / reset the job list and result summary.  This is an optional step and is only really
     * necessary if you're going to launch additional sets of jobs using the same launcher object.
     * If only one batch of jobs exists within the same scope that the launcher is instantiated then
     * this cleanup step isn't necessary.
     *
     * @return nothing
     */
    def clearJobs()
    {
        this._jobList.clear()
        this._resetLastResultSummary()
    }



    /**
     * Schedule and launch the jobs in the joblist in parallel.
     *
     * @return Map Results of the run containing key/value pairs:
     *             label: [
     *                        job:      <jenkins job name (String)>,
     *                        status:   <jenkins job status (String)>,
     *                        id:       <jenkins job build id (String)>,
     *                        url:      <absolute URL to jenkins job (String)>,
     *                        duration: <duration of job in seconds (Float)>,
     *                        dry_run:  <true IF job was launched as a dry-run (Boolean)>
     *                    ]
     */
    def launchInParallel()
    {
        this._env.println "[SPiFI]> JobLauncher.launchInParallel()"

        def builders = [:]
        def results  = [:]

        // Construct the execution 'block' that will be executed concurrently for each of the jobs.
        this._jobList.each
        {   _job ->
            def job = _job
            results[job.key] = "UNKNOWN"
            builders[job.key] =
            {
                if(job.value.monitor_node == "")
                {
                    results << this._jobBodyWithoutMonitorNode(job)
                }
                else
                {
                    results << this._jobBodyWithMonitorNode(job)
                }
            }
        }

        // Launch the jobs in parallel
        this._env.parallel builders

        // Update the result summary
        this._resetLastResultSummary()
        results.each
        {   _r ->
            def r = _r
            this._updateLastResultSummary(r.value["status"])

            this._env.println "[SPiFI]> Job ${r.key} (${r.value.job}) completed [${r.value.status}]."
            //this._env.println "[SPiFI]> - UpdateLastResultSummary:\n${r}"
        }

        return results
    }



    /**
     * Return a summary of the most recent set of jobs run by LaunchInParallel.
     *
     * @return Map with a summary of the most recent set of jobs. The following
     *             key value pairs are included in the output:
     *             NUMJOBS      - The # of jobs that were run.
     *             NUMSUCCESS   - The # of jobs that completed successfully.
     *             NUMFAILURE   - The # of jobs that completed with a failure.
     *             NUMUNSTABLE  - The # of jobs that completed with status UNSTABLE.
     *             NUMABORTED   - The # of jobs that completed with status ABORTED.
     *             NUMNOT_BUILT - The # of jobs that completed with status NOT_BUILT.
     */
    def getLastResultSummary()
    {
        return this._lastResultSummary
    }



    /**
     * Pretty Print the job list into the Jenkinns console output.
     *
     * @return nothing
     */
    def printJobList()
    {
        String strJobs = "[SPiFI]> ----[ JobList ]----------\n"
        for(job in this._jobList)
        {
            strJobs += "[SPiFI]> Job: ${job.key}:\n"
            strJobs += "[SPiFI]>   - jenkins_job_name        : " + job.value["jenkins_job_name"] + "\n"
            strJobs += "[SPiFI]>   - monitor_node            : " + job.value["monitor_node"] + "\n"
            strJobs += "[SPiFI]>   - quiet_period            : " + job.value["quiet_period"] + "\n"
            strJobs += "[SPiFI]>   - timeout                 : " + job.value["timeout"] + "\n"
            strJobs += "[SPiFI]>   - timeout_unit            : ${job.value.timeout_unit}\n"
            strJobs += "[SPiFI]>   - propagate_error         : ${job.value.propagate_error}\n"
            strJobs += "[SPiFI]>   - expected_duration_min   : ${job.value.expected_duration_min}\n"
            strJobs += "[SPiFI]>   - expected_duration_max   : ${job.value.expected_duration_max}\n"
            strJobs += "[SPiFI]>   - expected_duration_units : ${job.value.expected_duration_units}\n"
            if(job.value.dry_run == true)
            {
                strJobs += "[SPiFI]>   - dry_run                 : " + job.value.dry_run + "\n"
                strJobs += "[SPiFI]>   - dry_run_status          : " + job.value.dry_run_status + "\n"
                strJobs += "[SPiFI]>   - dry_run_delay           : " + job.value.dry_run_delay + "\n"
            }
            strJobs += "[SPiFI]>   - parameters              : \n"
            for(param in job.value["parameters"])
            {
                strJobs += "[SPiFI]>               " + param + "\n"
            }
            if(job.value.retry_conditions != null)                                                                                  // SCAFFOLDING
            {
                strJobs += "[SPiFI]>   - retry_lines_to_check    : ${job.value.retry_lines_to_check}\n"
                strJobs += "[SPiFI]>   - retry_max_limit         : ${job.value.retry_max_limit}\n"
                strJobs += "[SPiFI]>   - retry_max_count         : ${job.value.retry_max_count}\n"
                strJobs += "[SPiFI]>   - retry_conditions        : \n"
                for(cond in job.value.retry_conditions)
                {
                    strJobs += "[SPiFI]>               ${cond.asString()}\n"                                                        // SCAFFOLDING
                }
            }
        }
        // Strip off trailing newline...
        strJobs = strJobs.replaceAll("\\s\$","")

        this._env.println "${strJobs}"
    }


    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------



    /**
     * Body of the job launcher.  This method handles the actual launching and
     * result capture of a single job that is to be launched in Jenkins.
     *
     * @return Map containing job results.
     */
    def _jobBody(job)
    {
        def utility = new gov.sandia.sems.spifi.Utility()

        // Set up results data structure and set default properties
        def results = [:]
        results[job.key] = [:]
        results[job.key]["job"]      = job.value.jenkins_job_name
        results[job.key]["status"]   = ""
        results[job.key]["id"]       = 0
        results[job.key]["url"]      = ""
        results[job.key]["duration"] = 0
        results[job.key]["dry_run"]  = job.value.dry_run

        // Everything after this level is executed...
        try
        {
            this._env.timeout(time: job.value.timeout, unit: job.value.timeout_unit)
            {
                if(job.value.dry_run)
                {
                    this._env.println "[SPiFI]> ${job.value.jenkins_job_name} Execute in dry-run mode\n" +
                                      "[SPiFI]> - Delay : ${job.value.dry_run_delay} seconds\n" +
                                      "[SPiFI]> - Status: ${job.value.dry_run_status}"

                    results[job.key]["status"]   = job.value.dry_run_status
                    results[job.key]["duration"] = job.value.dry_run_delay
                    this._env.sleep job.value.dry_run_delay
                }
                else
                {
                    this._env.println "[SPiFI]> Number of attempts allowed = ${job.value.retry_max_count+1}\n" +
                                      "[SPiFI]> Lines of output to check   = ${job.value.retry_lines_to_check}"

                    if(job.value.retry_conditions != null)
                    {
                        this._env.println "[SPiFI]> Number of retry conditions to check: ${job.value.retry_conditions.size()}"
                    }
                    else
                    {
                        this._env.println "[SPiFI]> Number of retry conditions to check: 0"
                    }

                    def       status = null
                    String    jobStatus = ""
                    Float     duration_seconds = 0.0

                    Integer   attempt_number = 0
                    Integer   attempt_limit  = job.value.retry_max_count + 1        // +1 because the *first* attempt isn't a retry
                    Boolean   attempt_failed = true
                    Exception attempt_exception = null

                    while( attempt_failed && attempt_number < attempt_limit )
                    {

                        attempt_number++
                        attempt_failed = false

                        this._env.println "[SPiFI]> -------------------\n" +
                                          "[SPiFI]> Attempt ${attempt_number} of ${attempt_limit}\n" +
                                          "[SPiFI]> -------------------"

                        //
                        // Launch the job here
                        //
                        // Note: status is a org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper object
                        //       http://javadoc.jenkins.io/plugin/workflow-support/org/jenkinsci/plugins/workflow/support/steps/build/RunWrapper.html
                        //
                        status = this._env.build job        : job.value.jenkins_job_name,
                                                 parameters : job.value.parameters,
                                                 quietPeriod: job.value.quiet_period,
                                                 propagate  : job.value.propagate_error,
                                                 wait       : true

                        //this._env.println "[SPiFI DEBUGGING]> -------------------\n" +
                        //                  "[SPiFI DEBUGGING]>\n" +
                        //                  "[SPiFI DEBUGGING]> status = ${status}\n" +
                        //                  "[SPiFI DEBUGGING]>\n" +
                        //                  "[SPiFI DEBUGGING]> -------------------\n" 

                        duration_seconds = utility.convertDurationToSeconds(status.getDuration(), "MILLISECONDS")
                        jobStatus        = status.getResult()

                        // If job was successful, check the execution time bounds.
                        if(jobStatus=="SUCCESS")
                        {
                            // Set status to UNSTABLE if we care about min duration and duration < mn expected OR
                            //                        if we care about max duration and duration > max expected
                            if( (job.value.expected_duration_min > 0 && duration_seconds < job.value.expected_duration_min) ||
                                (job.value.expected_duration_max > 0 && duration_seconds > job.value.expected_duration_max) )
                            {
                                this._env.println "SPiFI> WARNING: Job returned SUCCESS but execution time is outside the expected time bounds.\n" +
                                                  "SPiFI>          Setting status to UNSTABLE"

                                jobStatus = "UNSTABLE"
                            }
                        }

                        // Check job status for a retry IF the job returned FAILURE or UNSTABLE
                        else if(jobStatus == "FAILURE" || jobStatus == "UNSTABLE")
                        {
                            // Only scan the job results if we have retry-conditions
                            if(job.value.retry_conditions != null)
                            {
                                this._env.println "[SPiFI]> Job status is not SUCCESS\n"

                                // Get the console log from the sub-job as a List of Strings.
                                List<String> build_log = status.getRawBuild().getLog( job.value.retry_lines_to_check )

                                // Scan each 'condition' for a match
                                def     retry_condition_matched = null
                                Boolean retry_condition_found   = job.value.retry_conditions.find
                                { rci ->
                                    if( rci.scanBuildLog( build_log: build_log) )
                                    {
                                        retry_condition_matched = rci
                                        return true
                                    }
                                    return false
                                }
                                retry_condition_found = retry_condition_found == true   // force value to be true || false

                                String msg = "[SPiFI]> retry_condition matched = ${retry_condition_found}\n"
                                if(retry_condition_found)
                                {
                                    msg += "[SPiFI]> retry_condition_matched = ${retry_condition_matched.asString()}\n" 
                                    msg += "[SPiFI]> retry delay             = ${retry_condition_matched.retry_delay}\n" 
                                    msg += "[SPiFI]> retry delay units       = ${retry_condition_matched.retry_delay_units}\n"
                                }
                                this._env.println "${msg}"

                                if(retry_condition_found && attempt_number < attempt_limit)
                                {
                                    attempt_failed = true

                                    // Set the delay on a failure
                                    this._env.sleep(time: retry_condition_matched.retry_delay,
                                                    unit: retry_condition_matched.retry_delay_units)
                                }

                            }   // if retry_conditions != null
                        }   // else status != SUCCESS
                    }   // While retries on regex

                    this._env.println "[SPiFI]> Final job status is ${jobStatus} after ${attempt_number} attempts."

                    // Save selected parts of the result to the results
                    results[job.key]["status"]   = jobStatus
                    results[job.key]["id"]       = status.getId()
                    results[job.key]["url"]      = status.getAbsoluteUrl()
                    results[job.key]["duration"] = duration_seconds

                    // To get the console log from the status object you need to call status.getRawBuild().getLog()
                    //  status.getRawBuild() returns a org.jenkinsci.plugins.workflow.job.WorkflowRun object
                    // getLog() returns a string containing the log.
                    // getLog( int maxLines ) returns a List<String> object.
                    // this._env.println "SPiFI> status isa ${status.getClass().getName()}"

                }   // else not a dry run...

                this._env.println "[SPiFI]> ${job.value.jenkins_job_name} = ${results[job.key]}"
            }  // End Timeout

            // TODO: Detect that the timeout was hit???

            // TODO: See if we can identify the job build id and url, etc. even when jobs timeout...
            //       maybe launch in non-blocking mode, get the info, and then block on results?
            //       (if this is possible in Jenkins' groovy).

        }      // End Try
        catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e)
        {
            // Note: FlowInterruptedException is triggered in both cases if the job was killed by 
            //       the user or if it was killed due to a test timeout.
            this._env.println "SPiFI> --------------------------------------------------------\n" +
                              "SPiFI> ERROR: ${job.key} (${job.value.jenkins_job_name}) was aborted.\n" +
                              "SPiFI> This can happen due to timeout or if the pipeline is aborted.\n" +
                              "SPiFI> Job timeout ${job.value.timeout} ${job.value.timeout_unit}.\n" +
                              "SPiFI> Exception:\n${e}\n" +
                              "SPiFI> Exception Cause:\n${e.getCause()}\n" +
                              "SPiFI> Exception Message:\n${e.getMessage()}\n" +
                              "SPiFI> Exception StackTrace:\n${e.getStackTrace()}\n" +
                              "SPiFI> --------------------------------------------------------"
            def strace = e.getStackTrace()

            // Many things can cause a FlowInterruptedException so let's check the stack trace 
            // to try and identify what caused it.
            // WARNING: This might be brittle if Jenkins changes things up, but for now there's 
            //          no way I know of to differentiate what interrupted the sub-job.
            /* DEBUG */ def tools = new gov.sandia.sems.spifi.JenkinsTools()
            /* DEBUG */ this._env.println tools.spifi_get_exception_stacktrace_pretty(env: this._env, exception: e)

            String interrupt_reason="UNKNOWN"
            strace.find 
            {
                // Sub-job killed due to some kind of timeout 
                if( it.getClassName().toString().contains("workflow.steps.TimeoutStepExecution") && it.getMethodName().toString() == "cancel")
                {
                    interrupt_reason = "TIMEOUT"
                    results[job.key]["duration"] = utility.convertDurationToSeconds(job.value.timeout, job.value.timeout_unit)
                    return true
                }
                // Sub-job killed due to an abort
                // - Note: I don't think we can distinguish if this was due to a pipeline abort or if it's a sub-job abort
                else if( it.getClassName().toString().contains("workflow.steps.BodyExecution") && it.getMethodName().toString() == "cancel" )
                {
                    interrupt_reason = "ABORTED"
                    throw e
                    return true
                }
                // Sometimes we get this in the stacktrace... I don't fully understand when this is triggered vs. the above one.
                else if( it.getClassName().toString().contains("workflow.cps.CpsFlowExecution") && it.getMethodName().toString() == "interrupt" )
                {
                    interrupt_reason = "ABORTED"
                    throw e
                    return true
                }
                return false
            }

            // TODO: In this case, when we can tell that the pipeline itself was aborted, we do what Jenkins will do and 
            //       we pass the exception up and just kill the whole pipeline at this point.  It will abort the sub-jobs
            //       and exit.  There won't be any emails or anything else sent because things die.  
            //       We could investigate some optional settings to JobLauncher to prevent killing the pipeline from immediately
            //       aborting the whole pipeline but rather have it pass up some flag or status to note that the pipeline was 
            //       killed here (though, a check for that should probably be put somewhere higher than in _jobBody).

            results[job.key]["status"] = interrupt_reason
        }
        catch(e)
        {
            this._env.println "SPiFI> --------------------------------------------------------\n" +
                              "SPiFI> ERROR: Unknown error occurred:\n${e}\n" +
                              "SPiFI> --------------------------------------------------------"
            results[job.key]["status"] = "FAILURE"
        }

        return results
    }   // _jobBody



    /**
     * Drives the launching of a job if a monitor-node is requested.
     * - This is not the default mode of operation since monitor nodes occupy an
     *   executor node.
     *
     * @return Map containing job results.
     */
    def _jobBodyWithMonitorNode(job)
    {
        def results = [:]
        this._env.node(job.value.monitor_node)
        {
            results << this._jobBody(job)
        }
        return results
    }



    /**
     * Drives the launching of a job if a monitor-node is not requested.
     * - This is the default mode of operation.
     *
     * @return Map containing job results.
     */
    def _jobBodyWithoutMonitorNode(job)
    {
        return this._jobBody(job)
    }



    /**
     * Reset / Clear the summary from the last result.
     * This will be called every time we invoke LaunchInParallel()
     *
     * @return nothing
     */
    def _resetLastResultSummary()
    {
        this._lastResultSummary.clear()
        this._lastResultSummary["NUMJOBS"] = this._jobList.size()
        this._allowable_job_status_core.each() 
        {
            this._lastResultSummary["NUM"+it] = 0
        }
        this._allowable_job_status_spifi.each() 
        {
            this._lastResultSummary["NUM"+it] = 0
        }
    }


    /**
     * Increment an entry of LastResultSummary for a given key by 1.
     *
     * @param status [REQUIRED] String - The status value to increment. This can be one of:
     *                                   [SUCCESS|FAILURE|UNSTABLE|ABORTED|NOT_BUILT]
     *                                   and "NUM" is prepended to the string to generate the
     *                                   appropriate keys.
     * @return nothing
     */
    def _updateLastResultSummary(String status)
    {
        // verify parameter(s)
        assert this._allowable_job_status_core.contains(status) || this._allowable_job_status_spifi.contains(status)
        
        String statusKey = "NUM" + status
        this._lastResultSummary[statusKey] += 1
    }

}  // class JobLauncher

