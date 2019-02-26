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

import gov.sandia.sems.spifi.DelayedRetry
import gov.sandia.sems.spifi.interfaces.JobRetry
import gov.sandia.sems.spifi.interfaces.Printable


/**
 * Specialization of DelayedRetry to include a REGEX condition.
 */
class DelayedRetryOnRegex extends DelayedRetry implements JobRetry, Printable
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
    Boolean testForRetryCondition( job_status )
    {
        // Validate paramter type
        assert job_status instanceof org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

        // Local vars
        Boolean output = false

        // Test the job status results
        this._env.println "[EXPERIMENTAL]> DelayedRetryOnRegex::testForRetryCondition() +++"

        return output
    }


    // Helpers/Utility
    String stringify() { return "${super.stringify()}, retry_regex: \"${this.retry_regex}\"" }

}   // class RetryDelayOnRegex


