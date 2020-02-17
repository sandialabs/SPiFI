#!/usr/bin/env groovy
/**
 * ResultUtility.groovy
 *
 * Formatter to generate pretty printable tables using output from jobs run via JobLauncher
 * HTML formatted items are compatible with the styles generated by HTMLUtility.
 *
 * @see JobLauncher
 * @see HTMLUtility
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-19
 *
 */
package gov.sandia.sems.spifi;

import groovy.json.*


/**
 *
 *
 */
class ResultsUtility implements Serializable
{
    // Member Variables
    private static _env           // Requird Jenkins environment.
    private _allowable_job_status_core
    private _allowable_job_status_spifi

    /**
     * Constructor for ResultsUtility
     *
     * @param env     [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
     * @param verbose [OPTIONAL] Boolean - Toggle extra verbosity for debugging (c'tor only). (v1.3.1)
     *                                     Default: false.
     *
     * @return nothing
     */
    ResultsUtility(Map params)
    {
        //
        // Begin parameter validation
        //
        if(!params.containsKey("env"))
        {
            throw new Exception("[SPiFI] Missing required parameter: 'env'")
        }
        this._env = params.env

        Map params_expected = [ "env":     [option: "R"],
                                "verbose": [option: "O"]
                              ]
        Boolean params_ok = gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check(env: this._env,
                                                                                   params_expected: params_expected,
                                                                                   params_received: params,
                                                                                   verbose: params.containsKey("verbose") && params.verbose
                                                                                   )
        if( !params_ok )
        {
            throw new Exception("SPiFI ERROR: parameter check failed for ResultsUtility.ResultsUtility")
        }
        //
        // Completed parameter validation
        //

        this._allowable_job_status_core  = ["SUCCESS"  : "R",
                                            "FAILURE"  : "R",
                                            "UNSTABLE" : "R",
                                            "ABORTED"  : "O",
                                            "NOT_BUILT": "O",
                                           ]

        this._allowable_job_status_spifi = ["TIMEOUT"  : "O"
                                           ]
    }



    /**
     * Generate a pretty printable summary table for emails, etc.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::getLastResultSummary()
     *
     * @param summary  [REQUIRED] Map    - Output Summary information from a call to
     *                                     JobLauncher.getLastResultSummary()
     * @param format   [OPTIONAL] String - Type of output table to generate.
     *                                     Must be one of: [ ASCII | HTML | MARKDOWN ]
     *                                     Default: ASCII
     * @param verbose [OPTIONAL] Boolean - Toggle extra verbosity for debugging. (1.3.1)
     *                                     Default: false
     *
     * @return String containing the Summary table for results that can be inserted into
     *                the console log, email, etc.
     */
    @NonCPS
    def genResultSummaryTable(Map params)
    {
        //
        // Begin parameter validation
        //
        Map params_expected = [ "summary":  [option: "R"],
                                "format":   [option: "O"],
                                "verbose":  [option: "O"]
                              ]
        Boolean params_ok = gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check(env: this._env,
                                                                                   params_expected: params_expected,
                                                                                   params_received: params,
                                                                                   verbose: params.containsKey("verbose") && params.verbose
                                                                                   )
        if( !params_ok )
        {
            throw new Exception("SPiFI ERROR: parameter check failed for ResultsUtility.genResultSummaryTable()")
        }

        // If format isn't provided, make it ASCII
        if(!params.containsKey("format"))
        {
            params["format"] = "ASCII"
        }

        assert params.summary.containsKey("NUMJOBS")

        // Check that we handle the status types that are provided by Jenkins
        this._allowable_job_status_core.each()
        {
            assert params.summary.containsKey("NUM" + it.key)
        }

        // Check the extra status types that are specific to SPiFI
        this._allowable_job_status_spifi.each()
        {
            assert params.summary.containsKey("NUM" + it.key)
        }
        //
        // Completed parameter validation
        //

        String output = ""
        switch(params.format)
        {
            case "HTML":
                output += this._genResultSummaryTableHTML(params.summary)
                break
            case "MARKDOWN":
                output += this._genResultSummaryTableMarkdown(params.summary)
                break
            case "ASCII":
                output += this._genResultSummaryTableASCII(params.summary)
                break
            case "JSONL":
                throw new Exception("[SPiFI] genResultSummaryTable does not provide JSONL output")
                break
            default:
                output += this._genResultSummaryTableASCII(params.summary)
                break
        }
        return output
    }


