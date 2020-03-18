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
 * @param timeout            [OPTIONAL] Integer How long is the timeout to be per attempt in SECONDS?
 *                                              Default: 30
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
    //
    // Begin parameter validation
    //

    // Check and set env first
    if(!params.containsKey("env"))
    {
        throw new Exception("[SPiFI] ERROR: Missing required parameter: env")
    }
    def env = params.env

    env.println "[SPiFI]> Git.clone()"
    env.println "[SPiFI]> Git.clone(): parameter check begin"
    Map params_expected = [ "env":                 [ option: "R" ],
                            "url":                 [ option: "R" ],
                            "branch":              [ option: "O" ],
                            "credentialsId":       [ option: "O" ],
                            "dir":                 [ option: "O" ],
                            "retries":             [ option: "O" ],
                            "retry_delay":         [ option: "O" ],
                            "timeout":             [ option: "O" ],
                            "verbose":             [ option: "O" ],
                            "recurse_submodules":  [ option: "O" ],
                            "shallow":             [ option: "O" ],
                          ]
    Boolean params_ok = gov.sandia.sems.spifi.impl.Tools.spifi_parameter_check(env: env,
                                                                               params_expected: params_expected,
                                                                               params_received: params,
                                                                               verbose: params.containsKey("verbose") && params.verbose
                                                                               )
    if( !params_ok )
    {
        throw new Exception("SPiFI ERROR: parameter check failed for Git.clone()")
    }

    // Additional type check on params.url
    assert params.url instanceof String

    env.println "[SPiFI]> Git.clone(): parameter check complete"

    //
    // Completed parameter validation
    //

    // Set up default values
    String  dir                = "."
    String  url                = params.url
    String  branch             = "master"
    String  credentialsId      = null
    Boolean recurse_submodules = false
    Integer retries            = 0
    Integer retry_delay        = 60
    Integer timeout            = 30
    Boolean shallow            = false
    Integer shallow_depth      = 50

    // Set optional parameters
    if(params.containsKey("branch"))               branch             = params.branch
    if(params.containsKey("credentialsId"))        credentialsId      = params.credentialsId
    if(params.containsKey("dir"))                  dir                = params.dir
    if(params.containsKey("recurse_submodules"))   recurse_submodules = params.recurse_submodules
    if(params.containsKey("retries"))              retries            = params.retries
    if(params.containsKey("retry_delay"))          retry_delay        = params.retry_delay
    if(params.containsKey("timeout"))              timeout            = params.timeout
    if(params.containsKey("shallow"))              shallow            = params.shallow

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
