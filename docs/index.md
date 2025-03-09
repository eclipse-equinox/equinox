# Equinox Documentation

This is the documentation of the [Eclipse Equinox Framework](https://projects.eclipse.org/projects/eclipse.equinox).

Equinox is an implementation of the OSGi core framework specification, a set of bundles that implement various optional OSGi services and other infrastructure for running OSGi-based systems.

Github: [https://github.com/eclipse-equinox/equinox](https://github.com/eclipse-equinox/equinox)


## Articles

<table class="property-index">
    <thead>
        <th>page</th>
        <th>Description</th>
    </thead>
    <tbody>
        {% for page in site.articles %}
        <tr>
            <td><a href="{{ page.url | prepend: site.baseurl }}">{{page.title | escape}}</a></td>
            <td>{{page.summary | escape}}</td>
        </tr>
        {% endfor %}
    </tbody>
</table>


## Console Commands

<table class="property-index">
    <thead>
        <th>page</th>
        <th>Description</th>
    </thead>
    <tbody>
        {% for page in site.commands %}
        <tr>
            <td><a href="{{ page.url | prepend: site.baseurl }}">{{page.title | escape}}</a></td>
            <td>{{page.summary | escape}}</td>
        </tr>
        {% endfor %}
    </tbody>
</table>


PRs on [Github](https://github.com/eclipse-equinox/equinox/tree/master/docs) to improve the documentation are always welcome.