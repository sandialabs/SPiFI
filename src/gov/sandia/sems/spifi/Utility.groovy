#!/usr/bin/env groovy
/**
 * Utility.groovy
 *
 * Utility functions used internally within SPiFI.  
 * 
 * Ordinarily these will not be exposed to users of the library.
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-04
 */
package gov.sandia.sems.spifi

import org.apache.commons.lang.RandomStringUtils


class Utility
{

    /**
     * Generate a random alphanumeric string
     *
     * @param length [REQUIRED] Integer - Length of string to generate.
     *
     * @return String of random alphanumeric characters (CAPS only). i.e., "AC8ZF9ORQ51G7"
     */
    def randomString(Integer length)
    {
        String charset = (('A'..'Z') + ('0'..'9')).join()
        String randomString = RandomStringUtils.random(length, charset.toCharArray())
        return randomString
    }



    /**
     * Normalize time from HOURS or MINUTES into SECONDS
     *
     * @param time  [REQUIRED] Float  - The amount of time units to convert.
     * @param units [REQUIRED] String - The unit of measurement of the time.  
     *                                  (HOURS, MINUTES, SECONDS, MILLISECONDS)
     *
     * @return Float value of time converted into seconds.
     */
    def convertDurationToSeconds(Float time, String units)
    {
        if( !("SECONDS"==units || "MINUTES"==units || "HOURS"==units || "MILLISECONDS"==units) )
        {
            throw new Exception("gov.sandia.sems.spifi.Utility.convertDurationToSeconds - Invalid value for units: ${units}")
        }

        Float time_seconds = time

        if("MINUTES"==units)
        {
            time_seconds = time * 60
        }
        else if("HOURS"==units)
        {
            time_seconds = time * 3600
        }
        else if("MILLISECONDS"==units)
        {
            time_seconds = time / 1000
        }
        return time_seconds
    }

}  // class Utility


