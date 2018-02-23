////////////////////////////////////////////////////////////////////////////////
//
//  emailMessageBuilder.groovy
//  ---------------------------
//
//  An email builder helper class that comes with some pre-configured 'canned'
//  emails as well as a customizable email setting.  This class provides a
//  HTML style pre-configured, allowing the point-of-use code to only need to
//  generate the appropriate email body.
//
//  Prerequisites:
//  - script_env.groovy
//
//  See Also:
//  - 
//
////////////////////////////////////////////////////////////////////////////////



// ----[ class emailMessageBuilder ]-------------------------------------------
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
class emailMessageBuilder
{
    // Member Variables
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
    emailMessageBuilder(String emailType, String recipients, String replyTo)
    {
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

        Script.env.emailext(body: "${emailBody}", \
                                    compressLog: true,         \
                                    replyTo: this._replyTo,    \
                                    subject: this._emailSubject, \
                                    to: this._recipients,      \
                                    mimeType: 'text/html')
    }


    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------


    // ----[ _getTemplate ]-----------------------------------------------------
    //
    // Private method.  Generates a template email HTML header with style
    // rules,etc.  The body can be customized with the setCustomEmail() method
    // or specific "canned" emails can be generated.
    //
    def _getTemplate()
    {
        String email_template = """
            <!DOCTYPE html>
            <HTML>
            <HEAD>
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
                    background-color: #AFAFAF;
                    font-weight: bold;
                    font-size: 14px;
                    text-align: center;
                }
                tr:nth-child(even) {
                    background-color: #E9E9E8;
                }
                tr:nth-child(odd) {
                    background-color: #FFEFE;
                }
                tr.SUCCESS td {
                    background-color: #80BD73;
                }
                tr.FAILURE td {
                    background-color: #E38E89;
                }
                tr.UNSTABLE td {
                    background-color: #E3D389;
                }
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
            </HEAD>
            <BODY>
            {{EMAIL_BODY}}
            </BODY>
            </HTML>
            """.stripIndent()
        return email_template
    }

} // class emailMessageBuilder


