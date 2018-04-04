
/**
 * ParallelJobLauncher.groovy
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
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-04
 *
 */
package gov.sandia.sems.spifi;



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
 *     def launcher = gov.sandia.sems.spifi.ParallelJobLaucher(this)
 *     launcher.appendJob("Job 1", "JENKINS_JOB_ALPHA")
 *     launcher.appendJob("Job 2", "JENKINS_JOB_BRAVO")
 *     launcher.appendJob("Job 2", "JENKINS_JOB_CHARLIE")
 *     def results = launcher.launchInParallel()
 *     def summary = launcher.getLastResultSummary()
 *     launcher.clearJobs()
 *
 */
class ParallelJobLauncher
{
    // helper subclass for dynamic parameters on appendJob
    class _parameters
    {
        String  label           = "__REQUIRED__"
        String  job_name        = "__REQUIRED__"
        List    parameters      = null
        Integer quiet_period    = 0
        Integer timeout         = 90
        String  timeout_unit    = "MINUTES"
        Boolean propagate_error = false
        Boolean dry_run         = false
        String  dry_run_status  = "SUCCESS"
        Integer dry_run_delay   = 30
        String  monitor_node    = ""
    }


    // Member Variables
    private static env
    private Map<String,String> _jobList                   // The jobs + their parameters
    private Map<String,String> _lastResultSummary         // Status on the latest results


    /**
     * Constructor for the job launcher.
     *
     * @param env [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
     *
     */
    ParallelJobLauncher(env)
    {
        // Set Parameter Default(s)
        this.env = env
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
     * @param timeout         [OPTIONAL] Integer - Timeout duration.
     *                                             Default: 90
     * @param timeout_unit    [OPTIONAL] String  - Timeout Unit {HOURS, MINUTES, SECONDS}.
     *                                             Default="MINUTES"
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
     * @return nothing
     */
    def appendJob(Map params)
    {
        // Check for required parameters
        if(!params.containsKey("label"))
        {
            throw new Exception("gov.sandia.sems.spifi.ParallelJobLauncher::appendJob missing required parameter: label")
        }
        if(!params.containsKey("job_name"))
        {
            throw new Exception("gov.sandia.sems.spifi.ParallelJobLauncher::appendJob missing required parameter: job_name")
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

        this.env.println "[SPiFI]> Append job ${job.label}"

        this._jobList[job.label] = [ jenkins_job_name: job.job_name,
                                     parameters:       job.parameters,
                                     quiet_period:     job.quiet_period,
                                     timeout:          job.timeout,
                                     timeout_unit:     job.timeout_unit,
                                     propagate_error:  job.propagate_error,
                                     dry_run:          job.dry_run,
                                     dry_run_status:   job.dry_run_status,
                                     dry_run_delay:    job.dry_run_delay,
                                     monitor_node:     job.monitor_node
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
     *             label: Jenkins result status ( SUCCESS | FAILURE | UNSTABLE | ABORTED | NOT_BUILT )
     */
    def launchInParallel()
    {
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
        this.env.parallel builders

        // Update the result summary
        this._resetLastResultSummary()
        results.each
        {   _r ->
            def r = _r
            this._updateLastResultSummary(r.value["status"])

            this.env.println "[SPiFI]> UpdateLastResultSummary:\n${r}"
        }

        return results
    }


    /**
     * Return a summary of the most recent set of tests run by LaunchInParallel.
     *
     * @return Map with a summary of the most recent set of tests. The following
     *             key value pairs are included in the output:
     *             NUMTESTS     - The # of jobs that were run.
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
            strJobs += "[SPiFI]>   - jenkins_job_name: " + job.value["jenkins_job_name"] + "\n"
            strJobs += "[SPiFI]>   - monitor_node    : " + job.value["monitor_node"] + "\n"
            strJobs += "[SPiFI]>   - quiet_period    : " + job.value["quiet_period"] + "\n"
            strJobs += "[SPiFI]>   - timeout         : " + job.value["timeout"] + "\n"
            strJobs += "[SPiFI]>   - timeout_unit    : " + job.value.timeout_unit + "\n"
            strJobs += "[SPiFI]>   - propagate_error : " + job.value["propagate_error"] + "\n"
            if(job.value.dry_run == true)
            {
                strJobs += "[SPiFI]>   - dry_run         : " + job.value.dry_run + "\n"
                strJobs += "[SPiFI]>   - dry_run_status  : " + job.value.dry_run_status + "\n"
                strJobs += "[SPiFI]>   - dry_run_delay   : " + job.value.dry_run_delay + "\n"
            }
            strJobs += "[SPiFI]>   - parameters      : \n"
            for(param in job.value["parameters"])
            {
                strJobs += "[SPiFI]>       " + param + "\n"
            }
        }
        // Strip off trailing newline...
        strJobs = strJobs.replaceAll("\\s\$","")

        this.env.println "${strJobs}"
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
        def results = [:]

        results[job.key] = [:]
        results[job.key]["job"]      = job.value.jenkins_job_name
        results[job.key]["status"]   = ""
        results[job.key]["id"]       = 0
        results[job.key]["url"]      = ""
        results[job.key]["duration"] = 0

        // Everything after this level is executed...
        this.env.timeout(time: job.value.timeout, unit: job.value.timeout_unit)
        {
            if(job.value.dry_run)
            {
                this.env.println "[SPiFI]> ${job.value.jenkins_job_name} Execute in dry-run mode\n" +
                                 "[SPiFI]> - Delay : ${job.value.dry_run_delay} seconds\n" +
                                 "[SPiFI]> - Status: ${job.value.dry_run_status}"

                results[job.key]["status"] = job.value.dry_run_status
                this.env.sleep job.value.dry_run_delay
            }
        else
            {
                // Note: status is a org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper object
                //       http://javadoc.jenkins.io/plugin/workflow-support/org/jenkinsci/plugins/workflow/support/steps/build/RunWrapper.html
                //
                def status = this.env.build job        : job.value.jenkins_job_name,
                                            parameters : job.value.parameters,
                                            quietPeriod: job.value.quiet_period,
                                            propagate  : job.value.propagate_error

                // Save selected parts of the result to the results
                results[job.key]["status"]   = status.getResult()
                results[job.key]["id"]       = status.getId()
                results[job.key]["url"]      = status.getAbsoluteUrl()
                results[job.key]["duration"] = status.getDuration()
            }

            this.env.println "[SPiFI]> ${job.value.jenkins_job_name} = ${results[job.key]}"

        }
        return results
    }


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
        this.env.node(job.value.monitor_node)
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
        this._lastResultSummary["NUMTESTS"]     = this._jobList.size()
        this._lastResultSummary["NUMSUCCESS"]   = 0
        this._lastResultSummary["NUMFAILURE"]   = 0
        this._lastResultSummary["NUMUNSTABLE"]  = 0
        this._lastResultSummary["NUMABORTED"]   = 0
        this._lastResultSummary["NUMNOT_BUILT"] = 0
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
        assert "SUCCESS"==status || "FAILURE"==status || "UNSTABLE"==status || "ABORTED"==status || "NOT_BUILT"==status

        String statusKey = "NUM" + status
        this._lastResultSummary[statusKey] += 1
    }

}  // class parallelTestLauncher

