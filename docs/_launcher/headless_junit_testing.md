---
layout: post
title: Headless JUnit Testing
summary: Running JUnit tests headlessly with the Equinox Launcher
---

* The generated Toc will be an ordered list
{:toc}

## Overview

Headless JUnit testing allows you to run Eclipse plugin tests in a non-interactive environment, which is essential for continuous integration (CI) and automated testing workflows. This guide covers how to run JUnit tests headlessly using the Equinox Launcher.

## Prerequisites

### Required Components

- Eclipse SDK or Test Framework
- JUnit plugin tests to execute
- Java Development Kit (JDK)
- Test dependencies and target platform

### Test Types

Eclipse supports different types of plugin tests:

1. **JUnit Plugin Tests**: Tests that require OSGi framework
2. **JUnit Tests**: Standard Java unit tests (don't need Eclipse)
3. **SWTBot Tests**: UI tests using SWTBot
4. **Performance Tests**: Eclipse performance test framework

This guide focuses on JUnit Plugin Tests running headlessly.

## Basic Headless Test Execution

### Using Eclipse Test Runner Application

The Eclipse platform provides a test runner application:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -data /tmp/workspace \
  -consoleLog
```

### Specifying Test Class

Run a specific test class:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -data /tmp/workspace \
  -testpluginname com.example.tests \
  -classname com.example.tests.MyTestClass \
  -consoleLog
```

### Specifying Test Suite

Run a test suite:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -data /tmp/workspace \
  -testpluginname com.example.tests \
  -classname com.example.tests.AllTests \
  -consoleLog
```

## Test Application Arguments

### Common Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `-testpluginname` | Bundle containing tests | `-testpluginname com.example.tests` |
| `-classname` | Test class to run | `-classname com.example.MyTest` |
| `-testApplication` | Application to test | `-testApplication org.eclipse.ui.ide.workbench` |
| `-product` | Product to test | `-product org.eclipse.platform.ide` |
| `-data` | Workspace location | `-data /tmp/test-workspace` |
| `-configuration` | Configuration area | `-configuration /tmp/test-config` |
| `-testLoaderClass` | Custom test loader | See below |

### UI vs Non-UI Tests

**Non-UI Tests** (default):
```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.CoreTest
```

**UI Tests** (requires headless display):
```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.uitestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.UITest
```

## Test Results

### Console Output

Test results are written to stdout:

```bash
eclipse -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.MyTest \
  -consoleLog > test-output.txt
```

### XML Results

Generate JUnit XML reports:

```bash
eclipse \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.MyTest \
  -os linux -ws gtk -arch x86_64 \
  -consoleLog \
  > test-results.xml
```

The output follows JUnit XML format compatible with CI tools.

### Formatter Options

Specify result formatter:

```bash
-testlistener org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter
```

## UI Testing Headlessly

### Using Xvfb on Linux

For tests requiring SWT/UI without a physical display:

```bash
# Start Xvfb
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99

# Run UI tests
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.uitestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.UITest \
  -consoleLog
```

Or use `xvfb-run`:

```bash
xvfb-run -a eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.uitestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.UITest \
  -consoleLog
```

### Using Headless SWT

Some tests can run with headless SWT (no display required):

```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.MyTest \
  -vmargs \
  -Djava.awt.headless=true
```

## Integration with Build Tools

### Ant

Example Ant task for running tests:

```xml
<target name="test">
  <exec executable="${eclipse.home}/eclipse" failonerror="true">
    <arg value="-nosplash"/>
    <arg value="-application"/>
    <arg value="org.eclipse.pde.junit.runtime.coretestapplication"/>
    <arg value="-data"/>
    <arg value="${test.workspace}"/>
    <arg value="-testpluginname"/>
    <arg value="com.example.tests"/>
    <arg value="-classname"/>
    <arg value="com.example.tests.AllTests"/>
    <arg value="-consoleLog"/>
  </exec>
</target>
```

### Maven (Tycho)

Using Tycho for Maven-based testing:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-surefire-plugin</artifactId>
  <version>${tycho.version}</version>
  <configuration>
    <useUIHarness>false</useUIHarness>
    <useUIThread>false</useUIThread>
    <argLine>-Xmx1024m</argLine>
    <testFailureIgnore>false</testFailureIgnore>
    <product>org.eclipse.platform.ide</product>
    <application>org.eclipse.pde.junit.runtime.coretestapplication</application>
    <dependencies>
      <dependency>
        <type>p2-installable-unit</type>
        <artifactId>org.eclipse.pde.junit.runtime</artifactId>
      </dependency>
    </dependencies>
  </configuration>
</plugin>
```

For UI tests with Tycho:

```xml
<configuration>
  <useUIHarness>true</useUIHarness>
  <useUIThread>true</useUIThread>
  <application>org.eclipse.pde.junit.runtime.uitestapplication</application>
</configuration>
```

### Gradle

Example Gradle task:

```groovy
task runTests(type: Exec) {
    workingDir project.projectDir
    executable = "${eclipseHome}/eclipse"
    args = [
        '-nosplash',
        '-application', 'org.eclipse.pde.junit.runtime.coretestapplication',
        '-data', "${buildDir}/test-workspace",
        '-testpluginname', 'com.example.tests',
        '-classname', 'com.example.tests.AllTests',
        '-consoleLog'
    ]
}
```

## Continuous Integration

### Jenkins

Example Jenkinsfile:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Test') {
            steps {
                script {
                    sh '''
                        ${ECLIPSE_HOME}/eclipse \
                          -nosplash \
                          -application org.eclipse.pde.junit.runtime.coretestapplication \
                          -data ${WORKSPACE}/test-workspace \
                          -testpluginname com.example.tests \
                          -classname com.example.tests.AllTests \
                          -consoleLog \
                          > test-results.xml
                    '''
                }
                
                junit 'test-results.xml'
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
```

### GitHub Actions

Example workflow:

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Download Eclipse
        run: |
          wget https://download.eclipse.org/eclipse/downloads/.../eclipse-platform.tar.gz
          tar -xzf eclipse-platform.tar.gz
      
      - name: Run Tests
        run: |
          xvfb-run ./eclipse/eclipse \
            -nosplash \
            -application org.eclipse.pde.junit.runtime.coretestapplication \
            -data ./test-workspace \
            -testpluginname com.example.tests \
            -classname com.example.tests.AllTests \
            -consoleLog
      
      - name: Publish Test Results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: test-results.xml
```

### GitLab CI

Example `.gitlab-ci.yml`:

```yaml
test:
  stage: test
  image: ubuntu:22.04
  
  before_script:
    - apt-get update
    - apt-get install -y wget xvfb libgtk-3-0 openjdk-17-jdk
    - wget https://download.eclipse.org/eclipse/downloads/.../eclipse-platform.tar.gz
    - tar -xzf eclipse-platform.tar.gz
  
  script:
    - |
      xvfb-run ./eclipse/eclipse \
        -nosplash \
        -application org.eclipse.pde.junit.runtime.coretestapplication \
        -data ./test-workspace \
        -testpluginname com.example.tests \
        -classname com.example.tests.AllTests \
        -consoleLog > test-results.xml
  
  artifacts:
    reports:
      junit: test-results.xml
```

## Advanced Testing Scenarios

### Running Specific Test Methods

While not directly supported, you can use JUnit test suites:

```java
@RunWith(Suite.class)
@SuiteClasses({
    MyTest.class
})
@Suite.SuiteClasses({
    @Suite.SuiteClasses.ClassNames("com.example.tests.MyTest#testSpecificMethod")
})
public class SpecificTestSuite {
}
```

### Parameterized Tests

Standard JUnit parameterized tests work in headless mode:

```java
@RunWith(Parameterized.class)
public class ParameterizedTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { 1, 2, 3 },
            { 4, 5, 9 }
        });
    }
    
    @Test
    public void testAddition() {
        // Test with parameters
    }
}
```

### Code Coverage

#### With JaCoCo via Tycho

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.10</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

#### Manual Instrumentation

Use JaCoCo agent:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.AllTests \
  -vmargs \
  -javaagent:/path/to/jacocoagent.jar=destfile=coverage.exec
```

## Troubleshooting

### Common Issues

#### Test Application Not Found

**Error**: "Application not found: org.eclipse.pde.junit.runtime.coretestapplication"

**Solution**: Ensure PDE test plugins are installed:
- `org.eclipse.pde.junit.runtime`
- `org.eclipse.jdt.junit.runtime`

#### Display Error (Linux)

**Error**: "Cannot open display"

**Solution**: Use Xvfb:
```bash
xvfb-run -a eclipse -application ...
```

#### Test Plugin Not Found

**Error**: "Could not find test plugin"

**Solution**: 
- Verify plugin is in the plugins directory
- Check `-testpluginname` matches Bundle-SymbolicName
- Ensure test plugin is started (check manifest)

#### Workspace Lock

**Error**: "Workspace is locked"

**Solution**: Use unique workspace per test run:
```bash
-data /tmp/test-workspace-${BUILD_NUMBER}
```

### Debug Mode

Enable debug output:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.AllTests \
  -debug \
  -consoleLog
```

### Verbose Output

Increase logging:

```bash
eclipse \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -testpluginname com.example.tests \
  -classname com.example.tests.AllTests \
  -vmargs \
  -Dorg.eclipse.pde.junit.runtime.debug=true
```

## Best Practices

### Test Organization

1. **Separate test plugins**: Keep tests in dedicated test plugins
2. **Test suites**: Create comprehensive test suites
3. **Test categories**: Use JUnit categories for grouping tests
4. **Naming conventions**: Follow consistent naming (e.g., `*Test.java`)

### CI/CD Integration

1. **Isolated workspaces**: Use unique workspace per build
2. **Clean configuration**: Use `-clean` to avoid cached issues
3. **Timeout handling**: Set reasonable timeouts for test execution
4. **Artifact collection**: Always collect test results and logs
5. **Parallel execution**: Run independent test suites in parallel

### Performance

1. **Reuse Eclipse installation**: Don't download for every run
2. **Workspace cleanup**: Clean up test workspaces after runs
3. **Memory allocation**: Allocate sufficient memory with `-Xmx`
4. **Selective testing**: Run only affected tests when possible

### Reporting

1. **JUnit XML**: Generate standard JUnit XML reports
2. **Console logging**: Always use `-consoleLog`
3. **Workspace logs**: Collect `.metadata/.log` on failures
4. **Screenshots**: Capture screenshots for UI test failures

## Example Script

Complete test execution script:

```bash
#!/bin/bash
set -e

# Configuration
ECLIPSE_HOME="/opt/eclipse"
WORKSPACE="/tmp/test-workspace-$$"
TEST_PLUGIN="com.example.tests"
TEST_CLASS="com.example.tests.AllTests"

# Cleanup function
cleanup() {
    rm -rf "${WORKSPACE}"
}
trap cleanup EXIT

# Run tests
xvfb-run -a "${ECLIPSE_HOME}/eclipse" \
  -nosplash \
  -application org.eclipse.pde.junit.runtime.coretestapplication \
  -data "${WORKSPACE}" \
  -configuration "${WORKSPACE}/.configuration" \
  -testpluginname "${TEST_PLUGIN}" \
  -classname "${TEST_CLASS}" \
  -clean \
  -consoleLog \
  -vmargs \
  -Xmx2048m \
  -XX:+UseG1GC

echo "Tests completed successfully"
```

## See Also

- [Starting Eclipse from Command Line](starting_eclipse_commandline.html)
- [Equinox Launcher](equinox_launcher.html)
- [Tycho Documentation](https://tycho.eclipseprojects.io/)
- [Eclipse PDE Documentation](https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/guide/tools/launchers/junit_launcher.htm)
