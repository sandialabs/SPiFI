////////////////////////////////////////////////////////////////////////////////
//
//  parallelTestLauncher.groovy
//  ---------------------------
//
//  A class to handle organizing and launching jenkins jobs in parallel as sub
//  tests.  Returns individual test results as well as a summary of the results
//  of all tests. 
//  Allows per-test timeout setting and a per-test dry-run mode for debugging.
//
//  Prerequisites:
//  - script_env.groovy
//
//  See Also:
//  - 
//
////////////////////////////////////////////////////////////////////////////////



// ----[ class parallelTestLauncher ]------------------------------------------
//
// Class to handle organizing and launching Jenkins jobs in parallel.
//
class parallelTestLauncher
{
    // helper subclass for dynamic parameters on appendTask
    class _task_parameters
    {
        String  label           = "__REQUIRED__"
        String  job_name        = "__REQUIRED__"
        List    parameters      = null
        Integer quiet_period    = 10
        Integer timeout         = 90
        String  timeout_unit    = "MINUTES"
        Boolean propagate_error = false
        Boolean dry_run         = false
        String  dry_run_status  = "SUCCESS"
        Integer dry_run_delay   = 30
        String  monitor_node    = "master"
    }


    // Member Variables
    private Map<String,String> _taskList                  // The tasks + their parameters
    private Map<String,String> _lastResultSummary         // Status on the latest results


    // ----[ Constructor ]-----------------------------------------------------
    parallelTestLauncher()
    {
        // Set Parameter Default(s)
        this._taskList = [:]
        this._lastResultSummary = [:]
    }


    // ----[ appendTask ]------------------------------------------------------
    //
    //  Add a new task/job into the task list.  The parameter is a map that can have the
    //  following key/value pairs:
    //
    //  label            [String]  - REQUIRED Task label name.
    //  job_name         [String]  - REQUIRED Name of Jenkins job to launch.
    //  parameters       [List]    - OPTIONAL List of jenkins parameters to the job, example:
    //                                        [
    //                                            (string(name:"PARAM_NAME_1", value:"PARAM_VALUE_1"),
    //                                            (string(name:"PARAM_NAME_2", value:"PARAM_VALUE_2")
    //                                        ]
    //                                        If there are no parameters, an empty list [] or null can be used.
    //  quiet_period     [Integer] - OPTIONAL Quiet period (seconds).  Default=1
    //  timeout          [Integer] - OPTIONAL Timeout duration.  Default=90
    //  timeout_unit     [String]  - OPTIONAL Timeout Unit {HOURS, MINUTES, SECONDS}.  Default="MINUTES"
    //  propagate_error  [Boolean] - OPTIONAL Propagate error to overall pipeline?  { true, false }
    //  dry_run          [Boolean] - OPTIONAL If true, then use dry-run mode (task will not be launched).
    //  dry_run_status   [String]  - OPTIONAL If dry_run is true, set status to this value.
    //                                        Must be one of {SUCCESS, FAILURE, UNSTABLE}
    //  dry_run_delay    [Integer] - OPTIONAL If dry_run is true, this introduces a delay (in seconds)
    //                                        for the 'simulated' job.  Default: 30
    //  monitor_node     [String]  - OPTIONAL Node expression for the Jenkins "node" where the task
    //                                        monitor will run.  Note: This does not affect where the
    //                                        job itself will run since that's controlled by the job
    //                                        itself (or should be).
    //
    //  Example:
    //     appendTask(label: "T1", job_name: "Jenkins-Job-To-Launch")
    //
    def appendTask(Map params)
    {
        assert params.containsKey("label")
        assert params.containsKey("job_name")

        def task = new _task_parameters()

        task.label    = params.label
        task.job_name = params.job_name

        if(params.containsKey("parameters"))      { task.parameters      = params.parameters      }
        if(params.containsKey("quiet_period"))    { task.quiet_period    = params.quiet_period    }
        if(params.containsKey("timeout"))         { task.timeout         = params.timeout         }
        if(params.containsKey("timeout_unit"))    { task.timeout_unit    = params.timeout_unit    }
        if(params.containsKey("propagate_error")) { task.propagate_error = params.propagate_error }
        if(params.containsKey("dry_run"))         { task.dry_run         = params.dry_run         }
        if(params.containsKey("dry_run_status"))  { task.dry_run_status  = params.dry_run_status  }
        if(params.containsKey("dry_run_delay"))   { task.dry_run_delay   = params.dry_run_delay   }
        if(params.containsKey("monitor_node"))    { task.monitor_node    = params.monitor_node    }

        Script.env.println "Add task ${task.label}"
        this._taskList[task.label] = [ jenkins_job_name: task.job_name,
                                       parameters:       task.parameters,
                                       quiet_period:     task.quiet_period,
                                       timeout:          task.timeout,
                                       timeout_unit:     task.timeout_unit,
                                       propagate_error:  task.propagate_error,
                                       dry_run:          task.dry_run,
                                       dry_run_status:   task.dry_run_status,
                                       dry_run_delay:    task.dry_run_delay,
                                       monitor_node:     task.monitor_node
                                     ]
    }


