#!/usr/bin/env groovy
/**
 * Git.groovy
 *
 * Git plugin function wrapper for SPiFI
 * Mainly the use of this is as a convenience function for pipelines since it's
 * fairly common that github, gitlab, etc. might fail to clone if the network
 * is experiencing a lot of lag, etc. so some retries are handy to have.
 *
 * Example Usage:
 *
 *    def git_spifi = new gov.sandia.sems.spifi.Git()
 *    git_spifi.clone(env: this, url: "git@github.com:trilinos/Trilinos.git")
 *
 * @author  William McLendon
 * @version 1.0
 * @since   2018-04-04
 *
 */
package gov.sandia.sems.spifi;



/**
 * Clone a git repository with retries, timeout, and directory location provided.
 *
 * @param env                [REQUIRED] Object  The jenkins environment in the jenkins file.
 *                                              This should be the `this` object.
 * @param url                [REQUIRED] String  The URL to the git repository.
 *                                              i.e., git@github.com:trilinos/Trilinos.git
 * @param branch             [OPTIONAL] String  The branch name to check out.
 *                                              Default: master
 * @param credentialsId      [OPTIONAL] String  A Jenkins credentialsID if required.
 *                                              Default: null
 * @param dir                [OPTIONAL] String  The directory to clone into, relative to
 *                                              dir at the current scope.
 *                                              Default: ""
 * @param retries            [OPTIONAL] Integer Number of retries to attempt.
 *                                              Default: 0
 * @param retry_delay        [OPTIONAL] Integer Number of seconds to wait before retrying.
 *                                              Default: 60
 * @param timeout            [OPTIONAL] Integer How long is the timeout to be per attempt?
 *                                              Default: 30
 * @param timeout_units      [OPTIONAL] String  Units to use for the timeout.
 *                                              Allowed values are: {HOURS|MINUTES|SECONDS}.
 *                                              Default: MINUTES
 *                                              DEPRECATED IN version 2.0.0 due to changeover 
 *                                              to GitSCM in 1.1.4
 * @param verbose            [OPTIONAL] Boolean Print out verbose information to the console.
 *                                              Default: false
 * @param recurse_submodules [OPTIONAL] Boolean Enable recursive submodule checkout.
 *                                              Default: false
 * @param shallow            [OPTIONAL] Boolean Enable a shallow clone (limits depth by default to 50) 
 *                                              to reduce the size of the checkout.
 *                                              Default: false
 *
 * @return true if clone was successful, false if it failed.
 *
 */
def clone(Map params)
{
    // List of known parameters to this function
    List parameter_list = ["env", 
                           "url",           "branch", 
                           "credentialsId", "dir", 
                           "retries",       "retry_delay",
                           "timeout",       "timeout_units", 
                           "verbose",       "recurse_submodules",
                           "shallow"] 

    // Set up default values
    String  dir                = "."
    String  url                = ""
    String  branch             = "master"
    String  credentialsId      = null
    Boolean recurse_submodules = false
    Integer retries            = 0
    Integer retry_delay        = 60
    Integer timeout            = 30
    Boolean shallow            = false
    Integer shallow_depth      = 50

    // Process required parameters.
    if(!params.containsKey("env"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: env")
    }
    def env = params.env

    if(!params.containsKey("url"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: url")
    }
    assert params.url instanceof String
    url = params.url

    // Update optional parameters
    if(params.containsKey("branch"))               branch             = params.branch
    if(params.containsKey("credentialsId"))        credentialsId      = params.credentialsId
    if(params.containsKey("dir"))                  dir                = params.dir
    if(params.containsKey("recurse_submodules"))   recurse_submodules = params.recurse_submodules
    if(params.containsKey("retries"))              retries            = params.retries
    if(params.containsKey("retry_delay"))          retry_delay        = params.retry_delay
    if(params.containsKey("timeout"))              timeout            = params.timeout
    if(params.containsKey("shallow"))              shallow            = params.shallow

    // Print a message if there are any unknown parameters.
    String unknown_parameters = ""
    params.keySet().each
    { key ->
      if( !parameter_list.contains(key) )
      {
          unknown_parameters += "${key}"
      }
    }
    if( "" != unknown_parameters )
    {
        env.println "[SPiFI]> Git.clone() received unknown parameters:\n" +
                    "[SPiFI]> - [ ${unknown_parameters.trim()} ] "
    }

    // Optionally print out some debugging output
    if(params.containsKey("verbose") && params.verbose == true)
    {
        env.println "[SPiFI]> Git.clone()\n" +
                    "[SPiFI]> -  url               : ${url}\n" +
                    "[SPiFI]> -  branch            : ${branch}\n" +
                    "[SPiFI]> -  dir               : ${dir}\n" +
                    "[SPiFI]> -  credentialsId     : ${credentialsId}\n" +
                    "[SPiFI]> -  recurse_submodules: ${recurse_submodules}\n" +
                    "[SPiFI]> -  retries           : ${retries}\n" +
                    "[SPiFI]> -  retry_delay       : ${retry_delay}\n" +
                    "[SPiFI]> -  timeout           : ${timeout}\n" +
                    "[SPiFI]> -  shallow           : ${shallow}\n" +
                    "[SPiFI]> Environment:\n" +
                    "[SPiFI]> -  workspace: ${env.WORKSPACE}\n" +
                    "[SPiFI]> Raw Params:\n${params}" 
    }

    // Initialize output variables
    Boolean output = true
    Integer attempts = 1

    try
    {
        env.retry(retries)
        {
            try
            {
                // Offical Docs to `checkout`: https://jenkins.io/doc/pipeline/steps/workflow-scm-step 
                checkout([$class: 'GitSCM', 
                          branches: [[name: branch]], 
                          doGenerateSubmoduleConfigurations: false, 
                          extensions: [ [$class: 'RelativeTargetDirectory', 
                                                 relativeTargetDir: dir
                                        ],
                                        [$class: 'CheckoutOption', 
                                                 timeout: timeout
                                        ], 
                                        [$class: 'CloneOption', 
                                                 depth: shallow_depth, 
                                                 noTags: false, 
                                                 reference: '', 
                                                 shallow: shallow, 
                                                 timeout: timeout
                                        ],
                                        [$class: 'SubmoduleOption', 
                                                 depth: shallow_depth, 
                                                 disableSubmodules: !recurse_submodules,
                                                 parentCredentials: true,
                                                 recursiveSubmodules: recurse_submodules /**/,
                                                 reference: '',
                                                 shallow: shallow,
                                                 threads: 2,
                                                 timeout: timeout,
                                                 trackingSubmodules: false
                                        ]
                                      ], 
                          submoduleCfg: [], 
                          userRemoteConfigs: [ [credentialsId: credentialsId, url: url] ]
                        ])
            }
            catch(ex)
            {
                env.println "[SPiFI]> git failed to clone: ${url}\n" +
                            "[SPiFI]> The error message is:\n${ex}\n" +
                            "[SPiFI]> retrying in ${retry_delay} seconds."
                attempts++
                sleep retry_delay
                throw ex
            }
        }
    }
    catch(ex)
    {
        env.println "[SPiFI]> git failed to clone: ${url}\n" +
                    "[SPiFI]> -  Attempts made: ${attempts}"
        output = false
    }
    return output
}


return this
