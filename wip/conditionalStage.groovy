#!/usr/bin/groovy


/**
 * conditionalStage
 *
 * @param stageName                  String  [REQUIRED] Name of the stage
 * @param stageCondition             Boolean [OPTIONAL] Skip stage if false. Default: true
 * @param callbackStagePre           Closure [OPTIONAL] Execute prior to stage()
 * @param callbackStagePreArgs       Map     [OPTIONAL]
 * @param callbackStageSkipped       Closure [OPTIONAL] Executed if a stage is skipped due to stageCondition failure.
 * @param callbackStageSkippedArgs   Map     [OPTIONAL] Map containing arguments to callbackStageSkipped(args).
 *                                                      Reserved key names: "stage_name"
 * @param callbackStageCompleted     Closure [OPTIONAL] Executed if the stage is executed to completion.
 * @param callbackStageCompletedArgs Map     [OPTIONAL] Map containing arguments to callbackStageCompleted(args).
 *                                                      Reserved key names: "stage_name", "stage_result"
 * @param callbackStagePost          Closure [OPTIONAL] Execute after stage()
 * @param callbackStagePostArgs      Map     [OPTIONAL]
 * @param dryRun                     Boolean [OPTIONAL] Toggle dry-run mode. Default: false
 * @param dryRunOutput               Object  [OPTIONAL] What should be RETURNED by the stage body closure if dryRun is true?
 *                                                      Default: null
 * @param callbackStageDryRun        Closure [OPTIONAL] Executed if stage is in dry-run mode.
 *                                                      Stage returns the value returned by the callback (null by default)
 * @param callbackStageDryRunArgs    Map     [OPTIONAL] Map containing arguments to callbackStageDryRun(args).
 * @param callbackSharedArgs         Map     [OPTIONAL] Map containing SHARED arguments across ALL callbacks. Default: [:]
 * @param verbose                    Boolean [OPTIONAL] Enable extra verbosity and logging. Default: false
 * @param logDebug                   Map     [OPTIONAL] If provided, this will store information about what actions
 *                                                      conditionalStage performed. Default: null
 *
 * @return value returned by the stage body closure executed in the stage if not skipped.
 */
def conditionalStage(Map args, Closure stageBody)
{
    // Validate parameters
    Boolean verbose = args.containsKey("verbose") && args.verbose ? true : false
    Boolean dry_run = args.containsKey("dryRun")  && args.dryRun  ? true : false
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
    logger["dryRun"]                         = false
    logger["dryRunCallbackExecuted"]         = false
    logger["dryRunGenericMessage"]           = false
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
            // If we ARE doing a dry-run:
            if( dry_run )
            {
                logger["dryRun"] = true
                if( args.containsKey("callbackDryRun") )
                {
                    assert args.callbackStageDryRun instanceof Closure

                    def args_cb_dry = args.containsKey("callbackStageDryRunArgs") ? args.callbackStageDryRunArgs : [:]
                    output = args.callbackStageDryRun(args_cb_dry, args_shared)
                    logger["dryRunCallbackExecuted"] = true
                }
                else
                {
                    println "\u276ESPiFI\u276F Stage ${stageName} is in dry-run mode"
                    logger["dryRunGenericMessage"] = true
                }
                output = args.containsKey("dryRunOutput") ? args.dryRunOutput : output
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
