#!/usr/bin/env groovy

// Goal: get the hostname only out of the JOB_URL environment variable in Jenkins


// the JOB_URL might look like this:
String job_url = "https://jenkins-srn-dev.sandia.gov:8443/job/SPiFI_Sandbox/"
println "${job_url}"


// Method 1: can split this out directly in a string
println "${job_url.tokenize('/')[1].split('\\.')[0]}"

// Method 2: modify the string and save it
job_url = job_url.tokenize('/')[1].split("\\.")[0]
println "${job_url}"

