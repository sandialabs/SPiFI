#!/usr/bin/env groovy
/**
 * EmailMessage.groovy
 *
 * An email builder helper class that comes with some pre-configured 'canned'
 * emails as well as a customizable email setting.  This class provides a
 * HTML style pre-configured, allowing the point-of-use code to only need to
 * generate the appropriate email body.
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-04
 *
 */
package gov.sandia.sems.spifi;



/**
 * Helper class to generate formatted emails by consolidating all the STYLE and
 * HEADER information for HTML style emails.  This can be expanded later to include
 * canned emails for the different status messages that we will generate in this
 * pipeline job.
 *
 * Canned email types are set by the emailType parameter.  Currently there are
 * the following types:
 * - SUCCESS : Canned email for SUCCESS status.
 * - FAILURE : Canned email for FAILURE status.
 * - CUSTOM  : Customizable email address.
 *
 */
class EmailMessage implements Serializable
{
    // Member Variables
    private static _env           // Requird Jenkins environment.
    private String _emailBody     // (optional), if emailType is Custom, then use this.
    private String _recipients    // comma-separated list of email address "person1@foo.com, person2@foo.com"
    private String _replyTo       // Who should responses go to?
    private String _emailType     // Notification Type - { "SUCCESS", "FAILURE", "CUSTOM" etc. }
    private String _emailSubject  // Email Title


    /**
     * Constructor for EmailMessage
     *
     * @param env        [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
     * @param emailType  [REQUIRED] String  - What type of email should be generated.
     *                                        Allowed values are [ SUCCESS | FAILURE | CUSTOM ]
     * @param recipients [REQUIRED] String  - Recipient list. Space-separated list of email
     *                                        recipients: "one@foo.com two@foo.com"
     * @param replyTo    [REQUIRED] String] - Reply-To email address. Example: "bar@foo.com"
     *
     * @return nothing
     */
    EmailMessage(env, String emailType, String recipients, String replyTo)
    {
        this._env          = env
        this._emailType    = emailType
        this._replyTo      = replyTo
        this._recipients   = recipients

        switch(emailType)
        {
            case "FAILURE":
                this._emailSubject = "[Jenkins] Test Failure!"
                this._emailBody = "<P>TEST FAILURE</P>"
                break
            case "SUCCESS":
                this._emailSubject = "[Jenkins] Test Success!"
                this._emailBody = "<P>TEST SUCCESS</P>"
                break
            case "CUSTOM":
                this._emailSubject = "Status"
                this._emailType = "CUSTOM"
                break
            default:
                throw new Exception("Invalid emailType parameter provided to SPiFI::EmailMessage constructor.")
                break
        }
    }


    /**
     * Set the subject and body of a custom email message.
     *
     * @param subject [REQUIRED] String - The subject of the email.
     * @param body    [REQUIRED] String - The body of the email.  This goes inside
     *                                    the <body></body> tags of the HTML message.
     *
     * @return nothing
     */
    def setCustomEmail(String subject, String body)
    {
        this._emailSubject = subject
        this._emailBody = body
    }


    /**
     * Sends the email.
     *
     * @return nothing
     */
    def send()
    {
        try
        {
            String emailBody = this._genTemplate().replace("{{EMAIL_BODY}}", this._emailBody)

            this._env.emailext(body: "${emailBody}",        \
                               compressLog: true,           \
                               replyTo: this._replyTo,      \
                               subject: this._emailSubject, \
                               to: this._recipients,        \
                               mimeType: 'text/html')
        }
        catch(e)
        {
            throw new Exception("[SPiFI] An Error occurred attempting to send an email!\n${e}")
        }
    }


    /**
     * Generate a pretty printable summary table for emails, etc.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::getLastResultSummary()
     *
     * @param summary  [REQUIRED] Map    - Output Summary information from a call to
     *                                     ParallelJobLauncher.getLastResultSummary()
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
     * Generate a pretty printable list of results by job from the results output
     * of ParallelJobLauncher.launchInParallel()
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::launchInParallel()
     *
     * @param results  [REQUIRED] Map    - Results from a call to
     *                                     ParallelJobLauncher.launchInParallel()
     * @param format   [OPTIONAL] String - Type of output table to generate.
     *                                     Must be one of: [ ASCII | HTML | MARKDOWN ]
     *                                     Default: ASCII
     *
     * @return String containing the Detail table for results that can be inserted into
     *                the console log, email, etc.
     */
    def genResultDetailTable(Map params)
    {
        // If format isn't provided, make it ASCII
        if(!params.containsKey("format"))
        {
            params["format"] = "ASCII"
        }

        // Check required param: results
        assert params.containsKey("results")

        String output = ""

        switch(params.format)
        {
            case "HTML":
                output += this._genResultDetailTableHTML(params.results)
                break
            case "MARKDOWN":
                output += this._genResultDetailTableMarkdown(params.results)
                break
            case "ASCII":
                output += this._genResultDetailTableASCII(params.results)
                break
            default:
                output += this._genResultDetailTableASCII(params.results)
                break
        }
        return output
    }


    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------


