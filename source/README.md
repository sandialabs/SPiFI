Sources
-------

Modules and code snippets are going here (for now).  If we can determine how to let Jenkins import sources then this might turn into more of a library that can get included at the top-level of Jenkins pipeline scripts :D

## Files
- `script_env.groovy`
  - Support class to set up a reference to the static job environment.
- `job_properties.groovy`
- `emailMessageBuilder.groovy`
  - Implements: class emailMessageBuilder
  - Requires: `script_env.groovy`
- `parallelTestLauncher.groovy`
   - Implements: class parallelTestLauncher
   - Requires: `script_env.groovy"
