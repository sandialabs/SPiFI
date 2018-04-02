////////////////////////////////////////////////////////////////////////////////
//
//  EmailMessage.groovy
//  ---------------------------
//
//  An email builder helper class that comes with some pre-configured 'canned'
//  emails as well as a customizable email setting.  This class provides a
//  HTML style pre-configured, allowing the point-of-use code to only need to
//  generate the appropriate email body.
//
//  See Also:
//  -
//
////////////////////////////////////////////////////////////////////////////////
package gov.sandia.sems.spifi;


// ----[ class EmailMessage ]-------------------------------------------
//
// Helper class to generate formatted emails by consolidating all the STYLE and
// HEADER information for HTML style emails.  This can be expanded later to include
// canned emails for the different status messages that we will generate in this
// pipeline job.
//
// Canned email types are set by the emailType parameter.  Currently there are
// the following types:
// - SUCCESS : Canned email for SUCCESS status.
// - FAILURE : Canned email for FAILURE status.
// - CUSTOM  : Customizable email address.
//
class EmailMessage implements Serializable
{
    // Member Variables
    private static _env           // Requird Jenkins environment.
    private String _emailBody     // (optional), if emailType is Custom, then use this.
    private String _recipients    // comma-separated list of email address "person1@foo.com, person2@foo.com"
    private String _replyTo       // Who should responses go to?
    private String _emailType     // Notification Type - { "SUCCESS", "FAILURE", "CUSTOM" etc. }
    private String _emailSubject  // Email Title


