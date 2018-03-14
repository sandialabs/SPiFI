package gov.sandia.sems.spifi;

// Jenkins says these must be serializable, but they will execute without it...
// not sure if there are negative consequences to leaving it off so /shrug
class SimpleClass implements Serializable
{
    public static env

    SimpleClass(env)
    {
      this.env = env
      this.env.println ">>> SimpleClass c'tor executing!"
    }

    def hello()
    {
      this.env.println ">>> Hello from SimpleClass!"
    }
}

