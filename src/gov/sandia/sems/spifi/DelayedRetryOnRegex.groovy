#!/usr/bin/env groovy
/**
 * RetryDelayOnRegex.groovy
 *
 * RetryDelayOnRegex class used by SPiFI
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2019-02-21
 */
package gov.sandia.sems.spifi

// Import Java modules
import java.util.regex.Matcher as Matcher

// Import JenkinsCI modules
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper as jenkinsRunWrapper

// Import SPiFI modules
import gov.sandia.sems.spifi.DelayedRetry as DelayedRetry
import gov.sandia.sems.spifi.interfaces.Printable as Printable
import gov.sandia.sems.spifi.interfaces.ScanBuildLog as ScanBuildLog


/**
 * Specialization of DelayedRetry to include a REGEX condition.
 */
class DelayedRetryOnRegex extends DelayedRetry implements ScanBuildLog, Printable
{
    private static _env
    public String retry_regex = ""

    /**
     *  Constructor
     */
    DelayedRetryOnRegex(Map params)
    {
        // Call the c'tor of the superclass
        super(params)

        // Handle required parameters
        if(!params.containsKey("env"))
        {
            throw new Exception("[SPiFI]> Missing required parameter: 'env'")
        }
        this._env = params.env

        // Handle optional parameters
        if(params.containsKey("retry_regex"))
        {
            assert params.retry_regex instanceof String
            this.retry_regex = params.retry_regex
        }
    }


    /**
     *
     */
    Boolean scanBuildLog( Map params )
    {
        // Check Parameters
        if(!params.containsKey("build_log"))
            throw new Exception("[SPiFI]> INPUT ERROR: Missing required parameter 'build_log'")
        else if(!params.build_log instanceof jenkinsRunWrapper )
            throw new Exception("[SPiFI]> TYPE ERROR: Expected 'build_log' to be a: 'org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper'\n"+
                                "[SPiFI]>             but got a:                       '${params.build_log.getClass().getName()}'")
        def build_log = params.build_log

        // Local vars
        Boolean output = false

        // Test the job status results
        this._env.println "[EXPERIMENTAL]> DelayedRetryOnRegex::testForRetryCondition() ]------------------------------------------"
        this._env.println "[EXPERIMENTAL]>      PATTERN = /${this.retry_regex}/"

        String __s = ""
        build_log.each
        { line ->
            Boolean m = (line =~ /${this.retry_regex}/).getCount() > 0

            __s += "[EXPERIMENTAL]>  "
            if(m) __s += "+"
            else  __s += "-"
            __s += "  ${line}\n"
        }
        this._env.println __s


        this._env.println "[EXPERIMENTAL]> DelayedRetryOnRegex::testForRetryCondition() ]------------------------------------------"

        return output
    }


    // Helpers/Utility
    String asString()
    {
        return "${this.getDelayedRetryAsString()}, retry_regex: \"${this.retry_regex}\""
    }

}   // class RetryDelayOnRegex


