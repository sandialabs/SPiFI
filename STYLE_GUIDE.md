SPiFI Coding Conventions and Style
==================================

- [Groovy](#Groovy)

Groovy
------
- Add 3 empty lines prior to a `class` definition.
- Indentation is 4 spaces.
- Tabs...
  - are not allowed in leading whitespace.
  - discouraged in intermediate whitespace.
- Remove all trailing whitespace.
- Open/Close brackets (Closures) for blocks associated with a function call should be on a new line
  and left-aligned to the calling scope:
  ```groovy
  if (A==1)
  {
  }
  ```
- Instructions inside a closure should be indented by 4 spaces and begin on the next line
  after the opening bracket.
  ```groovy
  if (A==1)
  {
      def B = 99
  }
  ```
  An exception can be made for closure parameters:
  ```groovy
  S.each 
  { item ->
      println("- ${item}")
  }
  ```
- Brackets can be opened on the same line as a Closure definition. 
  ```groovy
  Closure c = { /* single instruction */ }
  // or
  Closure c = {
      // multi line
      // closure
      // body
  }
  ```

