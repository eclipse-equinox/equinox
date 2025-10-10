---
layout: post
title: p2 Admin UI
summary: Using the p2 provisioning system with the Equinox Launcher
---

* The generated Toc will be an ordered list
{:toc}

## Overview

p2 (Provisioning Platform) is Eclipse's install and update mechanism. This guide covers how to use p2's administrative functions from the command line with the Equinox Launcher, which is useful for automated installations, updates, and maintenance.

## What is p2?

p2 is the Eclipse provisioning platform that:

- Installs and updates Eclipse components
- Manages dependencies between components
- Handles installation profiles
- Provides repository management
- Supports headless and UI-based operations

## p2 Director Application

The p2 director is a headless application for provisioning operations:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.jdt.feature.group
```

### Director Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `-repository` | Comma-separated repository URLs | `-repository http://site1,http://site2` |
| `-installIU` | Installable Unit (IU) to install | `-installIU org.eclipse.jdt.feature.group` |
| `-uninstallIU` | IU to uninstall | `-uninstallIU org.eclipse.mylyn.feature.group` |
| `-destination` | Installation destination | `-destination /path/to/eclipse` |
| `-profile` | Profile name | `-profile SDKProfile` |
| `-profileProperties` | Profile properties | `-profileProperties org.eclipse.update.install.features=true` |
| `-bundlepool` | Bundle pool location | `-bundlepool /shared/bundles` |
| `-p2.os` | Target OS | `-p2.os linux` |
| `-p2.ws` | Target windowing system | `-p2.ws gtk` |
| `-p2.arch` | Target architecture | `-p2.arch x86_64` |
| `-roaming` | Create roaming profile | `-roaming` |
| `-list` | List installed IUs | `-list` |
| `-listTags` | List available tags | `-listTags` |
| `-tag` | Tag this operation | `-tag MyTag` |

## Common p2 Operations

### Installing Features

Install a feature into an existing Eclipse:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/2023-12 \
  -installIU org.eclipse.jdt.feature.group \
  -destination /opt/eclipse \
  -profile SDKProfile
```

### Installing Multiple Features

Install multiple features at once:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/2023-12 \
  -installIU org.eclipse.jdt.feature.group,org.eclipse.pde.feature.group,org.eclipse.cdt.feature.group \
  -destination /opt/eclipse \
  -profile SDKProfile
```

### Uninstalling Features

Remove installed features:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -uninstallIU org.eclipse.mylyn.feature.group \
  -destination /opt/eclipse \
  -profile SDKProfile
```

### Listing Installed Features

List all installed IUs:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -list \
  -destination /opt/eclipse \
  -profile SDKProfile
```

### Creating a New Installation

Create a new Eclipse installation from scratch:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/2023-12 \
  -installIU org.eclipse.platform.ide \
  -destination /opt/new-eclipse \
  -profile SDKProfile \
  -roaming
```

## Repository Management

### Adding Repositories

Use comma-separated list for multiple repositories:

```bash
-repository http://download.eclipse.org/releases/latest,http://example.com/updates
```

### Local Repositories

Use file:// URLs for local repositories:

```bash
-repository file:///home/user/local-repo
```

### Repository Metadata

Cache repository metadata for offline use:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.cache \
  -repository http://download.eclipse.org/releases/latest
```

## Profile Management

### Profile Basics

A profile represents an installation instance. Key concepts:

- **Profile ID**: Unique identifier (e.g., `SDKProfile`, `_SELF_`)
- **Profile Properties**: Configuration for the profile
- **Bundle Pool**: Shared storage for bundles

### Working with Profiles

#### Use Current Profile

Use `_SELF_` as the profile name to modify the running Eclipse:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.jdt.feature.group \
  -profile _SELF_
```

#### Create Shared Bundle Pool

Use a shared bundle pool for multiple installations:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.platform.ide \
  -destination /opt/eclipse-instance1 \
  -bundlepool /shared/eclipse-bundles \
  -profile Instance1Profile
```

## Advanced p2 Operations

### Version-Specific Installation

Install a specific version:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/2023-12 \
  -installIU org.eclipse.jdt.feature.group/4.30.0 \
  -destination /opt/eclipse \
  -profile SDKProfile