    /**
     * Private method.  Format a string containing tabular results from the ParallelJobLauncher
     * result summary in HTML format.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::getLastResultSummary()
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
     * Private method.  Format a string containing tabular results from the ParallelJobLauncher
     * result summary in ASCII format.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::getLastResultSummary()
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
     * Private method.  Format a string containing tabular results from the ParallelJobLauncher
     * result summary in Markdown format.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::getLastResultSummary()
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
     * Private method.  Format a string containing tabular results from the ParallelJobLauncher
     * detailed results in HTML format.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::launchInParallel()
     *
     * @return String containing the formatted table of results
     */
    def _genResultDetailTableHTML(results)
    {
        String output = """
                        <table class="bgGreen tc1 tc2">
                            <tr><th>Status</th><th>Duration (s)</th><th>Job Name</th></tr>
                        """.stripIndent()
        results.each
        {   r ->

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")

            String msg = """
                         <tr {{CLASS}}>
                             <td>${r.value.status}</td>
                             <td>${r.value.duration}</td>
                             <td><A HREF="${r.value.url}/console">${r.value.job} #${r.value.id}</A></td>
                         </tr>
                         """.stripIndent()

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
     * Private method.  Format a string containing tabular results from the ParallelJobLauncher
     * detailed results in ASCII format.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::launchInParallel()
     *
     * @return String containing the formatted table of results
     */
    def _genResultDetailTableASCII(results)
    {
        String output = """
                           Status      |   Duration   |    Job Name
                        ---------------+--------------|-----------------------------------------------------------------------
                        """.stripIndent()
        results.each
        {   r ->

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")

            String job_name = "${r.value.job} #${r.value.id}"

            output += sprintf("   %-9s   |   %-8.2f   |   %-70s\n", [r.value.status, r.value.duration, job_name])
        }
        return output
    }


    /**
     * Private method.  Format a string containing tabular results from the ParallelJobLauncher
     * detailed results in Markdown format.
     *
     * @see gov.sandia.sems.spifi.ParallelJobLauncher::launchInParallel()
     *
     * @return String containing the formatted table of results
     */
    def _genResultDetailTableMarkdown(results)
    {
        String output = """
                        | Status   | Duration (s) | Job Name |
                        |:--------:|:------------:| -------- |
                        """.stripIndent()
        results.each
        {   r ->

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("duration")
            assert r.value.containsKey("url")

            output += sprintf("| %s | %s | [%s #%s](%s/console) |\n", [r.value.status, r.value.duration, r.value.job, r.value.id, r.value.url])
        }
        return output
    }


    /**
     *  _genStyleSheet
     *
     *  Generate the stylesheet part of a header (everything in <STYLE></STYLE>)
     *
     *  @return String containing the CSS Stylesheet, including the <STYLE></STYLE> HTML tags.
     */
     def _genStyleSheet()
     {
         String output = """
                <STYLE>

                /* Defined <span/> blocks to allow in-line coloring/formatting of text */
                span.bold   { font-weight: bolder; }
                span.gray   { color: gray;    }
                span.red    { color: red;     }
                span.green  { color: #00bb00; }
                span.yellow { color: #cccc00; }
                span.blue   { color: blue;    }

                /* ---------- Code Block ---------- */

                /* - use with <pre class="code">...</pre> */
                pre.code {
                    background: #f4f4f4;
                    border: 1px solid #ddd;
                    border-left: 3px solid #f36d33;
                    color: #666;
                    page-break-inside: avoid;
                    font-family: "Courier New" monospace;
                    font-size: 90%;
                    line-height: 1.2em;
                    margin-bottom: 1.0em;
                    max-width: 100%;
                    overflow: auto;
                    padding: 1em 1.0em;
                    display: block;
                    word-wrap: break-word;
                    margin: auto 20px auto 20px;
                }

                /* ---------- Table Themes ---------- */
                table, th, td {
                border: 1px solid black;
                border-collapse: collapse;
                padding: 5px;
                font-family: monospace;
                font-size: 12px;
                }
                table {
                    border-spacing: 5px;
                    vertical-align: middle;
                }
                th {
                    font-weight: bold;
                    font-size:   14px;
                    text-align:  center;
                }

                /* ---------- Light Background Theme ---------- */
                .bgLight th {
                    background-color: #AFAFAF;
                }
                .bgLight tr:nth-child(even) {
                    background-color: #D9D9D8;
                }
                .bgLight tr:nth-child(odd) {
                    background-color: #FFEFEE;
                }

                /* ---------- Dark Background Theme ---------- */
                .bgDark th {
                    background-color: #0C1A49;
                    color: #737785;
                }
                .bgDark tr:nth-child(even) {
                    background-color: #545867;
                    color: #151A2B;
                }
                .bgDark tr:nth-child(odd) {
                    background-color: #1E212C;
                    color: #737785;
                }

                /* ----------  Green Background Theme ---------- */
                .bgGreen th {
                    background-color: #AFAFAF;
                }
                .bgGreen tr:nth-child(even) {
                    background-color: #B1E894;
                }
                .bgGreen tr:nth-child(odd) {
                    background-color: #8CD564;
                }

                /* ---------- Status based themes ---------- */
                tr.SUCCESS td {
                    background-color: #80BD73;
                }
                tr.FAILURE td {
                    background-color: #E38E89;
                }
                tr.UNSTABLE td {
                    background-color: #E3D389;
                }
                tr.ABORTED td {
                    background-color: #e6e6e6;
                }
                tr.NOT_BUILT td {
                    background-color: #e6e6e6;
                }

                /* ---------- Center columns [1-9] ---------- */
                .tc1 td:nth-child(1), .tc1 th:nth-child(1),
                .tc2 td:nth-child(2), .tc2 th:nth-child(2),
                .tc3 td:nth-child(3), .tc3 th:nth-child(3),
                .tc4 td:nth-child(4), .tc4 th:nth-child(4),
                .tc5 td:nth-child(5), .tc5 th:nth-child(5),
                .tc6 td:nth-child(6), .tc6 th:nth-child(6),
                .tc7 td:nth-child(7), .tc7 th:nth-child(7),
                .tc8 td:nth-child(8), .tc8 th:nth-child(8),
                .tc9 td:nth-child(9), .tc9 th:nth-child(9)  { text-align:center }
                .tr1 td:nth-child(1), .tr1 th:nth-child(1),
                .tr2 td:nth-child(2), .tr2 th:nth-child(2),
                .tr3 td:nth-child(3), .tr3 th:nth-child(3),
                .tr4 td:nth-child(4), .tr4 th:nth-child(4),
                .tr5 td:nth-child(5), .tr5 th:nth-child(5),
                .tr6 td:nth-child(6), .tr6 th:nth-child(6),
                .tr7 td:nth-child(7), .tr7 th:nth-child(7),
                .tr8 td:nth-child(8), .tr8 th:nth-child(8),
                .tr9 td:nth-child(9), .tr9 th:nth-child(9)  { text-align:right }
                </STYLE>
         """.stripIndent()
         return output
     }


    /**
     * Private method.  Generates a template email HTML header with style
     * rules,etc.  The body can be customized with the setCustomEmail() method
     * or specific "canned" emails can be generated.
     *
     * Includes tags "{{STYLESHEET}}" and "{{EMAIL_BODY}}" which are to be replaced
     * in other methods.
     *
     * @return Template HTML email message text.
     */
    def _genTemplate()
    {
        String template = """
            <!DOCTYPE html>
            <HTML>
            <HEAD>
            {{STYLESHEET}}
            </HEAD>
            <BODY>
            {{EMAIL_BODY}}
            <P><span class="gray">--<BR/>View output on <A HREF="${this._env.BUILD_URL}">Jenkins</A>.</span></P>
            </BODY>
            </HTML>
            """.stripIndent()
        template = template.replace("{{STYLESHEET}}", this._genStyleSheet())
        return template
    }

} // class EmailMessage

