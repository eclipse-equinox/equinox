Java 2 Security provides fine grained access control to system and
network resources. When a privileged instruction (An instruction which
requires one or more J2SE permissions in order to execute without
throwing a SecurityException at runtime) is coded into a plug-in or
bundle, there are three possible courses of action the developer must
take to address the associated permission requirements:

1.  <b>Treat the privileged instruction as a trusted library
    `   function.`</b>` In this case, only the classes on the execution stack at and below the call to AccessController.doPrivileged will require the permissions.`
    `   This process entails:`
    `   `
    1.  Wrapping the privileged operation in a <i>privileged
        `       action`</i>
    2.  Adding the required permissions to the associated codebase in
        `       the java policy file (i.e. OSGi permissions file(s) for Eclipse`
        `       Plug-ins).`
2.  <b>Add the required permissions to the java policy file.</b>
    `   In this scenario, all classes on the execution stack must have the`
    `   required privilege. Any class which does not have the required`
    `   permission assignment at runtime will throw a security exception.`
3.  <b>Remove the privileged instruction, or refactor the code so that
    option one is viable.</b>

<b>Deciding which approach to take</b>


The goal of security enablement is follow the [Principle of Least
Privilege](http://en.wikipedia.org/wiki/Least_privilege), and only grant
the permissions which are actually required to perform a given task,
rather than granting all permission, or conversely granting too few
permissions which would result in runtime security exceptions. An common
attribute of well designed security enabled application is not to
unnecessarily propagate permission requirements from libraries, bundles
and plug-ins thereby reducing the complexity of using the associated
exported packages.

The simplest approach is to grant the required permission(s) in the
bundle policy file. However, this will require that all classes calling
the method also be granted the required permission, and increases the
complexity of use for any users of the associated library, bundle, or
plug-in.

Alternatively, creating a trusted library requires more work on the
developers part to ensure that they are not unintentionally elevating
the privileges of callers. This ultimately results in more consumable,
easier to use libraries, bundles, and plug-ins. To determine if it is
safe to create a "trusted library", the developer must perform the
following tasks:

  - <b>Check for tainted variables</b> A privileged operation
    `   typically uses parameters, such as filenames , URLs, user names, and`
    `   other inputs. The source of these inputs can be "tainted" data coming`
    `   from untrusted sources. Example sources are method parameters (supplied`
    `   by caller) and other objects which return values (e.g. A FileDialog`
    `   which prompts to the user to supply a path and filename from the`
    `   file-system). This is the "tainted variable" concept. Each call path`
    `   must be analyzed to determine what code-bases (.jar files and classes)`
    `   ultimately make use of a given method. Additionally, the developer must`
    `   analyze each path to determine if tainted objects along the associated`
    `   call paths are referenced by the associated privileged instruction.`
  - <b>Review Method visibility and Access modifiers</b> If a
    `   method is public, then any class capable of loading the class with the`
    `   privileged operation can call it's methods. Great care should be taken`
    `   when implementing a publicly visible trusted library method to avoid`
    `   unintentionally elevating the privileges of the caller.`
  - <b>Review object mutability characteristics</b> Objects which
    `   are intended to be constants after construction should be reviewed, and`
    `   documented. Examples of how objects become mutable are getter/setter`
    `   methods which modify the objects, or contain objects with fields that`
    `   can be modified.`

Finally, if creating a trusted library is desirable but not advisable
due to any of the above tasks turning up problems, refactoring the code
to remove taint flows, correct mutability problems, or remove privileged
instructions is necessary.

[Additional
references](http://domino.research.ibm.com/comm/research_projects.nsf/pages/javasec.resources.html/$FILE/java2network_security.pdf)

[Category:Equinox](Category:Equinox "wikilink") [Privileged
instructions](Category:Security "wikilink")