    // ----[ Constructor ]-----------------------------------------------------
    //
    // param emailType  [String] - Email Class.  Canned values are { SUCCESS, FAILURE, CUSTOM }
    // param recipients [String] - Recipient list.
    //                             Comma-separated list of email recipients: "one@foo.com, two@foo.com"
    // param replyTo    [String] - Reply-To email address. Example: "bar@foo.com"
    //
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
            default:
                this._emailSubject = "Status"
                this._emailType = "CUSTOM"
                break
        }
    }


    // ----[ setCustomEmailBody ]-----------------------------------------------
    //
    //  Set up a custom email.
    //
    //  param subject [String] : The subject of the email.
    //  param body    [String] : The email body.  This can be text or HTML
    //                           If HTML then this is the content that goes
    //                           inside the <BODY></BODY> section.
    //
    def setCustomEmail(String subject, String body)
    {
        this._emailSubject = subject
        this._emailBody = body
    }


    // ----[ sendEmail ]--------------------------------------------------------
    //
    //  Send the email.
    //
    def send()
    {
        String emailBody = this._getTemplate().replace("{{EMAIL_BODY}}", this._emailBody)

        this._env.emailext(body: "${emailBody}",        \
                           compressLog: true,           \
                           replyTo: this._replyTo,      \
                           subject: this._emailSubject, \
                           to: this._recipients,        \
                           mimeType: 'text/html')
    }


    // ----[ genResultSummaryTable ]--------------------------------------------------
    //
    //  Generate a pretty printable summary table for emails, etc.
    //
    //  Allowable Parameters:
    //
    //  summary  [Map]    - REQUIRED Output Summary information from a call to
    //                               ParallelJobLauncher.getLastResultSummary()
    //
    //  format   [String] - OPTIONAL Type of output table to generate. Default: ASCII
    //                               Must be one of: [ ASCII | HTML | MARKDOWN ]
    //
    def genResultSummaryTable(Map params)
    {
        // If format isn't provided, make it ASCII
        if(!params.containsKey("format"))
        {
            params["format"] = "ASCII"
        }

        // Check required param: summary
        assert params.containsKey("summary")

        assert params.summary.containsKey("NUMTESTS")
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


    // ----[ genResultDetailTable ]--------------------------------------------------
    //
    //  Generate a pretty printable list of results by job from the results output
    //  of ParallelJobLauncher.launchInParallel()
    //
    //  Allowable Parameters:
    //
    //  results  [Map]    - REQUIRED Results from a call to
    //                               ParallelJobLauncher.launchInParallel()
    //
    //  format   [String] - OPTIONAL Type of output table to generate.
    //                               Must be one of: [ ASCII | HTML | MARKDOWN ]
    //
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


    // ----[ _genResultSummaryTableHTML ]---------------------------------------
    def _genResultSummaryTableHTML(summary)
    {
        String output = """
                        <table class="bgGreen tc2">
                            <tr><th>Summary Stat</th><th>Count</th></tr>
                            <tr><td>Num Tests</td><td>${summary.NUMTESTS}</td></tr>
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

        if(summary.NUMSUCCESS != summary.NUMTESTS)
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


    // ----[ _genResultSummaryTableASCII ]--------------------------------------
    def _genResultSummaryTableASCII(summary)
    {
        String output = """
                           Summary Stat   |   Count
                        ------------------+-----------
                           Num Tests      |   ${summary.NUMTESTS}
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


    // ----[ _genResultSummaryTableMarkdown ]-----------------------------------
    def _genResultSummaryTableMarkdown(summary)
    {
        String output = """
                        | Summary Stat    | Count  |
                        | --------------- |:------:|
                        |   Num Tests     | ${summary.NUMTESTS}  |
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


    // ----[ _genResultDetailTableHTML ]----------------------------------------
    def _genResultDetailTableHTML(results)
    {
        String output = """
                        <table class="bgGreen tc2">
                            <tr><th>Job Name</th><th>Result</th></tr>
                        """.stripIndent()
        results.each
        {   _r ->
            def r = _r

            assert r.value.containsKey("job")
            assert r.value.containsKey("id")
            assert r.value.containsKey("status")
            assert r.value.containsKey("url")

            String msg = """
                         <tr {{CLASS}}>
                             <td><A HREF="${r.value.url}/console">${r.value.job} #${r.value.id}</A></td>
                             <td>${r.value.status}</td>
                         </tr>
                         """.stripIndent()

            switch(r.value.status)
            {
                case "ABORTED":
                case "NOT_BUILT":
                case "FAILURE":
                    msg = msg.replace("{{CLASS}}", "class='FAILURE'")
                    break
                case "UNSTABLE":
                    msg = msg.replace("{{CLASS}}", "class='UNSTABLE'")
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


    // ----[ _genResultDetailTableASCII ]---------------------------------------
    def _genResultDetailTableASCII(results)
    {
        String output = """
                           Job Name                                                               |   Result
                        --------------------------------------------------------------------------+--------------
                        """.stripIndent()
        results.each
        {   r ->
            assert r.value.containsKey("job")
            assert r.value.containsKey("status")
            output += sprintf("   %-70s |   %s\n", [r.value.job, r.value.status])
        }
        return output
    }


    // ----[ _genResultDetailTableMarkdown ]------------------------------------
    def _genResultDetailTableMarkdown(results)
    {
        String output = """
                        | Job Name | Result |
                        | -------- |:------:|
                        """.stripIndent()
        results.each
        {   r ->
            assert r.value.containsKey("job")
            assert r.value.containsKey("status")
            output += sprintf("| %s | %s |\n", [r.value.job, r.value.status])
        }
        return output
    }


    // ----[ _genStyleSheet ]---------------------------------------------------
    /**
     *  _genStyleSheet
     *
     *  Generate the stylesheet part of a header (everything in <STYLE></STYLE>)
     */
     def _genStyleSheet()
     {
         String output = """
                <STYLE>
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

                /* Defined <span/> blocks to allow in-line coloring of text */
                span.gray {
                    color: gray;
                }
                span.red {
                    color: red;
                }
                span.green {
                    color: green;
                }
                span.yellow {
                    color: yellow;
                }
                span.blue {
                    color: blue;
                }

                /* Code Block */
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

                /* Table Themes */

                /* Light Background Theme */
                .bgLight th {
                    background-color: #AFAFAF;
                }
                .bgLight tr:nth-child(even) {
                    background-color: #D9D9D8;
                }
                .bgLight tr:nth-child(odd) {
                    background-color: #FFEFEE;
                }

                /* Dark Background Theme */
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

                /* Green Background Theme */
                .bgGreen th {
                    background-color: #AFAFAF;
                }
                .bgGreen tr:nth-child(even) {
                    background-color: #B1E894;
                }
                .bgGreen tr:nth-child(odd) {
                    background-color: #8CD564;
                }

                /* Status based themes */
                tr.SUCCESS td {
                    background-color: #80BD73;
                }
                tr.FAILURE td {
                    background-color: #E38E89;
                }
                tr.UNSTABLE td {
                    background-color: #E3D389;
                }

                /* Center columns [1-9] */
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


    // ----[ _getTemplate ]-----------------------------------------------------
    /**
     *  _getTemplate
     *
     * Private method.  Generates a template email HTML header with style
     * rules,etc.  The body can be customized with the setCustomEmail() method
     * or specific "canned" emails can be generated.
     */
    def _getTemplate()
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

