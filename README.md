Sems Pipeline Framework Infrastructure (SPiFI)
----------------------------------------------
The **S**EMS **Pi**peline **F**ramework **I**nfrastructure (SPiFI) is a library
of helpers written to simplify common tasks used in scripted Jenkins Pipeline
jobs.

These tasks include:

- Executing Shell Scripts
- Cloning Git repositories
- Launching multiple Jenkins jobs in parallel & aggregating their results
- Develop formatted emails for notifications

SPiFI provides convenience features like setting timeouts, retries, dry-run mode
(for debugging), etc. to assist pipeline development and allow the pipeline
developer to focus more on "what" the pipeline does rather than "how" to do it
(which often involves a fight with what parts of Groovy Jenkins' sandbox enables).


### Jenkins Pipelines
The [Jenkins Pipeline Plugin][2] provides a capability to build continuous
delivery pipelines into Jenkins.  A continuous delivery pipeline provides an
automated expression of a process that progresses software multiple stages of
building, testing, and deployment.  Jenkins Pipelines can be expressed in two
syntax forms, declarative and scripted.  The declarative format is a new
addition to Pipelines and is not the focus of the SPiFI library.
The scripted format is implemented using a [sandboxed][4] version of the
[Apache Groovy][3] language.






