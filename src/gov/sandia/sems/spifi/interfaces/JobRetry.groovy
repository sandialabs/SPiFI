#!/usr/bin/env groovy
/**
 * JobRetryInterface.groovy
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2019-02-27
 */
package gov.sandia.sems.spifi.interfaces



interface JobRetry
{

    Boolean testForRetryCondition( job_status )

}   // interface JobRetry

