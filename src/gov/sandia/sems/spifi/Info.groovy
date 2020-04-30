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
 * Info.groovy
 *
 * Information function to get current version number, etc.
 *
 * @author  William McLendon
 * @version 1.2.0
 * @since   2018-06-06
 *
 */
package gov.sandia.sems.spifi;



/**
 * Return the current version of SPiFI
 * - In reality this should be the current branch name, but release branches
 *   are formatted as `v[major].[minor].[patch]` and use Semantic Versioning.
 *
 * @return String containing the version number
 */
static def version()
{
    // Note: for releases, this should be formatted X.X.X
    //       leave the 'v' out.  i.e., "2.1.0"
    String spifi_version = "master"
    return spifi_version
}


/**
 * Return the current version 'major' number for SPiFI
 * Added in version 1.2.0
 *
 * @return Integer containing the version 'major' number.
 */
static def version_major()
{
    Integer spifi_major = 2
    return  spifi_major
}


/**
 * Return the current version 'minor' number for SPiFI
 * Added in version 1.2.0
 *
 * @return Integer containing the version 'minor' number.
 */
static def version_minor()
{
    Integer spifi_minor = 1
    return  spifi_minor
}


/**
 * Return the current version 'patch' number for SPiFI
 * Added in version 1.2.0
 *
 * @return Integer containing the version 'patch' number.
 */
static def version_patch()
{
    Integer spifi_patch = 0
    return  spifi_patch
}

