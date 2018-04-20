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

import gov.sandia.sems.spifi.HTMLUtility;




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
            def htmlUtil = new HTMLUtility(env: this._env)

            String emailBody = htmlUtil.generate(body: this._emailBody)

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



    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------


} // class EmailMessage

