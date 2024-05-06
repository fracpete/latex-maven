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
          <version>1.4.4</version>
          <executions>
            <execution>
              <phase>package</phase>
              <goals>
                <goal>latex</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <forceBuild>true</forceBuild>
          </configuration>
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
    </plugins>
  </build>
```

Goals
-----

Use the following command to build the LaTeX documentation:
```
mvn latex:latex
```

**NB:** Requires Maven [2.2.1](https://archive.apache.org/dist/maven/binaries/apache-maven-2.2.1-bin.zip) and Java 8 for building.

FAQs
----
* My references are undefined: Check if your backend in LaTeX is the same as specified in the pom (bibtex configuration)
* I have no references at all: The name of the bibtex file needs to be the same as the main latex file. If your main latex file is named sample.tex, the bibfile needs to be sample.bib.

Options
-------

* `latex.docsRoot=...` the directory with the documentation, default: `src/main/latex`
* `latex.commonsDirName=...` the directory with the common LaTeX sources, default: `common`
* `latex.binariesPath=...` the directory with LaTeX binaries
* `latex.bibtex=...` the name of the bibtex binary (eg `bibtex8` or `biber` instead of `bibtex`)
* `latex.skipBuild=true|false` for skipping the build, default: `false`
* `latex.forceBuild=true|false` for forcing the build, default: `false`
* `latex.dummyBuild=true|false` for creating dummy PDFs (eg if no LaTeX present), default: `false`
* `latex.extraRuns=...` to run pdflatex more times than normal, default: `0`

