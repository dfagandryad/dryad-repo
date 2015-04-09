<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">
    
    <xsl:template name="text-plain">
        <html>
            <head>
                <link type="text/css" rel="stylesheet" href="{$ddwcss}"></link>
            </head>
            <body class="dryad-ddw-bitstream">
                <pre>
<xsl:value-of select="/xhtml:xhtml/xhtml:body"/>
</pre>
            </body>
        </html>
    </xsl:template>
    
</xsl:stylesheet>
