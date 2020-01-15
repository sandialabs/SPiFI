Sems Pipeline Framework Infrastructure (SPiFI)
----------------------------------------------
The **S**EMS **Pi**peline **F**ramework **I**nfrastructure (SPiFI) is a library
of helpers written to simplify common tasks used in scripted Jenkins Pipeline
jobs.

These tasks include:

- Executing Shell Scripts
- Cloning Git repositories
- Launching multiple Jenkins jobs in parallel & aggregating their results
- Develop formatted emails for notifications

SPiFI provides convenience features like setting timeouts, retries, dry-run mode
(for debugging), etc. to assist pipeline development and allow the pipeline
developer to focus more on "what" the pipeline does rather than "how" to do it
(which often involves a fight with what parts of Groovy Jenkins' sandbox enables).

### Jenkins Pipelines
The [Jenkins Pipeline Plugin][2] provides a capability to build continuous
delivery pipelines into Jenkins.  A continuous delivery pipeline provides an
automated expression of a process that progresses software multiple stages of
building, testing, and deployment.  Jenkins Pipelines can be expressed in two
syntax forms, declarative and scripted.  The declarative format is a new
addition to Pipelines and is not the focus of the SPiFI library.
The scripted format is implemented using a [sandboxed][4] version of the
[Apache Groovy][3] language.

### Contact Information
Please contact the SEMS helpdesk for questions and assistance with using SPiFI
in your Jenkins jobs.

### User Guide
The full user guide can be found at
[https://sems.sandia.gov/content/sems-pipeline-framework-infrastructure-spifi-user-guide][1]


### Change Log

#### master : 2020-01-15
- Deprecation: `JenkinsTools.checked_get_parameter` is now `JenkinsTools.spifi_checked_get_parameter`
- Next version will be 1.3.0 due to the changes in JobLauncher

#### master : 2020-01-14
- Adding a new "spifi-only" job completion label, "TIMEOUT" which will be set if a job is timed-out
  via the timeout set in `JobLauncher.appendJob()`. Detection of this is via stacktrace processing
  when we handle the exception that gets kicked off, so it may be brittle down the line.
- Updates for issue #23 and #7
- Add new function to JenkinsTools:
  ```groovy
  /**
   * Pretty print a Groovy exception StackTrace
   *
   * @param env       [REQUIRED] Object - Jenkins environment (use 'this' from the Jenkins pipeline).
   * @param exception [REQUIRED] Exception - Groovy exception object.
   *
   * @return 
   */
  def spifi_get_exception_stacktrace_pretty(Map args)
  ```

#### master : 2019-12-19
- ISSUE #23: Address issue where aborted dry-run jobs report 0 time for duration and provided
             a bogus link.
  - FEATURE: Add `dry_run` entry to the `Map` returned by `JobLauncher.launchInParallel()`
             as a boolean value to capture whether or not the build was in DRY-RUN mode.
  - FEATURE: If in `dry_run` mode, the `duration` entry returned by `JobLauncher.launchInParallel()`
             will reflect the value of `dry_run_delay`.
- STYLE: Minor tweaks to ResultsUtility
  - Don't add a link to HTML, Markdown entries if the job was run in dry run and indicate it
    was a dry-run by formatting the usual build-id (`#<build_id>`) to show dry-run as `#dry_run`.
- Features will appear in version 1.2.3 or later.

#### v1.2.2 : 2019-12-18
- FEATURE: Add `JenkinsTools` source file
  - New function: `checked_get_parameter()` - provides a convenience wrapper for dealing with Jenkins
    parameters provided to jobs.

#### v1.2.0 : 
- DEPRECATION: class `ParallelJobLauncher` is changed to `JobLauncher`

#### master : 2019-05-20
- DEPRECATION: `ResultsUtility::genResultDetailTable()` replaced with `ResultsUtility::genResultDetails()`
- FEATURE: Adds JSONL option to `ResultsUtility::genResultDetails()`
- FEATURE: Add optional Boolean parameter `beautify` to `ResultsUtility::genResultDetails()`
  - Only used by JSONL format currently.
  - Default is false.
  - If set to true, it tries to beautify the output to make it more human readable.
  - Is NOT available to `ResultsUtility::genResultDetailTable()` since that is going away.

#### v1.1.4 : 2019-03-26
- DEPRECATION: ParallelJobLauncher constructor with signature `ParallelJobLauncher(env)` will be replaced by a variant that uses
  a Map parameter to be consistent with the other classes and functions in SPiFI.  Use the form: `ParallelJobLauncher(env: <env>)`
  This consturctor will be deprecated in version 2.0.0
- FEATURE: Added retry-on-regex-in-console-log option for ParallelJobLauncher jobs.
  - This allows individual jobs launched by a ParallelJobLauncher to be retried if one of a set of user provided regular expressions
    are matched in the console log.  The purpose of this is to allow the job launcher to retry jobs when intermittent errors
    occur such as "failure to clone repo from github" or "not enough compiler licenses available".
    A regex-specific retry delay can be added to allow different delays between retries based on the condition matched, but the
    total number of retries is fixed based on the job, regardless of which errors are matched.
    Retry conditions are provided in a list and the first matched is used.
  - New parameters to `ParallelJobLauncher::appendJob()`:
    - `retry_lines_to_check: Integer` : Maximum console log lines from subjob to check. Minimum = 0, Default = 200
    - `retry_max_count: Integer` : Maximum number of retries allowed (note: total attempts is retries+1). Default=0
    - `retry_conditions: List<gov.sandia.sems.spifi.DelayedRetryOnRegex>`: List of conditions to check.
  - Omitting retry-on-regex-in-console-log options does not change previous behavior.
- NEW CLASS: `gov.sandia.sems.spifi.DelayedRetryOnRegex` contains a single regular expression condition for checking.
  - File location: `src/gov/sandia/sems/spifi/DelayedRetryOnRegex.groovy`
  - Support class for the retry-on-regex-in-console-log
  - Required parameters:
    - `env: environment` contains the Jenkins environment so sandboxed things can work. Use `this` from the jenkinsfile. REQUIRED
    - `retry_regex: String` contains the regular expression we're searching for. REQUIRED
    - `retry_delay: Integer` contains the retry delay amount to use if this expression is matched. Default: 90
    - `retry_delay_units: String` contains the units of the retry delay. Allowable: [SECONDS, MINUTES, HOURS] Default: SECONDS.
- REMOVE PLUGIN DEPENDENCY: `gov.sandia.sems.spifi.Git()` no longer uses the Git plugin, which didn't work with timeouts.
  we now just use the Jenkins `checkout()` function that comes with Jenkins.
  - DEPRECATION: The only deprecation notice for this change is that the timeouts are always using MINUTES so we're removing
    the `timeout_delay_units` parameter, which doesn't really change anything since it was ignored previously anyways. Nothing
    will break if it's added however.



[1]: https://sems.sandia.gov/content/sems-pipeline-framework-infrastructure-spifi-user-guide
[2]: https://jenkins.io/doc/book/pipeline/
[3]: http://www.groovy-lang.org/
[4]: https://wiki.jenkins.io/display/JENKINS/Script+Security+Plugin

