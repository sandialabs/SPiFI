#!/usr/bin/env groovy
/**
 * HTMLUtility.groovy
 *
 * A set of routines to help with formatting HTML messages.  Provides a set of
 * style rules and a basic template.
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-19
 *
 */
package gov.sandia.sems.spifi;



/**
 * Helper class to generate formatted emails by consolidating all the STYLE and
 * HEADER information for HTML style emails.  This can be expanded later to include
 * canned emails for the different status messages that we will generate in this
 * pipeline job.
 *
 */
class HTMLUtility implements Serializable
{
    // Member Variables
    private static _env           // Requird Jenkins environment.


    /**
     * Constructor
     *
     * @param env  [REQUIRED] Object  - Jenkins environment (use 'this' from the Jenkins pipeline)
     *
     * @return nothing
     */
    HTMLUtility(Map params)
    {
        // Validate parameters
        if(!params.containsKey("env"))
        {
            throw new Exception("[SPiFI] Missing required parameter: 'env'")
        }
        this._env = params.env
    }


    /**
     * generate
     *
     * @param body [REQUIRED] String  - String containing the body of the HTML to generate.
     *                                  This is the content that would be inside the <BODY></BODY> elements
     *                                  in a HTML document.
     *
     * @return String containing the assembled HTML document with template and headers.
     */
    def generate(Map params)
    {
        if(!params.containsKey("body"))
        {
            throw new Exception("[SPiFI] Missing required parameter: 'body'.")
        }
        String output = this._genTemplate()
        output = output.replace("{{BODY}}", params.body)
        return output
    }



    // -------------------------------------------------------------------------
    // ----[ PRIVATE METHODS ]--------------------------------------------------
    // -------------------------------------------------------------------------



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
            {{BODY}}
            <P><span class="gray">--<BR/>View output on <A HREF="${this._env.BUILD_URL}">Jenkins</A>.</span></P>
            </BODY>
            </HTML>
            """.stripIndent()
        template = template.replace("{{STYLESHEET}}", this._genStyleSheet())
        return template
    }

} // class HTMLUtility

