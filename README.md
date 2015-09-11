latex-maven
===========

Maven plugin that allows you to create PDF documentation from LaTeX source code
(fork of https://github.com/mojohaus/latex-maven)

For your `pom.xml`:

```xml
  <build>
    <pluginManagement>
      <plugins>
        ...
        <plugin>
          <groupId>com.github.fracpete</groupId>
          <artifactId>latex-maven-plugin</artifactId>
          <version>1.4.0</version>
          <executions>
            <execution>
              <phase>package</phase>
              <goals>
                <goal>latex</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
        ...
      </plugins>
    </pluginManagement>
    ...
    <plugins>
      <plugin>
        <groupId>com.github.fracpete</groupId>
        <artifactId>latex-maven-plugin</artifactId>
      </plugin>
      ...
    <plugins>
  </build>
```

Goals
-----

Use the following command to build the LaTeX documentation:
```
mvn latex:latex
```

Options
-------

* `latex.docsRoot=...` the directory with the documentation, default: `src/main/latex`
* `latex.commonsDirName=...` the directory with the common LaTeX sources, default: `common`
* `latex.binariesPath=...` the directory with LaTeX binaries
* `latex.bibtex=...` the name of the bibtex binary (eg `bibtex8` or `biber` instead of `bibtex`)
* `latex.skipBuild=true|false` for skipping the build, default: `false`
* `latex.forceBuild=true|false` for forcing the build, default: `false`

