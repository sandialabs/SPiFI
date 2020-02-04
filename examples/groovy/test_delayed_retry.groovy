#!/usr/bin/env groovy

class DelayedRetry
{
    Integer retry_delay = 90
    String  retry_delay_units = "SECONDS"

    DelayedRetry(Map params)
    {
        if(params != null)
        {
            // Set values based on parameters
            if(params.containsKey("retry_delay")) { this.retry_delay = params.retry_delay }
            if(params.containsKey("retry_delay_units")) { this.retry_delay_units = params.retry_delay_units; }
        }
    }

    // Getter(s)
    Integer getRetryDelay()      { return this.retry_delay }
    String  getRetryDelayUnits() { return this.retry_delay_units }

    // toString() will be called if an object of this class is passed to println
    String toString() { return "retry_delay: ${getRetryDelay()}, retry_delay_units: ${getRetryDelayUnits()}" }
}


class DelayedRetryOnRegex extends DelayedRetry
{
    String regex = ""

    DelayedRetryOnRegex(Map params)
    {
        // Call the c'tor of the superclass
        super(params)

        // Set values based on parameters and strip them off the parameter list
        if(params != null)
        {
            if(params.containsKey("regex")) { this.regex = params.regex }
        }
    }

    // Getter(s)
    String getRegex() { return this.regex }

    // toString() will be called if an object of this class is passed to println
    String toString() { return "${super.toString()}, regex: '${getRegex()}'" }
}


class RetryParameters
{
    List    retry_list = []      // List of retry parameters
    Integer retry_max = 0                  // Maximum # of retries
    Integer retry_max_allowable = 99       // Maximum # of retries allowable

    RetryParameters(Map params)
    {
    }

    // Setter(s)
    def setRetryMax(Integer value)
    {
        if(value < 0)
        {
            this.retry_max = 0
        }
        else if(value > this.retry_max_allowable)
        {
            this.retry_max = this.retry_max_allowable
        }
        else
        {
            this.retry_max = value
        }
    }


    def appendRetryParameter(DelayedRetry value)
    {
        this.retry_list += value
    }
    def appendRetryParameter(List values)
    {
        this.retry_list += values
    }


    String toString()
    {
        String s = ""
        s += "[\n"
        this.retry_list.each
        { iparam ->
            s += "    " + iparam + "\n"
        }
        s += "]\n"
        return s
    }
}



//def r = new DelayedRetryOnRegex("retry_count": 3, "retry_delay": 120, "retry_delay_units": "MINUTES", "regex": "ssh_error")

List r = []
r += new DelayedRetryOnRegex("retry_delay": 120, "retry_delay_units": "MINUTES", "regex": "ssh_error")
r += new DelayedRetryOnRegex("retry_delay": 120, "retry_delay_units": "MINUTES", "regex": "proxy error")

//
// Alternative: just use a list of maps...
//

// println r
println "["
r.each {
    ri ->
    println "    " + ri
}
println "]"
println "----------------------"

List x = [
            [ "retry_delay": 90, "retry_delay_units": "MINUTES", "retry_regex": "proxy error" ],
            [ "retry_delay": 90, "retry_delay_units": "MINUTES", "retry_regex": "ssh hangup" ],
         ]

println "["
x.each {
    xi ->
    println "    " + xi
}
println "]"

println "----------------------"

def RP = new RetryParameters()
RP.setRetryMax(10)
RP.appendRetryParameter( [ new DelayedRetryOnRegex("retry_delay": 30),
                           new DelayedRetryOnRegex("retry_delay": 60) ] )
println RP


