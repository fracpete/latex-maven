How to make a release
=====================

Preparation
-----------

* Commit/push all changes
* Switch to Java 8
* Ensure that Maven 2.2.1 is in use (eg updating MAVEN_HOME env variable)


Maven Central
-------------

* Run the following command to deploy the artifact:

  ```
  mvn release:clean release:prepare release:perform
  ```

