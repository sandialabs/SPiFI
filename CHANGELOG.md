# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- Next version will be v2.1.0

### Added
- Add `format_duration_hms` to `ResultsUtility.genResultDetails()` which causes the
  duration value to be reported as `DD:HH:MM:SS.SS`
  - For JSONL output, the `duration` field will be a string if using HMS.
  - For JSONL output, the `duration` field remains a float if not using HMS.
  - Resolves Issue #34
- Add new parameter `in_place` to `JenkinsTools.spifi_checked_get_parameter()` which enables
  in-place setting of Aparameters.

### Changed
### Removed


## [2.0.0] - 2020-02-17
- v2.0.0 branch released as the first branch in the 2.x line.

### Removed
- deprecations from v1.x are removed.


## [1.3.2] - 2020-02-17
- v1.3.2 branch released as the final branch in the 1.x line.

### Added
- Update `ResultsUtility` functions to use `gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check()`
- Add `verbose` parameter to ResultsUtility.genResultSummaryTable() parameter list.
- Add `verbose` parameter to ResultsUtility.genResultDetails() parameter list.
- Add `verbose` parameter to HTMLUtility.generate()
- Add `verbose` paramter to HTMLUtility.generateList()
- Add a new optional `verbose` parameter to `JobLauncher.appendJob()` for use
  by SPiFI developers to turn on additional debugging messages.  Currently
  this is only used by `gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check()`
  but we could expand its use later if we want to.
  - This will not affect any backwards compatibility or deprecate anything.

### Changed
- Update `HTMLUtility` functions to use `gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check()`
- Bug fix for [Issue 27][5] : Fixed alternating color bug in the HTML generated tables.

### Removed


## [1.3.1] - 2020-01-21
### Added
- Add new parameters to `Git.clone()`
  - `recurse_submodules` - enable recursive submodule clone of the given repository.
  - `shallow` - enable a shallow clone of the repositories (limit depth to 50)

### Changed
### Removed


## [1.3.0] - 2020-01-15
### Added
- Adding a new "spifi-only" job completion label, "TIMEOUT" which will be set if a job is timed-out
  via the timeout set in `JobLauncher.appendJob()`. Detection of this is via stacktrace processing
  when we handle the exception that gets kicked off, so it may be brittle down the line.

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

### Changed
- ISSUE #23: Address issue where aborted dry-run jobs report 0 time for duration and provided
             a bogus link.
  - FEATURE: Add `dry_run` entry to the `Map` returned by `JobLauncher.launchInParallel()`
             as a boolean value to capture whether or not the build was in DRY-RUN mode.
  - FEATURE: If in `dry_run` mode, the `duration` entry returned by `JobLauncher.launchInParallel()`
             will reflect the value of `dry_run_delay`.
- STYLE: Minor tweaks to ResultsUtility
  - Don't add a link to HTML, Markdown entries if the job was run in dry run and indicate it
    was a dry-run by formatting the usual build-id (`#<build_id>`) to show dry-run as `#dry_run`.
- Issue #7 fixes.

### Removed
- Deprecation: `JenkinsTools.checked_get_parameter` is now `JenkinsTools.spifi_checked_get_parameter`


## [1.2.2] - 2019-12-18
### Added
- FEATURE: Add `JenkinsTools` source file
  - New function: `checked_get_parameter()` - provides a convenience wrapper for dealing with Jenkins
    parameters provided to jobs.

### Changed
### Removed


## [1.2.0] - 2019-07-30
### Added
### Changed
- FEATURE: Adds JSONL option to `ResultsUtility::genResultDetails()`
- FEATURE: Add optional Boolean parameter `beautify` to `ResultsUtility::genResultDetails()`
  - Only used by JSONL format currently.
  - Default is false.
  - If set to true, it tries to beautify the output to make it more human readable.
  - Is NOT available to `ResultsUtility::genResultDetailTable()` since that is going away.

### Removed
- Deprecation: class `ParallelJobLauncher` changed to `JobLauncher`
- DEPRECATION: `ResultsUtility::genResultDetailTable()` replaced with `ResultsUtility::genResultDetails()`


## [1.1.4] - 2019-03-26
### Added
- NEW CLASS: `gov.sandia.sems.spifi.DelayedRetryOnRegex` contains a single regular expression condition for checking.
  - File location: `src/gov/sandia/sems/spifi/DelayedRetryOnRegex.groovy`
  - Support class for the retry-on-regex-in-console-log
  - Required parameters:
    - `env: environment` contains the Jenkins environment so sandboxed things can work. Use `this` from the jenkinsfile. REQUIRED
    - `retry_regex: String` contains the regular expression we're searching for. REQUIRED
    - `retry_delay: Integer` contains the retry delay amount to use if this expression is matched. Default: 90
    - `retry_delay_units: String` contains the units of the retry delay. Allowable: [SECONDS, MINUTES, HOURS] Default: SECONDS.

### Changed
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

### Removed
- DEPRECATION: ParallelJobLauncher constructor with signature `ParallelJobLauncher(env)` will be replaced by a variant that uses
  a Map parameter to be consistent with the other classes and functions in SPiFI.  Use the form: `ParallelJobLauncher(env: <env>)`
  This consturctor will be deprecated in version 2.0.0
- REMOVE PLUGIN DEPENDENCY: `gov.sandia.sems.spifi.Git()` no longer uses the Git plugin, which didn't work with timeouts.
  we now just use the Jenkins `checkout()` function that comes with Jenkins.
  - DEPRECATION: The only deprecation notice for this change is that the timeouts are always using MINUTES so we're removing
    the `timeout_delay_units` parameter, which doesn't really change anything since it was ignored previously anyways. Nothing
    will break if it's added however.



<!--
- Links
-->
[1]: https://gitlab-ex.sandia.gov/SEMS/sems-pipeline-framework-infrastructure/-/wikis/home
[2]: https://jenkins.io/doc/book/pipeline/
[3]: http://www.groovy-lang.org/
[4]: https://wiki.jenkins.io/display/JENKINS/Script+Security+Plugin
[5]: https://gitlab-ex.sandia.gov/SEMS/sems-pipeline-framework-infrastructure/issues/27
