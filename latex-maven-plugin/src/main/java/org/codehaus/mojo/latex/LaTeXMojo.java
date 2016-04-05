package org.codehaus.mojo.latex;

/*
 * Copyright 2010 INRIA / CITI Laboratory / Amazones Research Team.
 * Copyright 2015 University of Waikato, Hamilton, NZ.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import static org.apache.commons.exec.CommandLine.parse;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.iterateFiles;

/**
 * LaTeX documents building goal.
 *
 * @author Julien Ponge
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @goal latex
 * @phase compile
 */
public class LaTeXMojo
    extends AbstractMojo
{

    /** template for dummy PDFs, taken from <a href="https://brendanzagaeski.appspot.com/0004.html">here</a> */
    public final static String DUMMY_TEMPLATE = "%PDF-1.1\n" +
      "%¥±ë\n" +
      "\n" +
      "1 0 obj\n" +
      "  << /Type /Catalog\n" +
      "     /Pages 2 0 R\n" +
      "  >>\n" +
      "endobj\n" +
      "\n" +
      "2 0 obj\n" +
      "  << /Type /Pages\n" +
      "     /Kids [3 0 R]\n" +
      "     /Count 1\n" +
      "     /MediaBox [0 0 595 842]\n" +
      "  >>\n" +
      "endobj\n" +
      "\n" +
      "3 0 obj\n" +
      "  <<  /Type /Page\n" +
      "      /Parent 2 0 R\n" +
      "      /Resources\n" +
      "       << /Font\n" +
      "           << /F1\n" +
      "               << /Type /Font\n" +
      "                  /Subtype /Type1\n" +
      "                  /BaseFont /Times-Roman\n" +
      "               >>\n" +
      "           >>\n" +
      "       >>\n" +
      "      /Contents 4 0 R\n" +
      "  >>\n" +
      "endobj\n" +
      "\n" +
      "4 0 obj\n" +
      "  << /Length 59 >>\n" +
      "stream\n" +
      "  BT\n" +
      "    /F1 18 Tf\n" +
      "    240 440 Td\n" +
      "    ({NAME}) Tj\n" +
      "  ET\n" +
      "endstream\n" +
      "endobj\n" +
      "\n" +
      "xref\n" +
      "0 5\n" +
      "0000000000 65535 f \n" +
      "0000000018 00000 n \n" +
      "0000000077 00000 n \n" +
      "0000000178 00000 n \n" +
      "0000000457 00000 n \n" +
      "trailer\n" +
      "  <<  /Root 1 0 R\n" +
      "      /Size 5\n" +
      "  >>\n" +
      "startxref\n" +
      "{STARTXREF}\n" +
      "%%EOF\n";

    /** the startxref offset if printed name is length 0. */
    public final static int XREF_OFFSET = 558;

    /** the placeholder for the name in the PDF template. */
    public final static String PLACEHOLDER_NAME = "{NAME}";

    /** the placeholder for the startxref value in the PDF template. */
    public final static String PLACEHOLDER_STARTXREF = "{STARTXREF}";

    /**
     * The documents root.
     *
     * @parameter expression="${latex.docsRoot}" default-value="src/main/latex"
     * @required
     */
    private File docsRoot;

    /**
     * Common files directory inside the documents root (the only directory to be skipped).
     *
     * @parameter expression="${latex.commonsDirName}" default-value="common"
     * @required
     */
    private String commonsDirName;

    /**
     * The Maven build directory.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File buildDir;

    /**
     * The LaTeX builds directory.
     *
     * @parameter expression="${project.latex.build.directory}" default-value="${project.build.directory}/latex"
     * @required
     */
    private File latexBuildDir;

    /**
     * Path to the LaTeX binaries installation.
     *
     * @parameter expression="${latex.binariesPath}" default-value=""
     */
    private String binariesPath;

    /**
     * Name of bibtex executable. Use this to invoke bibtex8 or biber instead of bibtex.
     *
     * @parameter expression="${latex.bibtex}" default-value="bibtex"
     */
    private String bibtex;

    /**
     * Allows to skip the build.
     *
     * @parameter expression="${latex.skipBuild}" default-value="false"
     */
    private boolean skipBuild;

    /**
     * Allows to force the build.
     *
     * @parameter expression="${latex.forceBuild}" default-value="false"
     */
    private boolean forceBuild;

    /**
     * Generates dummy PDFs. Useful when no LaTeX installed.
     *
     * @parameter expression="${latex.dummyBuild}" default-value="false"
     */
    private boolean dummyBuild;

    /**
     * Extra times to run pdflatex. Useful to get references right.
     *
     * @parameter expression="${latex.extraRuns}" default-value="0"
     */
    private int extraRuns;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            if (!docsRoot.exists())
            {
                getLog().info("Directory '" + docsRoot + "' does not exist, skipped!");
                return;
            }
            if (skipBuild)
            {
                getLog().info("Build skipped!");
                return;
            }
            final File[] docDirs = getDocDirs();
            if (docDirs.length == 0) {
                getLog().info("Directory '" + docsRoot + "' contains no sub-directories, skipped!");
                return;
            }
            final File[] buildDirs = prepareLaTeXBuildDirectories( docDirs );
            buildDocuments( buildDirs );
        }
        catch ( IOException e )
        {
            getLog().error( e );
            throw new MojoFailureException( e.getMessage() );
        }
    }

    /**
     * Creates a dummy PDF file in the build directory.
     *
     * @param name the name of the PDF file
     */
    private void createDummyFile( String name )
    {
        getLog().info( "Generating dummy PDF (name): " + name );

        int startxref = XREF_OFFSET + name.length();
        getLog().info( "Generating dummy PDF (startxref): " + startxref );

        String pdf = DUMMY_TEMPLATE;
        pdf = pdf.replace(PLACEHOLDER_NAME, name);
        pdf = pdf.replace(PLACEHOLDER_STARTXREF, "" + startxref);

        BufferedWriter bwriter = null;
        FileWriter fwriter = null;
        File output = new File(buildDir, name + ".pdf");
        getLog().info( "Generating dummy PDF (output): " + output );
        try {
            fwriter = new FileWriter(output, false);
            bwriter = new BufferedWriter(fwriter);
            bwriter.write(pdf);
            bwriter.flush();
        }
        catch (Exception e) {
            getLog().error("Failed to generate dummy PDF: " + output, e);
        }
        finally {
            IOUtils.closeQuietly(bwriter);
            IOUtils.closeQuietly(fwriter);
        }
    }

    private void buildDocuments( File[] buildDirs )
        throws IOException, MojoFailureException
    {
        for ( File dir : buildDirs )
        {
            final File texFile = new File( dir, dir.getName() + ".tex" );
            final File pdfFile = new File( dir, dir.getName() + ".pdf" );
            final File bibFile = new File( dir, dir.getName() + ".bib" );
            final File gloFile = new File( dir, dir.getName() + ".glo" );

            if ( dummyBuild )
            {
                createDummyFile( dir.getName() );
                return;
            }

            if ( forceBuild || requiresBuilding(dir, pdfFile) )
            {

                final CommandLine pdfLaTeX =
                    parse( executablePath( "pdflatex" ) )
                        .addArgument( "-shell-escape" )
                        .addArgument( "--halt-on-error" )
                        .addArgument( texFile.getAbsolutePath() );
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "pdflatex: " + pdfLaTeX );
                }

                final CommandLine bibTeX = parse( executablePath( bibtex ) ).addArgument( dir.getName() );
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "bibtex: " + bibTeX );
                }

                final CommandLine makeglossaries = parse( executablePath( "makeglossaries" ) ).addArgument( dir.getName() );
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "makeglossaries: " + makeglossaries );
                }

                execute( pdfLaTeX, dir );
                if ( gloFile.exists() )
                {
                    execute( makeglossaries, dir );
                }
                if ( bibFile.exists() )
                {
                    execute( bibTeX, dir );
                    execute( pdfLaTeX, dir );
                }

                for ( int runs = extraRuns + 1; runs > 0; runs-- )
                {
                    execute( pdfLaTeX, dir );
                }

                copyFile( pdfFile, new File( buildDir, pdfFile.getName() ) );
            }
        }
    }

    private boolean requiresBuilding( File dir, File pdfFile )
        throws IOException
    {
        Collection texFiles = FileUtils.listFiles( dir, new String[]{ "tex", "bib" }, true );
        getLog().info(texFiles.toString());
        if ( pdfFile.exists() )
        {
            boolean upToDate = true;
            Iterator it = texFiles.iterator();
            while( it.hasNext() && upToDate )
            {
                File file = (File) it.next();
                if ( FileUtils.isFileNewer(file, pdfFile ) )
                {
                    if ( getLog().isInfoEnabled() )
                    {
                        getLog().info( "Changes detected on " + file.getAbsolutePath() );
                    }
                    return true;
                }
                if ( getLog().isInfoEnabled() )
                {
                    getLog().info( "No change detected on " + file.getAbsolutePath() );
                }
            }
             if ( getLog().isInfoEnabled() )
             {
                getLog().info( "Skipping: no LaTeX changes detected in " + dir.getCanonicalPath() );
             }
            return false;
        }
        else
        {
            return true;
        }
    }

    private String executablePath( String executable )
    {
        if ( binariesPath == null )
        {
            return executable;
        }
        else
        {
            return new StringBuilder().append( binariesPath ).append( File.separator ).append( executable ).toString();
        }
    }

    private void execute( CommandLine commandLine, File dir )
        throws IOException, MojoFailureException
    {
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory( dir );
        if ( executor.execute( commandLine ) != 0 )
        {
            throw new MojoFailureException( "Error code returned for: " + commandLine.toString() );
        }
    }

    private File[] prepareLaTeXBuildDirectories( File[] docDirs )
        throws IOException
    {
        final File[] buildDirs = new File[docDirs.length];
        final File commonsDir = new File( docsRoot, commonsDirName );

        for ( int i = 0; i < docDirs.length; i++ )
        {
            final File dir = docDirs[i];
            final File target = new File( latexBuildDir, docDirs[i].getName() );
            buildDirs[i] = target;

            copyDirectory( dir, target );
            if ( commonsDir.exists() )
            {
                copyDirectory( commonsDir, target );
            }

            final Iterator iterator = iterateFiles(target, new String[]{ ".svn" }, true);
            while ( iterator.hasNext() )
            {
                FileUtils.deleteDirectory( (File) iterator.next());
            }

        }

        return buildDirs;
    }

    private File[] getDocDirs()
    {
        return docsRoot.listFiles( new FileFilter()
        {
            public boolean accept( File pathname )
            {
                return pathname.isDirectory() && !( pathname.getName().equals( commonsDirName ) ) &&
                    !( pathname.isHidden() );
            }
        } );
    }

}