    /**
     * Generate a list of results by job from the results output of
     * JobLauncher.launchInParallel()
     *
     * @see gov.sandia.sems.spifi.JobLauncher::launchInParallel()
     *
     * @param results         [REQUIRED] Map     - Results from a call to
     *                                             JobLauncher.launchInParallel()
     * @param format          [OPTIONAL] String  - Type of output table to generate.
     *                                             Must be one of: [ ASCII | HTML | MARKDOWN | JSONL ]
     *                                             Default: ASCII
     * @param link_to_console [OPTIONAL] Boolean - If true, the links provided by HTML
     *                                             and Markdown will be to the console
     *                                             output on Jenkins.  If false, the
     *                                             links will point at the job itself.
     *                                             Default: false
     * @param beautify        [OPTIONAL] Boolean - Toggle beautification of the output to make it more
     *                                             human-readable.  This option is only used when
     *                                             format is set to one of: [ JSONL ]
     *                                             Default: false
     * @param verbose [OPTIONAL] Boolean - Toggle extra verbosity for debugging. (1.3.1)
     *                                     Default: false
     *
     * @return String containing the Detail table for results that can be inserted into
     *                the console log, email, etc.
     *
     */
    @NonCPS
    def genResultDetails(Map params)
    {
        //
        // Begin parameter validation
        //
        Map params_expected = [ "results":         [option: "R"],
                                "format":          [option: "O"],
                                "link_to_console": [option: "O"],
                                "beautify":        [option: "O"],
                                "verbose":         [option: "O"]
                              ]
        Boolean params_ok = gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check(env: this._env,
                                                                                   params_expected: params_expected,
                                                                                   params_received: params,
                                                                                   verbose: params.containsKey("verbose") && params.verbose
                                                                                   )
        if( !params_ok )
        {
            throw new Exception("SPiFI ERROR: parameter check failed for ResultsUtility.genResultDetails()")
        }

        // If format isn't provided, make it ASCII
        if(!params.containsKey("format"))
        {
            params["format"] = "ASCII"
        }
        // Set link_to_console if it's not already set.
        if(!params.containsKey("link_to_console"))
        {
            params["link_to_console"] = false
        }
        // If unspecified, set the beautify parameter to false.
        if(!params.containsKey("beautify"))
        {
            params["beautify"] = false
        }
        //
        // Completed parameter validation
        //

        String output = ""

        switch(params.format)
        {
            case "HTML":
                output += this._genResultDetailTableHTML(params)
                break
            case "MARKDOWN":
                output += this._genResultDetailTableMarkdown(params)
                break
            case "ASCII":
                output += this._genResultDetailTableASCII(params)
                break
            case "JSONL":
                output += this._genResultDetailTableJSONL(params)
                break
            default:
                output += this._genResultDetailTableASCII(params)
                break
        }
        return output
    }