    // ----[ clearTasks ]------------------------------------------------------
    //
    //  Clear / Reset the task list and result summary.
    //
    def clearTasks()
    {
        this._taskList.clear()
        this._resetLastResultSummary()
    }


    // ----[ launchTasksInParallel ]-------------------------------------------
    //
    //  Launch the jobs in parallel
    //
    def launchTasksInParallel()
    {
        def builders = [:]
        def results  = [:]

        // Construct the execution 'block' that will be executed concurrently for each of the tasks
        this._taskList.each
        {   _t ->
            def task = _t
            results[task.key] = "UNKNOWN"
            builders[task.key] =
            {
                // Everything within this scope level is executed...

                Script.env.node(task.value.monitor_node)
                {
                    Script.env.timeout(time: task.value.timeout, unit: task.value.timeout_unit)
                    {
                        if(task.value.dry_run)
                        {
                            Script.env.println ">>> DRY RUN MODE <<<"
                            results[task.key] = task.value.dry_run_status
                            Script.env.sleep task.value.dry_run_delay
                        }
                        else
                        {
                            def status = Script.env.build job        : task.value.jenkins_job_name,
                                                          parameters : task.value.parameters,
                                                          quietPeriod: task.value.quiet_period,
                                                          propagate  : task.value.propagate_error

                            // Save the result of the test that was run.
                            results[task.key] = status.getResult()
                        }
                        Script.env.println "${task.value.jenkins_job_name} = ${results[task.key]}"
                    }
                }
            }
        }

        // Launch the jobs in parallel
        Script.env.parallel builders

        // Update the result summary
        this._resetLastResultSummary()
        results.each
        { _r ->
          def r = _r
            Script.env.println ">>> r = ${r}"
            this._updateLastResultSummary(r.value)
        }

        return results
    }


    // ----[ getLastResultSummary ]--------------------------------------------
    //
    // Get a summary of the most recent set of tests.
    //
    // Returns a Map: ["NUMTESTS"   : <# of tasks run>,
    //                 "NUMSUCCESS" : <# of SUCCESS tasks>,
    //                 "NUMFAILURE" : <# of FAILURE tasks>,
    //                 "NUMUNSTABLE": <# of UNSTABLE tasks>
    //                ]
    //
    def getLastResultSummary()
    {
        return this._lastResultSummary
    }


    // ----[ printTaskList ]---------------------------------------------------
    //
    //  Pretty print the task list.
    //
    def printTaskList()
    {
        String strTasks = "----[ TaskList ]----------\n"
        for(task in this._taskList)
        {
            strTasks += "${task.key}:\n"
            // strTasks += " - ${task.value}\n"
            strTasks += " - jenkins_job_name: " + task.value["jenkins_job_name"] + "\n"
            strTasks += " - monitor_node    : " + task.value["monitor_node"] + "\n"
            strTasks += " - quiet_period    : " + task.value["quiet_period"] + "\n"
            strTasks += " - timeout         : " + task.value["timeout"] + "\n"
            strTasks += " - timeout_unit    : " + task.value.timeout_unit + "\n"
            strTasks += " - propagate_error : " + task.value["propagate_error"] + "\n"
            strTasks += " - dry_run         : " + task.value.dry_run + "\n"
            strTasks += " - dry_run_status  : " + task.value.dry_run_status + "\n"
            strTasks += " - dry_run_delay   : " + task.value.dry_run_delay + "\n"
            strTasks += " - parameters      : \n"
            for(param in task.value["parameters"])
            {
                strTasks += "     " + param + "\n"
            }
        }
        Script.env.println "${strTasks}"
    }


    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------


    // ----[ _resetLastResultSummary ]------------------------------------------
    def _resetLastResultSummary()
    {
        this._lastResultSummary.clear()
        this._lastResultSummary["NUMTESTS"]    = this._taskList.size()
        this._lastResultSummary["NUMSUCCESS"]  = 0
        this._lastResultSummary["NUMFAILURE"]  = 0
        this._lastResultSummary["NUMUNSTABLE"] = 0
    }


    // ----[ _updateLastResultSummary ]-----------------------------------------
    def _updateLastResultSummary(String status)
    {
        String statusKey = "NUM" + status
        this._lastResultSummary[statusKey] += 1
    }

}  // class parallelTestLauncher



