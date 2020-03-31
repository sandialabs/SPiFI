# Contributing to SPiFI
Thank you for your interest in contributing to SPiFI development. The procedure
to do so is the following:

## Create a GitHub Issue
Navigate to the SPiFI [GitHub Issues Page](https://spifi/SPiFI/issues) and create
a new issue. The issue can be used for many things such as:
- Bug Reporting
- Suggesting an enhancement
- Asking a question
- etc.

## Work on an Issue
To conduct work on an issue, the proper workflow is the following:

### Fork SPiFI
- If you haven't already forked SPiFI, create a fork on GitHub under your username.
- Clone your fork of SPiFI with `git clone git@github.com:<username>/SPiFI`
- In each new clone of your fork you shoudl create an _upstream_ remote using the
  command `git remote add upstream git@github.com:spifi/SPiFI`. This adds the
  original repository as an upstream remote.

### Updating the Main Development Branches
SPiFI is currently using `master` as its main development branch. We may change
this in the future to use a more formalized `develop` and `master` branch workflow
but for now as a small project we're just using master. To keep your local `master`
updated, you can merge in updates from the upstream master branch.  From the clone
of your fork:
- `git checkout master`
- `git fetch upstream`
- `git merge upstream/master`
- `git push origin master`

This is best done prior to working on any new feature branch to ensure you're 
starting point is current.

### Make Your Changes
Make the changes necessary to address the issue you are tracking. It is ok to
commit small updates / checkpoints in your local repository and then use 
`git rebase -i` to reorganize your commits before sharing. 

Be sure to reference the appropriate GitHub issue in your commits.

### Update Your Branch
While working on your feature in your local branch, other commits may have 
come into the main SPiFI repository. Prior to pushing your changes into the
main repository via a Pull Request it is good to consider merging the latest
`upstream/master` branch into your feature branch. There are many ways to do this
but one possibility is:
- `git checkout <branchname>`
- `git fetch upstream`
- `git merge upstream/master`

### Create Pull Request
When you are ready to merge your updates into the main SPiFI repository, you 
will need to create a [Pull Request](https://github.com/spifi/SPiFI/pulls).
- Push your local feature branch up to your GitHub fork using `git push -u origin <branchname>`
- Navigate to your fork of SPiFI and create a new pull request.
  - When creating the Pull Request, be sure to reference the related Issue.

### Feedback
Once your Pull Request is created, your updates will be reviewed by SPiFI developers
who may have questions or make requests for changes before it is acceptible for 
merging into the main repository. If approved, a team member will merge your updates 
and close the Pull Request and related Issue.


# Useful Resources

## Jenkins Pipelines
- [Jenkins Pipeline information](https://jenkins.io/doc/book/pipeline/)

## Groovy Language
- [Groovy Programming Language](https://groovy-lang.org/)
- [Groovy Programming Docs](https://docs.groovy-lang.org/docs/next/html/documentation/)
- [Groovy Basics](https://docs.smartthings.com/en/latest/getting-started/groovy-basics.html)
