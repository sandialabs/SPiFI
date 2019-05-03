SPiFI Tests
-----------

This directory contains jenkinsfiles for tests that can be set up to run against
the SPiFI library.

The main test driver files are:
- `SPiFI_test_driver.jenkinsfile` - This tests the `master` branch on jenkins-srn-dev.sandia.gov
- `SPiFI_test_driver_default.jenkinsfile` - This test loads the `default` branch (i.e., `@Library('SPiFI') _`) which will be the current 'release' version on the production servers.  This test should not be edited until AFTER a new version is deployed.
- `SPiFI_test_driver_*.jenkinsfile` - Other test driver files that might be set up for specific tasks. These should only be edited if you know exatly which Jenkins jobs in testing are driving these.


