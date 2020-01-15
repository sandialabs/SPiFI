#!/usr/bin/env groovy
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
    String spifi_version = "1.3.0"
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
    Integer spifi_major = 1
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
    Integer spifi_minor = 3
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

