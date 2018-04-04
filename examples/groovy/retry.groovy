#!/usr/bin/env groovy

// Example of a generic exception in Groovy

Integer retries = 4

Integer attempts = retries + 1

while(attempts > 0)
{
  println("Attempt: ${attempts}")


  if(attempts==2) 
  {
      break
  }


  attempts--
}