```

### Platform-Specific Installation

Install for a different platform:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.platform.ide \
  -destination /opt/eclipse-linux-arm \
  -profile SDKProfile \
  -p2.os linux \
  -p2.ws gtk \
  -p2.arch aarch64
```

### Verify Installation

Verify an installation without modifying it:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.jdt.feature.group \
  -destination /opt/eclipse \
  -profile SDKProfile \
  -verifyOnly
```

### Revert to Previous State

Use tags to mark and revert installations:

```bash
# Tag current state
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -tag BeforeUpdate \
  -destination /opt/eclipse \
  -profile SDKProfile

# Install updates
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.jdt.feature.group \
  -destination /opt/eclipse \
  -profile SDKProfile

# Revert if needed
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -revert BeforeUpdate \
  -destination /opt/eclipse \
  -profile SDKProfile
```

## p2 Repository Applications

### Mirror Application

Create a local mirror of a repository:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication \
  -source http://download.eclipse.org/releases/2023-12 \
  -destination file:///local/mirror/2023-12
```

### Metadata Mirror

Mirror only metadata (not artifacts):

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.metadata.repository.mirrorApplication \
  -source http://download.eclipse.org/releases/2023-12 \
  -destination file:///local/metadata/2023-12
```

## Automation and Scripting

### Installation Script

Example script for automated installation:

```bash
#!/bin/bash
set -e

ECLIPSE_HOME="/opt/eclipse"
INSTALL_DIR="/opt/new-eclipse"
REPO_URL="http://download.eclipse.org/releases/2023-12"

# Features to install
FEATURES=(
    "org.eclipse.platform.ide"
    "org.eclipse.jdt.feature.group"
    "org.eclipse.pde.feature.group"
    "org.eclipse.m2e.feature.feature.group"
)

# Create comma-separated list
IU_LIST=$(IFS=,; echo "${FEATURES[*]}")

# Perform installation
"${ECLIPSE_HOME}/eclipse" \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository "${REPO_URL}" \
  -installIU "${IU_LIST}" \
  -destination "${INSTALL_DIR}" \
  -profile SDKProfile \
  -roaming \
  -consoleLog

echo "Installation complete: ${INSTALL_DIR}"
```

### Update Script

Script to update an existing Eclipse:

```bash
#!/bin/bash
set -e

ECLIPSE_HOME="/opt/eclipse"

# Create backup tag
"${ECLIPSE_HOME}/eclipse" \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -tag "backup-$(date +%Y%m%d-%H%M%S)" \
  -profile _SELF_

# Update all installed features
"${ECLIPSE_HOME}/eclipse" \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -profile _SELF_ \
  -list | grep "feature.group" | cut -d= -f1 | while read -r feature; do
    echo "Updating: ${feature}"
    "${ECLIPSE_HOME}/eclipse" \
      -nosplash \
      -application org.eclipse.equinox.p2.director \
      -repository http://download.eclipse.org/releases/latest \
      -installIU "${feature}" \
      -profile _SELF_
done

echo "Update complete"
```

## Integration with CI/CD

### Docker Example

Dockerfile for Eclipse with specific features:

```dockerfile
FROM ubuntu:22.04

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget \
    libgtk-3-0 \
    openjdk-17-jdk

# Download Eclipse
RUN wget https://download.eclipse.org/eclipse/downloads/.../eclipse-platform.tar.gz \
    && tar -xzf eclipse-platform.tar.gz -C /opt

# Install features via p2
RUN /opt/eclipse/eclipse \
    -nosplash \
    -application org.eclipse.equinox.p2.director \
    -repository http://download.eclipse.org/releases/2023-12 \
    -installIU org.eclipse.jdt.feature.group,org.eclipse.pde.feature.group \
    -destination /opt/eclipse \
    -profile SDKProfile

# Set environment
ENV PATH="/opt/eclipse:${PATH}"