    /**
     * *** DEPRECATED ***
     * Generate a list of results by job from the results output of
     * JobLauncher.launchInParallel()
     *
     * @see gov.sandia.sems.spifi.JobLauncher::launchInParallel()
     *
     * @param results         [REQUIRED] Map     - Results from a call to
     *                                             JobLauncher.launchInParallel()
     * @param format          [OPTIONAL] String  - Type of output table to generate.
     *                                             Must be one of: [ ASCII | HTML | MARKDOWN | JSONL ]
     *                                             Default: ASCII
     * @param link_to_console [OPTIONAL] Boolean - If true, the links provided by HTML
     *                                             and Markdown will be to the console
     *                                             output on Jenkins.  If false, the
     *                                             links will point at the job itself.
     *                                             Default: false
     *
     * @return String containing the Detail table for results that can be inserted into
     *                the console log, email, etc.
     *
     */
    @Deprecated
    def genResultDetailTable(Map params)
    {
        this._env.println "SPiFI DEPRECATION NOTICE]>\n" +
                          "SPiFI DEPRECATION NOTICE]> ResultsUtility::genResultDetailTable() is deprecated, use\n" +
                          "SPiFI DEPRECATION NOTICE]> ResultsUtility::genResultDetails() instead.\n" +
                          "SPiFI DEPRECATION NOTICE]>"

        // If format isn't provided, make it ASCII
        if(!params.containsKey("format"))
        {
            params["format"] = "ASCII"
        }

        // Set link_to_console if it's not already set.
        if(!params.containsKey("link_to_console"))
        {
            params["link_to_console"] = false
        }

        // Check required param: results
        assert params.containsKey("results")

        String output = ""

        switch(params.format)
        {
            case "HTML":
                output += this._genResultDetailTableHTML(params)
                break
            case "MARKDOWN":
                output += this._genResultDetailTableMarkdown(params)
                break
            case "ASCII":
                output += this._genResultDetailTableASCII(params)
                break
            case "JSONL":
                output += this._genResultDetailTableJSONL(params)
                break
            default:
                output += this._genResultDetailTableASCII(params)
                break
        }
        return output
    }


    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------


    /**
     * Private method.  Format a string containing tabular results from the JobLauncher
     * result summary in HTML format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::getLastResultSummary()
     *
     * @return String containing the formatted table of results
     */
    @NonCPS
    def _genResultSummaryTableHTML(summary)
    {
        String output = """
                        <table class="bgGreen tc2">
                            <tr><th>Summary Stat</th><th>Count</th></tr>
                            <tr><td>Num Tests</td><td>${summary.NUMJOBS}</td></tr>
                        """.stripIndent()

        this._allowable_job_status_core.each()
        {
            Integer tmpCount  = summary["NUM" + it.key]
            if( it.value=="R" || (it.value=="O" && tmpCount > 0) )
            {
                output += "    <tr class='${it.key}'><td>Num ${it.key}</td><td>${tmpCount}</td></tr>\n"
            }
        }
        this._allowable_job_status_spifi.each()
        {
            Integer tmpCount  = summary["NUM" + it.key]
            if( it.value=="R" || (it.value=="O" && tmpCount > 0) )
            {
                output += "    <tr class='${it.key}'><td>Num ${it.key}</td><td>${tmpCount}</td></tr>\n"
            }
        }
        output += "</table>\n"

        // TODO: Revisit this logic -- what were we doing here again?
        //       I think it was to make sure we're removing any remaining {{}} blocks
        //       so they're all handled, but it might not really be necessary.
        if(summary.NUMSUCCESS != summary.NUMJOBS)
        {
            output = output.replace("{{CLASSNUMSUCCESS}}", "class='FAILURE'")
        }
        if(summary.NUMFAILURE > 0)
        {
            output = output.replace("{{CLASSFAILURE}}", "class='FAILURE'")
        }
        if(summary.NUMUNSTABLE > 0)
        {
            output = output.replace("{{CLASSUNSTABLE}}", "class='UNSTABLE'")
        }
        output = output.replace("{{CLASSNUMSUCCESS}}", "")
        output = output.replace("{{CLASSFAILURE}}", "")
        output = output.replace("{{CLASSUNSTABLE}}", "")

        return output
    }


