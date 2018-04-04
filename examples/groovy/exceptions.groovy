#!/usr/bin/env groovy

// Example of a generic exception in Groovy
try
{
  throw new Exception("Hello, I am a generic exception... ")
} 
catch (e) 
{
  println "Caught: ${e}"
}
finally
{
  println "Finally!"
}

