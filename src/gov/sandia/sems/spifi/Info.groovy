#!/usr/bin/env groovy
/**
 * Info.groovy
 *
 * Information function to get current version number, etc.
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-06-06
 *
 */
package gov.sandia.sems.spifi;



/**
 * Return the current version of SPiFI
 *
 * @return String containing the version number
 */
static def version()
{
    String spifi_version = "__master__"
    return spifi_version
}


