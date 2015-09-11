How to make a release
=====================

Preparation
-----------

* Commit/push all changes


Maven Central
-------------

* Run the following command to deploy the artifact:

  <pre>
  mvn release:clean release:prepare release:perform
  </pre>

* After successful deployment, push the changes out:

  <pre>
  git push
  </pre>

