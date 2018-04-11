#!/usr/bin/env groovy

List L = [ "item1",
           "item2",
           "item3",
           "item4",
           "item5"
         ]

println L


// Can I comment out an entry from the middle of the list, 
// or will Groovy get confused?
List L2 = [ "item1",
            "item2",
//            "item3",
            "item4",
            "item5"
         ]

println L2
