#!/usr/bin/env groovy

List L = []

Integer val = 1
if(val in L) { println "${val} is in L: ${L}"  }
else         { println "${val} not in L: ${L} +++" }

L << 1
L << 2
L += [3,4]

println L

val = 3
if(!(val in L)) { println "${val} not in L: ${L}"  }
else            { println "${val} is in L: ${L} +++" }




