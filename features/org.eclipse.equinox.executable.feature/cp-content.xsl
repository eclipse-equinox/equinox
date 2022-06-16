<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output omit-xml-declaration="yes" indent="yes"/>
<xsl:strip-space  elements="*"/>


<xsl:template match="/units">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
    <xsl:copy-of select="unit[@id='org.eclipse.equinox.executable.feature.group']"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="/units/unit[@id='org.eclipse.equinox.executable.feature.group']/@id">
  <xsl:attribute name="id">
    <xsl:value-of select="'org.eclipse.equinox.executable'"/>
  </xsl:attribute>
</xsl:template>

<xsl:template match="/units/unit[@id='org.eclipse.equinox.executable.feature.group']/provides/provided[@name='org.eclipse.equinox.executable.feature.group']/@name">
  <xsl:attribute name="name">
    <xsl:value-of select="'org.eclipse.equinox.executable'"/>
  </xsl:attribute>
</xsl:template>

<xsl:template match="/units/unit[@id='org.eclipse.equinox.executable.feature.group']/update/@id">
  <xsl:attribute name="id">
    <xsl:value-of select="'org.eclipse.equinox.executable'"/>
  </xsl:attribute>
</xsl:template>

<xsl:template match="/units/unit[@id='org.eclipse.equinox.executable.feature.group']/requires/required[starts-with(@name, 'org.eclipse.equinox.executable_root')]"/>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()">
          <!--xsl:sort select="@id|@name"/-->
    </xsl:apply-templates>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>

