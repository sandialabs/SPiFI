#!/usr/bin/env groovy

class DelayedRetry
{
    Integer retry_count = 0
    Integer retry_delay = 90
    String  retry_delay_units = "SECONDS"

    DelayedRetry(Map params)
    {
        println "ctor: DelayedRetry()"

        // Set values based on parameters
        if(params.containsKey("retry_count")) { this.retry_count = params.retry_count }
        if(params.containsKey("retry_delay")) { this.retry_delay = params.retry_delay }
        if(params.containsKey("retry_delay_units")) { this.retry_delay_units = params.retry_delay_units }
    }

    Integer getRetryCount()      { return this.retry_count }
    Integer getRetryDelay()      { return this.retry_delay }
    String  getRetryDelayUnits() { return this.retry_delay_units }

    String toString() { return "retry_count: ${getRetryCount()}, retry_delay: ${getRetryDelay()}, retry_delay_units: ${getRetryDelayUnits()}" }
}


class DelayedRetryOnRegex extends DelayedRetry
{
    String regex = "";

    DelayedRetryOnRegex(Map params)
    {
        super(params)
        println "ctor: DelayedRetryOnRegex()"
        if(params.containsKey("regex")) { this.regex = params.regex }
    }

    String getRegex() { return this.regex }

    String toString() { return "regex: ${getRegex()}, ${super.toString()}" }

}





def obj = [ color: "Blue", shape: "Circle" ]

def map = ["color": "Blue", "Shape": "Circle", "Foo": [1,2,3] ]

println "obj: " + obj
println "map: " + map

println "map[color]: " + map["color"]
println "map[color]: " + obj.color
println "map[color]: " + map.Foo[1]

def RRR = [  
            [ "retry_count": 7, "retry_delay": 90, "retry_delay_units": "SECONDS", "regex": "SSH Error" ],
            [ "retry_count": 7, "retry_delay": 60, "retry_delay_units": "SECONDS", "regex": "SSH Error" ],
          ]

println "RRR:"

RRR.each {
    _RRRi ->
        println " - " + _RRRi
}


def r = new DelayedRetryOnRegex("retry_count": 3)

println r



