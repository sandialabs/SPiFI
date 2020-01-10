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


    /**
     * Constructor for ResultsUtility
     *
     * @param env  [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
     *
     * @return nothing
     */
    ResultsUtility(Map params)
    {
        // Validate parameters
        if(!params.containsKey("env"))
        {
            throw new Exception("[SPiFI] Missing required parameter: 'env'")
        }
        this._env = params.env
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
     *
     * @return String containing the Summary table for results that can be inserted into
     *                the console log, email, etc.
     */
    def genResultSummaryTable(Map params)
    {
        // If format isn't provided, make it ASCII
        if(!params.containsKey("format"))
        {
            params["format"] = "ASCII"
        }

        // Check required param: summary
        assert params.containsKey("summary")

        assert params.summary.containsKey("NUMJOBS")
        assert params.summary.containsKey("NUMSUCCESS")
        assert params.summary.containsKey("NUMFAILURE")
        assert params.summary.containsKey("NUMUNSTABLE")
        assert params.summary.containsKey("NUMABORTED")
        assert params.summary.containsKey("NUMNOT_BUILT")

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
     *
     * @return String containing the Detail table for results that can be inserted into
     *                the console log, email, etc.
     *
     */
    def genResultDetails(Map params)
    {
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
    def _genResultSummaryTableHTML(summary)
    {
        String output = """
                        <table class="bgGreen tc2">
                            <tr><th>Summary Stat</th><th>Count</th></tr>
                            <tr><td>Num Tests</td><td>${summary.NUMJOBS}</td></tr>
                            <tr {{CLASSNUMSUCCESS}}><td>Num Passed</td><td>${summary.NUMSUCCESS}</td></tr>
                            <tr {{CLASSFAIL}}><td>Num Failed</td><td>${summary.NUMFAILURE}</td></tr>
                            <tr {{CLASSUNSTABLE}}><td>Num Unstable</td><td>${summary.NUMUNSTABLE}</td></tr>
                        """.stripIndent()

        // If there were aborted jobs, add the row
        if(summary.NUMABORTED > 0)
        {
            output += "<tr class='FAILURE'><td>Num Aborted</td><td>${summary.NUMABORTED}</td></tr>\n"
        }
        // If there were NOT_BUILT jobs, add the row
        if(summary.NUMNOT_BUILT > 0)
        {
            output += "<tr class='FAILURE'><td>Num NOT_Built</td><td>${summary.NUMNOT_BUILT}</td></tr>\n"
        }

        output += "</table>\n"

        if(summary.NUMSUCCESS != summary.NUMJOBS)
        {
            output = output.replace("{{CLASSNUMSUCCESS}}", "class='FAILURE'")
        }
        if(summary.NUMFAILURE > 0)
        {
            output = output.replace("{{CLASSFAIL}}", "class='FAILURE'")
        }
        if(summary.NUMUNSTABLE > 0)
        {
            output = output.replace("{{CLASSUNSTABLE}}", "class='UNSTABLE'")
        }
        output = output.replace("{{CLASSNUMSUCCESS}}", "")
        output = output.replace("{{CLASSFAIL}}", "")
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
    def _genResultSummaryTableASCII(summary)
    {
        String output = """
                           Summary Stat   |   Count
                        ------------------+-----------
                           Num Tests      |   ${summary.NUMJOBS}
                           Num Passed     |   ${summary.NUMSUCCESS}
                           Num Failure    |   ${summary.NUMFAILURE}
                           Num Unstable   |   ${summary.NUMUNSTABLE}
                        """.stripIndent()

        if(summary.NUMABORTED > 0)
        {
            output += "   Num Aborted    |   ${summary.NUMABORTED}\n"
        }
        if(summary.NUMNOT_BUILT > 0)
        {
            output += "   Num NOT_Built  |   ${summary.NUMNOT_BUILT}\n"
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
    def _genResultSummaryTableMarkdown(summary)
    {
        String output = """
                        | Summary Stat    | Count  |
                        | --------------- |:------:|
                        |   Num Tests     | ${summary.NUMJOBS}  |
                        |   Num Passed    | ${summary.NUMSUCCESS}  |
                        |   Num Failure   | ${summary.NUMFAILURE}  |
                        |   Num Unstable  | ${summary.NUMUNSTABLE}  |
                        """.stripIndent()
        if(summary.NUMABORTED > 0)
        {
            output += "|   Num Aborted   | ${summary.NUMABORTED}  |\n"
        }
        if(summary.NUMNOT_BUILT > 0)
        {
            output += "|   Num NOT_Built | ${summary.NUMNOT_BUILT}  |\n"
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
    def _genResultDetailTableHTML(params)
    {
        String output = """
                        <table class="bgGreen tc1 tc2">
                            <tr><th>Status</th><th>Duration (s)</th><th>Job Name</th></tr>
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

            String msg = """
                         <tr {{CLASS}}>
                             <td>${r.value.status}</td>
                             <td>${r.value.duration}</td>
                         """.stripIndent()

            // If dry run, note that in "link"
            if(true == r.value.dry_run)
            {
                msg += sprintf("    <td>%s #dry-run</td>\n", [r.value.job])
            } 
            // Don't provide empty links
            else if("" == link)
            {
                msg += sprintf("    <td>%s</td>\n", [r.value.job])
            }
            // Otherwise, provide the link
            else
            {
                msg += sprintf("    <td><A HREF='%s'>%s #%s</A></td>\n", [link, r.value.job, r.value.id])
            }

            msg += "</tr>"

            switch(r.value.status)
            {
                case "FAILURE":
                    msg = msg.replace("{{CLASS}}", "class='FAILURE'")
                    break
                case "UNSTABLE":
                    msg = msg.replace("{{CLASS}}", "class='UNSTABLE'")
                    break
                case "ABORTED":
                    msg = msg.replace("{{CLASS}}", "class='ABORTED'")
                    break
                case "NOT_BUILT":
                    msg = msg.replace("{{CLASS}}", "class='NOT_BUILT'")
                    break
                default:
                    msg = msg.replace("{{CLASS}}", "")
                    break
            }
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

            String job_name = "${r.value.job} #"
            if(r.value.dry_run == true)
            {
                job_name += "dry-run"
            }
            else
            {
                job_name += "${r.value.id}"
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
            output += "\"dry_run\": ${r.value.dry_run},"            // TODO: Can JsonL handle a boolean?
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

