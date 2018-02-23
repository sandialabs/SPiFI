
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
    // Member Variables
    private Map _taskList                  // The tasks + their parameters
    private Map _lastResultSummary         // Status on the latest results


    // ----[ Constructor ]-----------------------------------------------------
    parallelTestLauncher()
    {
        // Set Parameter Default(s)
        this._taskList = [:]
        this._lastResultSummary = [:]
    }


    // ----[ appendTask ]------------------------------------------------------
    //
    //  Add a new task/job into the task list.
    //
    //  param task_label       [String]  - REQUIRED Task label name.
    //  param jenkins_job_name [String]  - REQUIRED Name of Jenkins job to launch.
    //  param parameters       [List]    - REQUIRED List of jenkins parameters to the job, example:
    //                                              [
    //                                                  (string(name:"PARAM_NAME_1", value:"PARAM_VALUE_1"),
    //                                                  (string(name:"PARAM_NAME_2", value:"PARAM_VALUE_2")
    //                                              ]
    //                                              If there are no parameters, an empty list [] or null can be used.
    //  param quiet_period     [Integer] - REQUIRED Quiet period (seconds)
    //  param timeout          [Integer] - REQUIRED Timeout duration.
    //  param timeout_unit     [String]  - REQUIRED Timeout Unit {HOURS, MINUTES, SECONDS}
    //  param propagate_error  [Boolean] - REQUIRED Propagate error to overall pipeline?  { true, false }
    //  param dry_run          [Boolean] - OPTIONAL If true, then use dry-run mode (task will not be launched).
    //  param dry_run_status   [String]  - OPTIONAL If dry_run is true, set status to this value.
    //                                              Must be one of {SUCCESS, FAILURE, UNSTABLE}
    //
    def appendTask(String  task_label,
                   String  jenkins_job_name,
                   List    parameters,
                   Integer quiet_period,
                   Integer timeout,
                   String  timeout_unit,
                   Boolean propagate_error,
                   Boolean dry_run=false,
                   String  dry_run_status="SUCCESS")
    {
        Script.env.println "Add task ${task_label}"
        this._taskList[task_label] = [ jenkins_job_name: jenkins_job_name,
                                       parameters:       parameters,
                                       quiet_period:     quiet_period,
                                       timeout:          timeout,
                                       timeout_unit:     timeout_unit,
                                       propagate_error:  propagate_error,
                                       dry_run:          dry_run,
                                       dry_run_status:   dry_run_status
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
                Script.env.timeout(time: task.value.timeout, unit: task.value.timeout_unit)
                {
                    Script.env.node
                    {
                        if(task.value.dry_run)
                        {
                            Script.env.println ">>> DRY RUN MODE <<<"
                            results[task.key] = task.value.dry_run_status
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
            strTasks += " - quiet_period    : " + task.value["quiet_period"] + "\n"
            strTasks += " - timeout         : " + task.value["timeout"] + "\n"
            strTasks += " - timeout_unit    : " + task.value.timeout_unit + "\n"
            strTasks += " - propagate_error : " + task.value["propagate_error"] + "\n"
            strTasks += " - dry_run         : " + task.value.dry_run + "\n"
            strTasks += " - dry_run_status  : " + task.value.dry_run_status + "\n"
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

