CMD ["eclipse"]
```

### Jenkins Pipeline

Jenkinsfile for provisioning Eclipse:

```groovy
pipeline {
    agent any
    
    environment {
        ECLIPSE_HOME = '/opt/eclipse'
        REPO_URL = 'http://download.eclipse.org/releases/2023-12'
    }
    
    stages {
        stage('Provision Eclipse') {
            steps {
                sh """
                    ${ECLIPSE_HOME}/eclipse \
                      -nosplash \
                      -application org.eclipse.equinox.p2.director \
                      -repository ${REPO_URL} \
                      -installIU org.eclipse.jdt.feature.group \
                      -destination ${WORKSPACE}/eclipse-install \
                      -profile SDKProfile \
                      -roaming
                """
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

#### Missing p2 Application

**Error**: "Application not found: org.eclipse.equinox.p2.director"

**Solution**: Ensure p2 plugins are present:
- `org.eclipse.equinox.p2.director`
- `org.eclipse.equinox.p2.director.app`

#### Repository Connection Failures

**Error**: "Unable to connect to repository"

**Solutions**:
- Check network connectivity
- Verify repository URL
- Check for proxy settings: `-vmargs -Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080`
- Try local mirror

#### Dependency Resolution Failures

**Error**: "Cannot complete the install because of missing dependencies"

**Solutions**:
- Add additional repositories with required dependencies
- Check feature version compatibility
- Use `-list` to see what's already installed

#### Profile Locked

**Error**: "Profile is locked"

**Solution**: Another process is using the profile. Close other Eclipse instances or wait for operations to complete.

### Debug Mode

Enable p2 debugging:

```bash
eclipse \
  -nosplash \
  -application org.eclipse.equinox.p2.director \
  -repository http://download.eclipse.org/releases/latest \
  -installIU org.eclipse.jdt.feature.group \
  -destination /opt/eclipse \
  -profile SDKProfile \
  -consoleLog \
  -vmargs \
  -Dorg.eclipse.equinox.p2.core.debug=true
```

### Verbose Output

Increase logging verbosity:

```bash
-vmargs \
-Dorg.eclipse.equinox.p2.director.debug=true \
-Dorg.eclipse.equinox.p2.engine.debug=true
```

## Best Practices

### Installation Management

1. **Use tags**: Tag installations before major changes
2. **Shared bundle pools**: Save disk space with multiple installations
3. **Local mirrors**: Create mirrors for reliable, fast access
4. **Version pinning**: Specify versions for reproducible installs
5. **Verification**: Always verify critical installations

### Repository Management

1. **Composite repositories**: Use composite repos for multiple sources
2. **Local caching**: Cache repositories for offline use
3. **Repository metadata**: Keep metadata up to date
4. **Mirror strategy**: Mirror critical repositories locally

### Automation

1. **Idempotent scripts**: Make scripts safe to run multiple times
2. **Error handling**: Always check exit codes
3. **Logging**: Use `-consoleLog` for debugging
4. **Cleanup**: Clean up temporary workspaces and configs

### Security

1. **HTTPS repositories**: Use HTTPS when possible
2. **Verify signatures**: Check artifact signatures
3. **Trusted sources**: Only use trusted repository sources
4. **Access control**: Restrict access to internal repositories

## p2 API Usage

For programmatic p2 operations, use the p2 API:

```java
IProvisioningAgent agent = ServiceHelper.getService(
    Activator.getContext(), 
    IProvisioningAgentProvider.class
).createAgent(location);

IProfileRegistry profileRegistry = (IProfileRegistry) 
    agent.getService(IProfileRegistry.SERVICE_NAME);

IProfile profile = profileRegistry.getProfile(profileId);

// Perform provisioning operations
```

## Reference

### Feature Group Naming

Features typically use `.feature.group` suffix:
- `org.eclipse.jdt.feature.group` - JDT
- `org.eclipse.pde.feature.group` - PDE
- `org.eclipse.platform.ide` - Platform IDE

### Common Repository URLs

- Latest Release: `http://download.eclipse.org/releases/latest`
- Specific Release: `http://download.eclipse.org/releases/2023-12`
- Eclipse Platform: `http://download.eclipse.org/eclipse/updates/`
- Orbit: `http://download.eclipse.org/tools/orbit/downloads/`

## See Also

- [Starting Eclipse from Command Line](starting_eclipse_commandline.html)
- [Equinox Launcher](equinox_launcher.html)
- [p2 Documentation](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/p2_overview.htm)
- [p2 Developer Guide](https://wiki.eclipse.org/Equinox/p2)
