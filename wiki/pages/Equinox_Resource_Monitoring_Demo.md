## Server Setup

  - Install the server zip file into an Eclipse SDK (tested with build
    I0228-0930).
  - Note you can set port and protocol via System properties (but we
    won't).
  - Start the SDK.
      - The server will start automatically.
  - Create resources in the workspace.
      - <strong>File -\> Import -\> Plug-ins and Fragments -\> Import As
        Projects with source folders</strong>
      - Type `org.eclipse.core.*` in the filter to get all the Core
        projects selected and choose <strong>Add</strong>
      - Finish.
      - <em>Note:</em> The Core projects appear in the workspace
  - Look at a preference value.
      - <strong>Window -\> Preferences -\> General -\> Workspace -\>
        Build Automatically</strong>
      - <em>Note:</em> the auto-build preference is set to be true.
      - Close the Preferences window.

## Client

  - Install the client zip file into an Eclipse SDK (tested with build
    I0228-0930).
  - Start the SDK.
  - Switch perspectives and create a connection to the server.
      - <strong>Window -\> Open Perspective -\> Other -\> JMX Resource
        Management</strong>
      - <strong>JMX Server -\> Open Connection</strong>
      - Choose proper host, port and protocol. (default is
        <em>localhost</em>, <em>8118</em>, and <em>rmi</em>)
      - OK.
  - <em>Note:</em> The <strong>Contributions View</strong> will be
    populated with the available contributions from the server.

### Bundles

  - In the <strong>Contributions View</strong>, expand the
    <strong>Bundles</strong> element.
      - <em>Note:</em> These are all the bundles that are installed on
        the server.
  - Expand `org.eclipse.equinox.registry`.
      - <em>Note:</em> The imported packages and required bundles are
        listed as well as the services.
      - Expand the imported packages to show what it requires.
      - Expand the required bundles to show what it requires.
          - Expand `org.eclipse.equinox.common` to show it is recursive.
      - Expand <strong>Services</strong>.
          - <em>Note:</em> there are 3 different icons, one each for
            providing, using, and both.

### Extension Registry

  - In the <strong>Contributions View</strong> expand the
    <strong>Extension Points</strong> element.
      - <em>Note:</em> All the extension points are listed.
  - Navigate to the `org.eclipse.runtime.applications` extension point
      - <em>Note:</em> All of the available applications should be
        there, including the JMX server application and the IDE
        application.
  - <em>TODO:</em> Can we hook in Tom's demos here? click on an
    extension and start it remotely?
  - <em>Future work:</em> add extensions, namespaces, better grouping,
    and more methods.

### Preferences

  - In the <strong>Contributions View</strong> expand the
    <strong>Preferences</strong> element.
  - Expand to <strong>Preferences -\> instance -\>
    org.eclipse.core.resources</strong>.
  - Select the `org.eclipse.core.resources` node.
  - In the <strong>Operations</strong> part of the editor, select the
    `put` operation.
  - In <strong>Invocation View</strong>, set the parameters to be the
    key `description.autobuilding` and the value `false`.
  - Select the <strong>Invoke</strong> button.
  - Look at the preference on the server.
      - On the server choose: <strong>Window -\> Preferences -\> General
        -\> Workspace</strong>.
      - <em>Note:</em> The <strong>Build Automatically</strong>
        preference is now unchecked.

### Services

  - In the <strong>Contributions View</strong> expand the
    <strong>Services</strong> element.
  - Expand the `org.eclipse.core.runtime.IExtensionRegistry` element.
  - Expand the <strong>Bundles</strong> element and see who is importing
    the service, exporting the service, and both.

### Sleak

  - In the <strong>Contributions View</strong> select the
    <strong>Sleak</strong> element.
  - <em>TODO:</em> This is currently broken and needs to be fixed.

### VM

  - In the <strong>Contributions View</strong> expand the <strong>VM
    Stats</strong> element.
  - <em>Note:</em> All the VM info is for the VM currently running the
    server.

### Workspace Resources

  - In the <strong>Contributions View</strong> expand the
    <strong>Workspace Resources</strong> element.
  - <em>Note:</em> The Core projects that you previously imported into
    the server's workspace should be in the list.
  - Select the `org.eclipse.code.boot` project.
  - In the editor, in the <strong>Operations</strong> part of the form,
    select the `delete` method.
  - Make the <strong>Navigator View</strong> from the server instance
    visible to the user.
  - In the <strong>Invocation View</strong> select the
    <strong>Delete</strong> button.
  - <em>Note:</em> The project was deleted from the server's workspace.

[Category:Equinox](Category:Equinox "wikilink")