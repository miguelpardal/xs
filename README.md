# Xml Shorthand #

Tool to convert a shorthand XML language to full XML.

The shorthand language can represent a limited, but useful, subset of XML - without the excessive verbosity.

The following XS syntax:
```
 <root
 <child attribute "value"
 text
 <child /
```

translates to XML as:
```
 <root>
 <child attribute="value">
 text
 </child>
 <child />
 </root>
```

Tag names, attribute names and attribute values can be expanded automatically using the abbreviations map and $.

Attributes for an element must be written in the same (tag) line or the \ character must be used at end of the line to perform line continuation