    /**
     * Private method.  Format a string containing tabular results from the JobLauncher
     * result summary in ASCII format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::getLastResultSummary()
     *
     * @return String containing the formatted table of results
     */
    @NonCPS
    def _genResultSummaryTableASCII(summary)
    {
        String output = """
                           Summary Stat   |   Count
                        ------------------+-----------
                           Num Tests      |   ${summary.NUMJOBS}
                        """.stripIndent()

        this._allowable_job_status_core.each()
        {
            Integer tmpCount  = summary["NUM" + it.key]
            if( it.value=="R" || (it.value=="O" && tmpCount > 0) )
            {
                output += sprintf("   Num %-9s  |   %d\n", [it.key, tmpCount])
            }
        }
        this._allowable_job_status_spifi.each()
        {
            Integer tmpCount  = summary["NUM" + it.key]
            if ( it.value == "R" || ( it.value=="O" && tmpCount > 0) )
            {
                output += sprintf("   Num %-9s  |   %d\n", [it.key, tmpCount])
            }
        }

        return output
    }


    /**
     * Private method.  Format a string containing tabular results from the JobLauncher
     * result summary in Markdown format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::getLastResultSummary()
     *
     * @return String containing the formatted table of results
     */
    @NonCPS
    def _genResultSummaryTableMarkdown(summary)
    {
        String output = """
                        | Summary Stat    | Count  |
                        | --------------- |:------:|
                        """.stripIndent()
        output += sprintf("|   Num Tests     | %5s  |\n", [summary.NUMJOBS])

        this._allowable_job_status_core.each()
        {
            Integer tmpCount  = summary["NUM" + it.key]
            if( it.value=="R" || (it.value=="O" && tmpCount > 0) )
            {
                output += sprintf("|   Num %-9s | %5d  |\n", [it.key, tmpCount])
            }
        }
        this._allowable_job_status_spifi.each()
        {
            Integer tmpCount  = summary["NUM" + it.key]
            if ( it.value == "R" || ( it.value == "O" && tmpCount > 0 ) )
            {
                output += sprintf("|   Num %-9s | %5d  |\n", [it.key, tmpCount])
            }
        }

        return output
    }


    /**
     * Private method.  Format a string containing tabular results from the JobLauncher
     * detailed results in HTML format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::launchInParallel()
     *
     * @return String containing the formatted table of results
     */
    @NonCPS
    def _genResultDetailTableHTML(params)
    {
        String output = """
                        <table class="bgGreen tc1 tc2">
                            <tr>
                                <th>Status</th>
                                <th>Duration (s)</th>
                                <th>Job Name</th>
                            </tr>
                        """.stripIndent()
        params.results.each
        {   r ->

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")
            assert r.value.containsKey("dry_run")

            String link = r.value.url
            if(link != "" && params.link_to_console)
            {
                link += "/console"
            }

            String msg = ""
            msg += "    <tr class='${r.value.status}'>\n"
            msg += "        <td>${r.value.status}</td>\n"
            msg += "        <td>${r.value.duration}</td>\n"

            // If dry run, note that in "link"
            if(true == r.value.dry_run)
            {
                msg += sprintf("    <td>%s #dry-run</td>\n", [r.value.job])
            }
            // If the link is empty, don't send it.
            else if("" == link)
            {
                msg += sprintf("    <td>%s</td>\n", [r.value.job])
            }
            // If the job id is set:
            else if(r.value.id != "0")
            {
                msg += sprintf("    <td><A HREF='%s'>%s #%s</A></td>\n", [link, r.value.job, r.value.id])
            }
            // Otherwise, just give the job name...
            else
            {
                msg += sprintf("    <td>%s</td>\n", [r.value.job])
            }

            msg += "</tr>"

            // Replace the {{CLASS}} entry
            // -- Should this be put into the msg definition earlier rather than using the {{}} replacement?
            //msg = msg.replace("{{CLASS}}", "class='${r.value.status}'")

            output += msg
        }
        output += "</table>\n"
        return output
    }


