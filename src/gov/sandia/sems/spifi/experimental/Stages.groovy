#!/usr/bin/groovy
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
 * @param stageCondition             Boolean [OPTIONAL] Skip stage if false. Default: true
 * @param callbackStagePre           Closure [OPTIONAL] Execute prior to stage()
 * @param callbackStagePreArgs       Map     [OPTIONAL] Arguments for the pre-stage callback.
 * @param callbackStageSkipped       Closure [OPTIONAL] Executed if a stage is skipped due to stageCondition failure.
 * @param callbackStageSkippedArgs   Map     [OPTIONAL] Map containing arguments to callbackStageSkipped(args).
 *                                                      Reserved key names: "stage_name"
 * @param callbackStageCompleted     Closure [OPTIONAL] Executed if the stage is executed to completion.
 * @param callbackStageCompletedArgs Map     [OPTIONAL] Map containing arguments to callbackStageCompleted(args).
 *                                                      Reserved key names: "stage_name", "stage_result"
 * @param callbackStagePost          Closure [OPTIONAL] Execute after stage()
 * @param callbackStagePostArgs      Map     [OPTIONAL] Arguments for the post-stage callback.
 * @param simulate                   Boolean [OPTIONAL] Toggle simulation mode. Default: false
 * @param simulateOutput             Object  [OPTIONAL] What should be RETURNED by the stage body closure if simulate is true?
 *                                                      Default: null
 * @param callbackStageSimulate      Closure [OPTIONAL] Executed if stage is in simulate mode.
 *                                                      Stage returns the value returned by the callback (null by default)
 * @param callbackStageSimulateArgs  Map     [OPTIONAL] Map containing arguments to callbackStageSimulate(args).
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
    Boolean skip_stage = args.containsKey("stageCondition") && args.stageCondition ? true : false

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
    logger["simulateCallbackExecuted"]       = false
    logger["simulateGenericMessage"]         = false
    logger["stageBodyExecuted"]              = false
    logger["stageCompletedCallbackExecuted"] = false
    logger["stageCompletedGenericMessage"]   = false
    logger["postStageCallbackExecuted"]      = false

    // Shared argumengs for all callbacks
    Map args_shared  = args.containsKey("callbackSharedArgs") ? args.callbackSharedArgs : [:]

    println "\u276ESPiFI\u276F BEGIN conditionalStage(${stageName})"

    // Create output variable
    def output = null

    if( args.containsKey("callbackStagePre") )
    {
        assert args.callbackStagePre instanceof Closure

        def args_cb_pre = args.containsKey("callbackStagePreArgs") ? args.callbackStagePreArgs : [:]
        args.callbackStagePre( args_cb_pre, args_shared )
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
            logger["stageSkipped"] = true
            if( args.containsKey("callbackStageSkipped") )
            {
                assert( args.callbackStageSkipped instanceof Closure )

                def args_cb_skip = args.containsKey("callbackStageSkippedArgs") ? args.callbackStageSkippedArgs : [:]
                args.callbackStageSkipped( args_cb_skip, args_shared )
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
            // If we ARE doing a simulated run:
            if( simulate )
            {
                logger["simulate"] = true
                if( args.containsKey("callbackSimulate") )
                {
                    assert args.callbackStageSimulate instanceof Closure

                    def args_cb_sim = args.containsKey("callbackStageSimulateArgs") ? args.callbackStageSimulateArgs : [:]
                    output = args.callbackStageSimulate(args_cb_sim, args_shared)
                    logger["simulateCallbackExecuted"] = true
                }
                else
                {
                    println "\u276ESPiFI\u276F Stage ${stageName} is in simulate mode"
                    logger["simulateGenericMessage"] = true
                }
                output = args.containsKey("simulateOutput") ? args.simulateOutput : output
            }

            // If we ARE NOT doing a dry-run:
            else
            {
                // Execute stage body
                logger["stageBodyExecuted"] = true
                output = stageBody()

                // Execute stageCompleted callback if one was provided
                if( args.containsKey("callbackStageCompleted") )
                {
                    assert args.callbackStageCompleted instanceof Closure

                    def args_cb_completed = args.containsKey("callbackStageCompletedArgs") ? args.callbackStageCompletedArgs : [:]
                    args.callbackStageCompleted( args_cb_completed, args_shared )
                    logger["stageCompletedCallbackExecuted"] = true
                }
                else
                {
                    println "\u276ESPiFI\u276F Stage ${stageName} completed"
                    logger["stageCompletedGenericMessage"] = true
                }
            }
        }
    } // stage

    // Call the POST-STAGE callback if one was provided.
    if( args.containsKey("callbackStagePost") )
    {
        assert args.callbackStagePost instanceof Closure

        def args_cb_post = args.containsKey("callbackStagePostArgs") ? args.callbackStagePostArgs : [:]
        args.callbackStagePost( args_cb_post, args_shared )
        logger["postStageCallbackExecuted"] = true
    }

    println "\u276ESPiFI\u276F END conditionalStage(${stageName})"
    return output
}


return this
