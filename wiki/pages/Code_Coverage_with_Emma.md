Olivier from the [JDT/Core](http://eclipse.org/jdt/core) team has been
working on running the [Emma](http://emma.sourceforge.net/) code
coverage tool on the [JDT/Core](http://eclipse.org/jdt/core) test suites
to get an idea of their coverage. Here are some steps to get the tool
running in your workspace. (Olivier did most of the work... we just put
it on the wiki :-)

## Steps

### Setup

  - Download and extract latest [Emma
    release](http://emma.sourceforge.net/).
  - Put the contents of the `build.xml` file below into a file in your
    workspace.
  - Edit the 3 properties at the top of the file.
      - `project.dir` - the root directory of the project that you are
        testing
      - `output.dir` - the directory where you want your output to go
      - `emma.dir` - your Emma installation directory

### Instrumentation

  - Create a new Ant launch configuration called <i>Emma
    Instrumentation</i> and select the `instr` target.
  - Run the <i>Emma Instrumentation</i> launch configuration. This will
    instrument the class files in your output directory.

### Run the Tests

Next you want to run your tests and collect the output. There are 2 ways
to get the output file in the correct place. You can do
<strong>either</strong> of the following:

1.  Run your test suites with the following System property set:
    `-Demma.coverage.out.file=<output.dir>/coverage.ec` (fill in your
    output directory).
2.  Run your test suite as per normal and copy the `coverage.ec` file to
    your emma_output directory after it is done. (its location will
    appear in the console)

### Create the Report

  - Create a new Ant launch configuration called <i>Emma Create
    Report</i> and select the `report` target.
  - Run the <i>Emma Create Report</i> launch configuration. This will
    generate a report in the output directory. Open the `index.html`
    file to start browsing the results.

### <em>Notes</em>

Some notes and things to think of...

  - Remember to clean your bin/ folder to remove instrumented class
    files.
  - You may have to expand the `export-src` Ant call if you have
    multiple source folders in your project.
  - You will also have to make changes if you have multiple output
    folders in your project.

## build.xml

These are the contents of the Ant build file that you will be running.

    <?xml version="1.0" encoding="UTF-8"?>
    <project name="emma" basedir="../../.">

        <!-- What project are you testing? -->
        <property name="project.dir" value="${basedir}/org.eclipse.core.jobs"/>

        <!-- Where should we output the results?-->
        <property name="output.dir" value="D:/temp/emma_output"/>

        <!-- Emma? Where are you? (Emma installation directory)-->
        <property name='emma.dir' value='D:/downloads/emma-2.0.5312' />

        <!------------------------------------------------------------->

        <!-- Where are the class files? -->
        <property name="bin" value="${project.dir}/bin"/>
        <!-- EMMA distribution directory: -->
        <path id='emma.lib' >
            <fileset dir='${emma.dir}' includes='lib/*.jar' />
        </path>
        <taskdef resource='emma_ant.properties' classpathref='emma.lib' />

        <!-- Target which instruments the class files -->
        <target name="instr">
            <mkdir dir="${output.dir}"/>
            <emma>
                <!-- instrumentation of bin folder -->
                <instr instrpath="${bin}" mode="overwrite" outfile="${output.dir}/coverage.em"/>
            </emma>
            <!-- copy the emma classes into the bin folder -->
            <unjar dest="${bin}" src="${emma.dir}/lib/emma.jar" overwrite="true"/>
        </target>

        <!-- This target copies your source files so they can be used for generation of
                the reports later. -->
        <property name="src.dir" value="${output.dir}/src" />
        <target name="export-src">
            <antcall target="cleanup"/>
            <mkdir dir="${src.dir}" />
            <copy todir="${src.dir}">
                <fileset dir="${project.dir}/src">
                    <include name="**/*.java"/>
                </fileset>
            </copy>
        </target>

        <!-- Generate the report. -->
        <target name="report">
            <antcall target="export-src"/>
            <emma>
                <!-- creating a report -->
                <report sort="+name,+class,+method,+block" sourcepath="${src.dir}">
                    <infileset dir="${output.dir}" includes="*.em, *.ec"/>
                    <html outfile="${output.dir}/index.html" depth="method" columns="name,class,method,block,line"/>
                </report>
            </emma>
        </target>

        <target name="cleanup">
            <delete dir="${src.dir}" failonerror="false"/>

        </target>

    </project>

[Code Coverage](Category:Equinox "wikilink")
[Category:JDT](Category:JDT "wikilink")