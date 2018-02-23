
////////////////////////////////////////////////////////////////////////////////
//
//  properties.jenkinsfile
//  ----------------------
//
//  Example code for setting Jenkins job properties from within a scripted
//  pipeline.
//
//  Prerequisites:
//  - None
//
//  See Also:
//  - https://github.com/jenkinsci/pipeline-model-definition-plugin/wiki/Parametrized-pipelines
//
////////////////////////////////////////////////////////////////////////////////


// Configure Job Parameters
//
// - buildDiscarder: Handles Log Rotations, etc.
//
// - parameters: takes a list of parameters.  Examples:
//   - parameters([
//                  string(name: 'PARAM_TEST', defaultValue: 'default_value', description: 'Test Parameter'),
//                  booleanParam(name: 'myBooleanParameter', defaultValue: true, description: 'toggle')
//                ])
//
// - disableConcurrentBuilds: toggles concurrent build allowance.
//   - disableConcurrentBuilds()
//
// - pipelineTriggers
//   - Run the job on a CRON setting (midnight in Mountain Timezone):
//     - pipelineTriggers([ cron("""TZ=America/Denver\n0 0 * * *""")])
//
properties([
    buildDiscarder(
        logRotator(daysToKeepStr: '90', numToKeepStr: '30', artifactDaysToKeepStr: '', artifactNumToKeepStr: '')
    ),
    disableConcurrentBuilds(),
    parameters([ booleanParam(name: 'DryRun',
                              defaultValue: true,
                              description: '''dry-run mode: echo commands but don't run anything''')
               ])
])


