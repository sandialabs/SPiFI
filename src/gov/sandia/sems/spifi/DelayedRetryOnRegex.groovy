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



class DelayedRetryOnRegex extends gov.sandia.sems.spifi.DelayedRetry
{
    private static _env

    public String retry_regex = ""

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
        if(params.containsKey("retry_regex")) { this.retry_regex = params.retry_regex }
    }

    // Helpers/Utility
    String Stringify() { return "${super.Stringify()}, retry_regex: ${this.retry_regex}" }

}   // class RetryDelayOnRegex


