////////////////////////////////////////////////////////////////////////////////
//
//  script_env.jenkinsfile
//  ----------------------
//
//  Implements a simple class that sets up a static reference to the Jenkins
//  environment in a jenkinsfile.  This enables the use of sandboxed methods
//  outside of the node{} blocks in a Jenkins pipeline script.
//
//  This should be put at the top of the pipeline script in your groovy file.
//
//  Prerequisites:
//  - None
//
//  See Also:
//  - https://stackoverflow.com/questions/39817225/commands-fail-when-moving-to-them-custom-class-in-jenkinsfile
//
////////////////////////////////////////////////////////////////////////////////


// Set up a static reference in the script...
Script.env = this

public class Script
{
    public static env
}

