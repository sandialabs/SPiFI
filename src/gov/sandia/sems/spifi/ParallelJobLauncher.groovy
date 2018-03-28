
////////////////////////////////////////////////////////////////////////////////
//
//  ParallelJobLauncher.groovy
//  ---------------------------
//
//  Class to handle organizing and launching Jenkins jobs in parallel.
//
//  See Also:
//  -
//
////////////////////////////////////////////////////////////////////////////////
package gov.sandia.sems.spifi;



// ----[ class ParallelJobLauncher ]------------------------------------------
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


    // ----[ Constructor ]-----------------------------------------------------
    ParallelJobLauncher(env)
    {
        // Set Parameter Default(s)
        this.env = env
        this._jobList = [:]
        this._lastResultSummary = [:]
    }


    // ----[ appendJob ]-------------------------------------------------------
    //
    //  Add a new job into the job list.  The parameter is a map that can have the
    //  following key/value pairs:
    //
    //  label            [String]  - REQUIRED Job label name.
    //  job_name         [String]  - REQUIRED Name of Jenkins job to launch.
    //  parameters       [List]    - OPTIONAL List of jenkins parameters to the job, example:
    //                                        [
    //                                            (string(name:"PARAM_NAME_1", value:"PARAM_VALUE_1")),
    //                                            (string(name:"PARAM_NAME_2", value:"PARAM_VALUE_2"))
    //                                        ]
    //                                        If there are no parameters, an empty list [] or null can be used.
    //  quiet_period     [Integer] - OPTIONAL Quiet period (seconds).  Default=0
    //  timeout          [Integer] - OPTIONAL Timeout duration.  Default=90
    //  timeout_unit     [String]  - OPTIONAL Timeout Unit {HOURS, MINUTES, SECONDS}.  Default="MINUTES"
    //  propagate_error  [Boolean] - OPTIONAL Propagate error to overall pipeline?  { true, false }
    //  dry_run          [Boolean] - OPTIONAL If true, then use dry-run mode (job will not be launched).
    //  dry_run_status   [String]  - OPTIONAL If dry_run is true, set status to this value.
    //                                        Must be one of {SUCCESS, FAILURE, UNSTABLE, ABORTED, NOT_BUILT}
    //  dry_run_delay    [Integer] - OPTIONAL If dry_run is true, this introduces a delay (in seconds)
    //                                        for the 'simulated' job.  Default: 30
    //  monitor_node     [String]  - OPTIONAL Node expression for the Jenkins "node" where the job
    //                                        monitor will run.  Note: This does not affect where the
    //                                        job itself will run since that's controlled by the job
    //                                        itself (or should be).  If omitted then no 'monitor' job
    //                                        will be created.
    //
    //  Example:
    //     appendJob(label: "T1", job_name: "Jenkins-Job-To-Launch")
    //
    def appendJob(Map params)
    {
        assert params.containsKey("label")
        assert params.containsKey("job_name")

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

        this.env.println "Add job ${job.label}"
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


    // ----[ clearJobs ]------------------------------------------------------
    //
    //  Clear / Reset the job list and result summary.
    //
    def clearJobs()
    {
        this._jobList.clear()
        this._resetLastResultSummary()
    }


    // ----[ launchInParallel ]-------------------------------------------
    //
    //  Launch the jobs in parallel
    //
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
            this.env.println ">>> r = ${r}"
            this._updateLastResultSummary(r.value["status"])
        }

        return results
    }



    // ----[ getLastResultSummary ]--------------------------------------------
    //
    // Get a summary of the most recent set of tests.
    //
    // Returns a Map: ["NUMTESTS"    : <# of jobs run>,
    //                 "NUMSUCCESS"  : <# of SUCCESS jobs>,
    //                 "NUMFAILURE"  : <# of FAILURE jobs>,
    //                 "NUMUNSTABLE" : <# of UNSTABLE jobs>
    //                 "NUMABORTED"  : <# of ABORTED jobs>
    //                 "NUMNOT_BUILT": <# of NOT_BUILT jobs>
    //                ]
    //
    def getLastResultSummary()
    {
        return this._lastResultSummary
    }


    // ----[ printJobList ]---------------------------------------------------
    //
    //  Pretty print the job list.
    //
    def printJobList()
    {
        String strJobs = "----[ JobList ]----------\n"
        for(job in this._jobList)
        {
            strJobs += "${job.key}:\n"
            // strJobs += " - ${job.value}\n"
            strJobs += " - jenkins_job_name: " + job.value["jenkins_job_name"] + "\n"
            strJobs += " - monitor_node    : " + job.value["monitor_node"] + "\n"
            strJobs += " - quiet_period    : " + job.value["quiet_period"] + "\n"
            strJobs += " - timeout         : " + job.value["timeout"] + "\n"
            strJobs += " - timeout_unit    : " + job.value.timeout_unit + "\n"
            strJobs += " - propagate_error : " + job.value["propagate_error"] + "\n"
            if(job.value.dry_run == true)
            {
                strJobs += " - dry_run         : " + job.value.dry_run + "\n"
                strJobs += " - dry_run_status  : " + job.value.dry_run_status + "\n"
                strJobs += " - dry_run_delay   : " + job.value.dry_run_delay + "\n"
            }
            strJobs += " - parameters      : \n"
            for(param in job.value["parameters"])
            {
                strJobs += "     " + param + "\n"
            }
        }
        this.env.println "${strJobs}"
    }


    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------


    // ----[ _jobBody ]---------------------------------------------------------
    //
    // The actual body 'launcher' of the job.
    //
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
                this.env.println ">>> DRY RUN MODE <<<"
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
            this.env.println "${job.value.jenkins_job_name} = ${results[job.key]}"
        }
        return results
    }


    // ----[ _jobBodyWithMonitorNode ]-----------------------------------------
    //
    // Launch the job with a monitor node.
    //
    def _jobBodyWithMonitorNode(job)
    {
        def results = [:]
        this.env.node(job.value.monitor_node)
        {
            results << this._jobBody(job)
        }
        return results
    }


    // ----[ _jobBodyWithoutMonitorNode ]--------------------------------------
    //
    // Launch the job without a monitor node.
    //
    def _jobBodyWithoutMonitorNode(job)
    {
        return this._jobBody(job)
    }


    // ----[ _resetLastResultSummary ]------------------------------------------
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


    // ----[ _updateLastResultSummary ]-----------------------------------------
    def _updateLastResultSummary(String status)
    {
        String statusKey = "NUM" + status
        this._lastResultSummary[statusKey] += 1
    }

}  // class parallelTestLauncher

