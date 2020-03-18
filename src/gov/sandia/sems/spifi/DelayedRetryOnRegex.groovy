#!/usr/bin/env groovy
/*
 *  SEMS Pipeline Framework Infrastructure (SPiFI)
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
     * Scan the build log for a match of the 'retry' condition.
     *
     * Parameters:
     * build_log [REQUIRED] - List<String> containing the console log as a list of lines.
     *
     */
    Boolean scanBuildLog( Map params )
    {
        // Check Parameters
        if( !params.containsKey("build_log") )
            throw new Exception("[SPiFI]> INPUT ERROR: Missing required parameter 'build_log'")
        if( !params.build_log instanceof List<String> )
            throw new Exception("[SPiFI]> TYPE ERROR: Expected 'build_log' to be a List<String>.")

        List<String> build_log = params.build_log

        Boolean output = build_log.find
        { line ->
            // Check the line for matches to the regex
            if( (line =~ /${this.retry_regex}/).getCount() > 0 )
            {
                // Break the 'find' search if found
                return true
            }
            // Otherwise, continue
            return false
        }
        // output will be null if not true, so force it to true
        output = output == true

        return output
    }


    // Helpers/Utility
    String asString()
    {
        return "${this.getDelayedRetryAsString()}, retry_regex: \"${this.retry_regex}\""
    }

}   // class RetryDelayOnRegex