    /**
     * Private method.  Format a string containing tabular results from the JobLauncher
     * detailed results in ASCII format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::launchInParallel()
     *
     * @return String containing the formatted table of results
     */
    @NonCPS
    def _genResultDetailTableASCII(params)
    {
        String output = """
                           Status      |   Duration   |   Job Name
                        ---------------+--------------|-----------------------------------------------------------------------
                        """.stripIndent()
        params.results.each
        {   r ->

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")
            assert r.value.containsKey("dry_run")

            String job_name = "${r.value.job}"
            if(r.value.dry_run == true)
            {
                job_name += " #dry-run"
            }
            else if(r.value.id != "0")
            {
                job_name += " #${r.value.id}"
            }

            Float duration = r.value.duration

            output += sprintf("   %-9s   |   %10.2f |   %-70s\n", [r.value.status, duration, job_name])
        }
        return output
    }


    /**
     * Private method.  Format a string containing results from the JobLauncher
     * detailed results in JSONL format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::launchInParallel()
     *
     * @return String containing the formatted results
     */
    @NonCPS
    def _genResultDetailTableJSONL(params)
    {
        def timestamp = new Date()
        String output = "{"
        output += "\"datestamp\": \"" +
          timestamp.format("yyyy-MM-dd") + "\", "
        output += "\"timestamp\": \"" +
          timestamp.format("HH:mm:ss") + "\", "
        output += "\"jobs\": ["
        params.results.each
        {   r ->
            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")
            assert r.value.containsKey("dry_run")

            Float duration = r.value.duration
            output += "{"
            output += "\"name\": \"${r.value.job}\", "
            output += sprintf("\"id\": \"%s\", ", r.value.id)
            output += "\"status\": \"${r.value.status}\", "
            output += sprintf("\"duration\": %.2f,", duration)
            output += "\"dry_run\": ${r.value.dry_run},"
            output += "\"url\": \"${r.value.url}\""
            output += "}"

            //this._env.println "SPiFI-DEBUG]> params.results isa: " + params.results.getClass().getName()
            //this._env.println "SPiFI-DEBUG]> r isa: " + r.getClass().getName()
            //this._env.println "SPiFI-DEBUG]> params.results : " + params.results.values().last() + "\n" +
            //                  "SPiFI-DEBUG]> r.value        : " + r.value

            if (r.value != params.results.values().last())
            {
                output += ", "
            }
        }
        output += "]}"

        // Optionally, beautify the json so it's more human readable
        // Note: This would make the output a JSON string, not JSONL
        if(params.beautify)
        {
            output = groovy.json.JsonOutput.prettyPrint(output)
        }
        return output
    }


    /**
     * Private method.  Format a string containing tabular results from the JobLauncher
     * detailed results in Markdown format.
     *
     * @see gov.sandia.sems.spifi.JobLauncher::launchInParallel()
     *
     * @return String containing the formatted table of results
     */
    @NonCPS
    def _genResultDetailTableMarkdown(params)
    {
        // this._env.println "[SPiFI-DEBUG]> Entering ResultsUtility::_genResultDetailTableMarkdown(params)"
        String output = """
                        | Status     | Duration (s) | Job Name |
                        |:----------:|:------------:| -------- |
                        """.stripIndent()
        params.results.each
        {   r ->

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")
            assert r.value.containsKey("dry_run")

            String link = r.value.url
            if(link != "" && params.link_to_console)
            {
                link += "/console"
            }

            Float duration = r.value.duration

            output += sprintf("| %10s | %12.2f |", [r.value.status, duration])
            // If dry run, note that in link
            if(r.value.dry_run == true)
            {
                output += sprintf(" %s #dry-run |", r.value.job)
            }
            // Don't provide empty links
            else if(link == "")
            {
                output += sprintf(" %s |", r.value.job)
            }
            // Otherwise, provide the link
            else
            {
                output += sprintf(" [%s #%s](%s) |", [r.value.job, r.value.id, link])
            }
            output += "\n"


        }
        return output
    }



} // class ResultsUtility

