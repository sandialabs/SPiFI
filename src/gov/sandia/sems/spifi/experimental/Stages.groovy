#!/usr/bin/groovy
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
 * Stages.groovy
 *
 * A set of free-functions that provide a stage-level abstraction of
 * `stage()` capabilities through the use of visitor/callback routines
 * to customize the actions taken by a stage.
 */
package gov.sandia.sems.spifi.experimental



/**
 * spifi_stage
 *
 * Provides a generic `stage` function that uses callbacks (`Closures`) to customize
 * a standard set of event points in a stage.
 *
 * The abstraction of a stage is the following:
 *
 *      (1) execute optional pre-stage options
 *      STAGE():
 *          IF stage-is-skipped:
 *              (2) execute skip-stage code (notifications, logging, etc.)
 *          ELSE:
 *              IF simulate is true:
 *                  (3) execute simulation code (notification, simulating results, etc.)
 *              ELSE:
 *                  (4) execute optional pre-stage-body code
 *                  (5) execute stage body
 *                  (6) execute optional post-stage-body code
 *              ENDIF
 *          ENDIF
 *      END STAGE
 *      (7) execute optional post-stage options
 *
 * This general abstraction of a 'stage' is based on experiences with pipelines
 * which have many stages that may wish to check some state before or after a
 * stage is executed. These control points are handled in (1) and (7).
 * Within a stage itself, we might optonally _skip_ a stage based on what has
 * occurred earlier in the pipeline. This is handled in (2).
 *
 * We may also wish to "simulate" the stage, which can either be a no-op or
 * some action such as creating sample output files that later stages might
 * expect to exist.
 * For example if the stage we're simulating a stage that compiles code that can
 * take a long time. If we're debugging the pipeline we might not wish to wait
 * for an hour for the compile to complete but we have later stages that expect
 * the executable file to exist. In a 'simulation' we could put in a callback that
 * might create a bash script that prints out "hello world" rather than actually
 * compile the code if all we need is an "executable" for our purposes.
 *
 * If we aren't simulating the pipeline, there might also be pre- and post- activities
 * around the actual stage body itself that might largely be boiler-plate which we can
 * put in (4) and (6) that would execute before and after the actual stage-body (5).
 *
 * Generally, we expect that most pipeline stages _will not_ need to inject code
 * into all of these points but this function is designed to break apart a stage
 * into its general components and provide a general purpose interface that moves
 * pipelines towards a consistent structure.
 *
 * The _minimal_ use case is `spifi_stage(name: "stage name") { ... }` which is
 * nearly identical to existing `stage("stage name") { ... }` commands provided by
 * Pipelines.
 *
 * @param stageName                  String  [REQUIRED] Name of the stage
 * @param skip                       Boolean [OPTIONAL] Skip stage if false. Default: true
 *
 * @param callbackStagePre           Closure [OPTIONAL] Execute prior to stage()
 * @param callbackStagePreArgs       Map     [OPTIONAL] Arguments for the pre-stage callback.
 *
 * @param callbackStageSkipped       Closure [OPTIONAL] Executed if a stage is skipped due to stageCondition failure.
 * @param callbackStageSkippedArgs   Map     [OPTIONAL] Map containing arguments to callbackStageSkipped(args).
 *                                                      Reserved key names: "stage_name"
 *
 * @param callbackStageBodyPre      Closure [OPTIONAL] Executes inside `stage() { ... }` prior to stage body execution.
 * @param callbackStageBodyPreArgs  Map     [OPTIONAL] Map containing arguments specific to callbackStageBodyPre(args).
 *                                                     Reserved key names: "stage_name", "cb_result"
 *
 * @param callbackStageBodyPost     Closure [OPTIONAL] Executes inside `stage() { ... }` after stage body execution completes.
 * @param callbackStageBodyPostArgs Map     [OPTIONAL] Map containing arguments to callbackStageBodyPost(args).
 *                                                     Reserved key names: "stage_name", "cb_result"
 *
 * @param callbackStagePost          Closure [OPTIONAL] Execute after stage()
 * @param callbackStagePostArgs      Map     [OPTIONAL] Arguments for the post-stage callback.
 *
 * @param simulate                   Boolean [OPTIONAL] Toggle simulation mode. Default: false
 * @param simulateOutput             Object  [OPTIONAL] What should be RETURNED by the stage body closure if simulate is true?
 *                                                      Default: null
 * @param callbackStageSimulate      Closure [OPTIONAL] Executed if stage is in simulate mode.
 *                                                      Stage returns the value returned by the callback (null by default)
 * @param callbackStageSimulateArgs  Map     [OPTIONAL] Map containing arguments to callbackStageSimulate(args).
 *
 * @param callbackSharedArgs         Map     [OPTIONAL] Map containing SHARED arguments across ALL callbacks. Default: [:]
 * @param verbose                    Boolean [OPTIONAL] Enable extra verbosity and logging. Default: false
 * @param logDebug                   Map     [OPTIONAL] If provided, this will store information about what actions
 *                                                      conditionalStage performed. Default: null
 *
 * @return value returned by the stage body closure executed in the stage if not skipped.
 */
