### Bundles and exe

  - Each bundle will get a corresponding Installable Unit.
  - Each bundle with specific configuration needs (e.g start level) will
    have an installable unit fragment. A default fragment will provide
    configuration for all other bundles.
  - The eclipse.exe will have a corresponding installable unit.

### Features

The following mapping between features and IUs is kept for backward
compatibility and consumability reasons. However in the future
dependencies should always be expressed on the IUs representing bundles.

  - Each feature is currently mapped to an installable unit (marked as
    group). Each IU depends on the IUs representing the bundles and the
    IUs representing the features.
  - Each feature is mapped to a recommendation (an installable unit
    containing recommendation information). This recommendation will be
    used to indicate which version of an IU should be used. The
    recommendation will only talk about things that are actually being
    delivered by the feature from which the IU is being generated.

The installation of a recommendation installable unit will not cause the
installation of any of the entity referred to from the list of
recommendation. Each recommendation IU will provide as a capability the
"requirements" being refined, thus allowing recommendation IUs to be
used when installing an IU.

  - The two IUs will evolve on different time line since the content of
    the group may not change whereas new versions of an IU are being
    produced.

For example the list of plugins listed in the version 3.2 and 3.2.1 of
the RCP feature is the same. The only thing that changed is the version
to be used, here represented by the recommendation IU. Therefore in this
scenario the recommendation IU will be updated.

### Example

Platform Group 3.2

`requires`
` org.eclipse.core.resources`
` org.eclipse.core.variables`
` org.eclipse.ui`
` org.eclipse.rcp (group)`

Platform recommendation 3.2.0.v2006

` provide recommendation capability for the the platform group`

` provide recommendation capability for org.eclipse.core.resources`
` provide recommendation capability for org.eclipse.core.variables`
` provide recommendation capability for org.eclipse.ui`

` `<touchpoint data>
`   list of recommendations`
`   -`
` `</touchpoint data>

SDK Group 3.2

`requires`
` org.eclipse.platform (group)`
` org.eclipse.pde (group)`

SDK Recommendation 3.2.0.v2006

` provide recommendation capability for SDK group`

` `<touchpoint data>
`   `<list of recommendations/>
`   `<install root/>
` `</touchpoint data>

SDK Recommendation 3.2.1.v2006

` provide recommendation capability for SDK group`

` `<touchpoint data>
`   `<list of recommendations/>
`   `<install root/>
` `</touchpoint data>

### Questions

  - What does a user install?
  - If an IU is being installed and does not change but the
    recommendation that has been picked at install time changes, what
    should be done? How do we detect updates?
  - Most of this information is of a meta level. How does the agent
    recognize the difference between the base and the meta level? The
    meta level introduces dependencies that should not be used when
    sorting the IUs for processing.
  - Are we trying too hard to make everything an IU when we say that a
    recommendation is an IU? It operates on the system before it is ever
    installed, and it typically does not get installed completely. And a
    user would probably never view it as an installable entity, but
    rather something that guides the install.

[Provisioning](Category:Equinox "wikilink")