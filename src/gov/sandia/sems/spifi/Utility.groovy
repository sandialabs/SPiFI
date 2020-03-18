#!/usr/bin/env groovy
/*
 * SEMS Pipeline Framework Infrastructure (SPiFI)
 *
 * Copyright 2020 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
 * Under the terms of Contract DE-NA0003525 with NTESS, the U.S. Government retains
 * certain rights in this software.
 *
 * LICENSE (3-Clause BSD)
 * ----------------------
 * Copyright 2020 National Technology & Engineering Solutions of Sandia, LLC (NTESS).
 * Under the terms of Contract DE-NA0003525 with NTESS, the U.S. Government retains
 * certain rights in this software.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact
 * -------
 * William C. McLendon III (wcmclen@sandia.gov)
 */
/**
 * Utility.groovy
 *
 * Utility functions used internally within SPiFI.  
 * Ordinarily these will not be exposed to users of the library.
 *
 * @author  William McLendon
 * @since   1.0
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