def spifi_stage(Map args, Closure stageBody)
{
    // Validate parameters
    Boolean verbose = args.containsKey("verbose") && args.verbose ? true : false
    Boolean simulate = args.containsKey("simulate")  && args.simulate  ? true : false
    Boolean skip_stage = args.containsKey("skip") && args.skip ? true : false

    Map logger = args.containsKey("logDebug") && args.logDebug instanceof Map ? args.logDebug : [:]

    // stageName is a REQUIRED paramter
    assert args.containsKey("stageName")
    String stageName = args.stageName

    // Set up the logger values
    // - These get toggled in the proper sections if we execute them
    //   as a execution trace.
    logger["verbose"]                        = verbose ?: false
    logger["preStageCallbackExecuted"]       = false
    logger["stageSkippedCallbackExecuted"]   = false
    logger["stageSkippedGenericMessage"]     = false
    logger["simulate"]                       = false
    logger["skip"]                           = false
    logger["simulateCallbackExecuted"]       = false
    logger["simulateGenericMessage"]         = false
    logger["stageBodyExecuted"]              = false
    logger["stageBodyPreCallbackExecuted"]   = false
    logger["stageBodyPreGenericMessage"]     = false
    logger["stageBodyPostCallbackExecuted"]  = false
    logger["stageBodyPostGenericMessage"]    = false
    logger["postStageCallbackExecuted"]      = false

    // Shared argumengs for all callbacks
    Map args_shared  = args.containsKey("callbackSharedArgs") ? args.callbackSharedArgs : [:]

    println "\u276ESPiFI\u276F BEGIN conditionalStage(${stageName})"

    // Create output variable
    def output = null

    // Pre-Stage callback (if provided)
    if( args.containsKey("callbackStagePre") )
    {
        def args_cb = args.containsKey("callbackStagePreArgs") ? args.callbackStagePreArgs : [:]
        spifi_execute_optional_callback(stageName, args.callbackStagePre, args_cb, args_shared)
        logger["preStageCallbackExecuted"] = true
    }

    stage(stageName)
    {
        if(verbose)
        {
            println "\u276ESPiFI\u276F Additional Verbose Logging"
        }

        // Stage is skipped
        if(skip_stage)
        {
            logger["skip"] = true
            if( args.containsKey("callbackStageSkipped") )
            {
                def args_cb = args.containsKey("callbackStageSkippedArgs") ? args.callbackStageSkippedArgs : [:]
                spifi_execute_optional_callback(stageName, args.callbackStageSkipped, args_cb, args_shared)
                logger["stageSkippedCallbackExecuted"] = true
            }
            else
            {
                println "\u276ESPiFI\u276F Stage ${stageName} was skipped due to condition failure"
                logger["stageSkippedGenericMessage"] = true
            }
        }

        // Stage is NOT skipped
        else
        {

            // Execute stageBodyPre callback if one was provided
            if( args.containsKey("callbackStageBodyPre") )
            {
                def args_cb = args.containsKey("callbackStageBodyPreArgs") ? args.callbackStageBodyPreArgs : [:]
                spifi_execute_optional_callback(stageName, args.callbackStageBodyPre, args_cb, args_shared)
                logger["stageBodyPreCallbackExecuted"] = true
            }
            else
            {
                println "\u276ESPiFI\u276F Stage ${stageName} body pre operation"
                logger["stageBodyPreGenericMessage"] = true
            }

            // If we ARE doing a simulated run:
            if(simulate)
            {
                logger["simulate"] = true
                if( args.containsKey("callbackStageSimulate") )
                {
                    def args_cb = args.containsKey("callbackStageSimulateArgs") ? args.callbackStageSimulateArgs : [:]
                    spifi_execute_optional_callback(stageName, args.callbackStageSimulate, args_cb, args_shared)
                    output = args_cb.cb_result
                    logger["simulateCallbackExecuted"] = true
                }
                else
                {
                    println "\u276ESPiFI\u276F Stage ${stageName} is in simulate mode"
                    logger["simulateGenericMessage"] = true
                }
                output = args.containsKey("simulateOutput") ? args.simulateOutput : output
            }

            // If we ARE NOT doing a simulation:
            else
            {
                // Execute stage body
                logger["stageBodyExecuted"] = true
                output = stageBody()

            } // end (not simulated)

            // Execute stageBodyPost callback if one was provided
            if( args.containsKey("callbackStageBodyPost") )
            {
                def args_cb = args.containsKey("callbackStageBodyPostArgs") ? args.callbackStageBodyPostArgs : [:]
                spifi_execute_optional_callback(stageName, args.callbackStageBodyPost, args_cb, args_shared)
                logger["stageBodyPostCallbackExecuted"] = true
            }
            else
            {
                println "\u276ESPiFI\u276F Stage ${stageName} body post operation"
                logger["stageBodyPostGenericMessage"] = true
            }

        } // else stage NOT skipped

    } // stage

    // Post-Stage callback (if provided)
    if( args.containsKey("callbackStagePost") )
    {
        def args_cb = args.containsKey("callbackStagePostArgs") ? args.callbackStagePostArgs : [:]
        spifi_execute_optional_callback(stageName, args.callbackStagePost, args_cb, args_shared)
        logger["postStageCallbackExecuted"] = true
    }

    println "\u276ESPiFI\u276F END conditionalStage(${stageName})"
    return output
}



/**
 * Helper function to execute a callback and set up the 'reserved'
 * arguments.
 */
def spifi_execute_optional_callback(String stage_name,
                                    Closure callback,
                                    Map callback_args=[:],
                                    Map shared_args=[:])
{
    assert callback instanceof Closure : "callback must be a Closure object."
    if(callback_args.containsKey("stage_name")) callback_args.remove("stage_name")
    if(callback_args.containsKey("cb_result"))  callback_args.remove("cb_result")
    assert !callback_args.containsKey("stage_name") : "stage_name is a reserved key."
    assert !callback_args.containsKey("cb_result")  : "cb_result is a reserved key."
    callback_args["stage_name"] = stage_name
    callback_args["cb_result"]  = callback(callback_args, shared_args)
}



return this
