/*
 *  XS - XML Shorthand
 *  https://github.com/miguelpardal/xs
 *
 *  Copyright (c) 2015 Miguel L. Pardal
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT License
 *  which accompanies this distribution, and is available at
 *  http://opensource.org/licenses/MIT
 */
package net.trakchain.xs;

import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 *  XS - Xml Shorthand
 *
 *  Converts shorthand XML language to full XML.
 *  Shorthand XML can represent a limited, but useful, subset of XML -
 *  without the excessive verbosity ;)
 *
 *  The following XS syntax:
 *  <root
 *      <child attribute "value"
 *      text
 *      <child /
 *
 *  translates to XML:
 *  <root>
 *      <child attribute="value">
 *      text
 *      </child>
 *      <child />
 *  </root>
 *
 *  Tag names, attribute names and attribute values
 *  can be expanded automatically using the abbreviations map and $.
 *
 *  Attributes for an element must be written in the same (tag) line
 *  or the \ character must be used at end of the line to perform line continuation
 *
 *  Known limitations:
 *  - spaces to tabs equivalence is used both for input and output
 *  - further improve error messages
 *  - should other whitespace characters be considered? [ \t\n\x0B\f\r]
 *  - improve command line options
 *  - load abbreviations from property files
 *
 *  The code is intentionally minimalistic and simple.
 *  Many more features could be added.
 *  See PXSL for a different approach: http://community.moertel.com/ss/space/pxsl
 *
 *  @author Miguel L. Pardal <miguel.pardal@outlook.com>
 *
 *  2012-04-27 - initial version
 *  2012-05-01 - added support for empty tags <child/> using the following shorthand: <tag/ att="1"
 *  2012-05-03 - modified empty tag <child/> syntax to: <tag att="1" /~
 *             - added abbreviation expand character: <$tag -> <TagMapping>
 *             - changed line continuation character back to '\' at the end of the line
 *  2012-05-09 - added convert file to file; hasXSFileExt; xsToXMLFile
 *  2012-05-10 - added update (convert if target file is older than source file)
 *  2012-05-11 - output XML comments according to their indentation
 *  2015-06-11 - renamed packages
 */
public class XS {

    /** Xml Shorthand file extension */
    public static final String XS_EXT = "xs";
    private static final String DOT_XS_EXT = "." + XS_EXT;

    /** Xml file extension */
    public static final String XML_EXT = "xml";
    private static final String DOT_XML_EXT = "." + XML_EXT;


    // main --------------------------------------------------------------------

    /** Main method - creates an instance and runs it */
    public static void main(String[] args) throws IOException {
        new XS().run(args);
    }

    private static PrintStream err = System.err;

    public XS() {
        initAbvMap();
    }


    /* Tool name */
    private String toolName = "Xml Shorthand tool";

    /* Get tool presentation name */
    public String getToolName() {
        return toolName;
    }

    /* Set tool presentation name */
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    /** Runs the command line tool, processing the specified arguments */
    public void run(String[] args) throws IOException {
        err.println(getToolName());

        if (args.length == 0) {
            // no files specified - take input from stdin and write to stdout
            err.println("Reading from standard input...");
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter pw = new PrintWriter(System.out, /*autoFlush*/ true);
                convert(br,pw);

            } catch(Exception e) {
                err.println("Error: " + e.getMessage());
                err.println("Details:");
                e.printStackTrace(err);
            }

        } else {
            // process one file at a time
            err.println("Processing files...");

            for (int i=0; i < args.length; i++) {
                FileReader fr = null;
                FileWriter fw = null;
                try {
                    File file = new File(args[i]);
                    if (!file.exists()) {
                        throw new IllegalArgumentException(file.getCanonicalPath() + " does not exist!");
                    }
                    if (file.isDirectory()) {
                        throw new IllegalArgumentException(file.getCanonicalPath() + " is a directory!");
                    }

                    String fileName = file.getName();
                    String destFileName = xsToXMLFileName(fileName);
                    File destFile = new File(file.getParentFile(), destFileName);

                    err.printf("%s -> %s%n", fileName, destFile.getName());

                    fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);

                    fw = new FileWriter(destFile);
                    PrintWriter pw = new PrintWriter(fw);

                    convert(br, pw);

                } catch(Exception e) {
                    err.println("Error: " + e.getMessage());
                    err.println("Details:");
                    e.printStackTrace(err);
                } finally {
                    if (fr != null) fr.close();
                    if (fw != null) fw.close();
                }
            }
        }
        err.println("Done!");
    }

    /** Does the file have the XS file extension? */
    public boolean hasXSFileExt(File xsFile) {
        return hasXSFileExt(xsFile.getName());
    }

    /** Does the file name have the XS file extension? */
    public boolean hasXSFileExt(String xsFileName) {
        return xsFileName.toLowerCase().endsWith(DOT_XS_EXT);
    }

    /** get XML file from XS file */
    public File xsToXMLFile(File xsFile) {
        String xmlFileName = xsToXMLFileName(xsFile.getName());
        File xmlFile = new File(xsFile.getParentFile(), xmlFileName);
        return xmlFile;
    }

    /** get XML file name from XS file name */
    public String xsToXMLFileName(String xsFileName) {
        String destFileName;
        if (hasXSFileExt(xsFileName)) {
            destFileName = xsFileName.substring(0, xsFileName.length() - DOT_XS_EXT.length()) + DOT_XML_EXT;
        } else {
            destFileName = xsFileName + DOT_XML_EXT;
        }
        return destFileName;
    }


    // core --------------------------------------------------------------------

    /** tag name */
    private static final String TAG_REGEX = "[\\$a-zA-Z_:][a-zA-Z0-9_:.]+|\\$?[a-zA-Z]";
    // single word character or
    // single word character or _ or : followed by at least one word character or digit or _ or : or .

    /** attribute name */
    private static final String ATT_REGEX = TAG_REGEX;

    /** string */
    private static final String STR_REGEX = "\"(.*?)\"";

    /** tag line compiled pattern to extract tag name */
    private final Pattern TAG_PATTERN = Pattern.compile("<(" + TAG_REGEX + ")\\s*");

    /** tag line compiled pattern to extract attribute name and string value */
    private final Pattern ATT_PATTERN = Pattern.compile("\\s*(" + ATT_REGEX + ")[=|\\s+]?\\s*" + STR_REGEX + "\\s*");


    /** Buffers provided Reader and reads lines of XS language and writes lines of XML */
    public void convert(Reader r, PrintWriter pw) throws XSException, IOException {
        convert(new BufferedReader(r), pw);
    }

    /** Reads lines of XS language and writes lines of XML */
    public void convert(BufferedReader br, PrintWriter pw) throws XSException, IOException {
        String line;
        int lineCount = 0;

        try {
            // stacks for tag closing control
            Stack<Integer> idtStack = new Stack<Integer>();
            Stack<String> tagStack = new Stack<String>();

            // string builder to put together continued lines
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                lineCount++;

                // handle line continuations
                if (line.endsWith("\\")) {
                    sb.append(line.substring(0, line.length()-1));
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(line);
                    line = sb.toString();
                    // reset sb
                    sb = new StringBuilder();
                }

                // check indentation level
                int idt = checkIndent(line);

                // trim line
                String tLine = line.substring(skipIndent(line, idt), line.length());

                if (tLine.startsWith("//")) {
                    // xs comment
                    // (ignore line)

                } else if (tLine.startsWith("<?")) {
                    // xml preamble
                    pw.println(line);

                } else if (tLine.startsWith("<!--")) {
                    // xml comment

                    // close previous tags of equal or greater indentation
                    while (!idtStack.empty() && idtStack.peek() >= idt) {
                        int previousIdt = idtStack.pop();
                        String previousTag = tagStack.pop();

                        // output XML close tag
                        printIndent(pw, previousIdt);
                        pw.printf("</%s>%n", expandAbv(previousTag));
                    }
                    // print comment
                    pw.println(line);

                } else if (tLine.startsWith("<!")) {
                    // dtd element
                    pw.println(line);

                } else if (tLine.startsWith("<")) {
                    // tag line
                    String tag;
                    List<String> attList = new ArrayList<String>();
                    Map<String,String> attMap = new HashMap<String,String>();

                    // handle empty tag
                    int tailIdx = lastNonWhitespace(tLine);
                    boolean isEmptyTag = ('/' == tLine.charAt(tailIdx));
                    if (isEmptyTag) {
                        // trim tail
                        tLine = tLine.substring(0, tailIdx);
                    }

                    // extract tag name
                    Matcher tagMatcher = TAG_PATTERN.matcher(tLine);
                    if (!tagMatcher.find()) {
                        throw new XSException(String.format("Invalid tag line #%d '%s'", lineCount, line), lineCount);
                    } else {
                        tag = tagMatcher.group(1);
                        if (tagMatcher.end() < tLine.length()) {
                            // extract attribute names and values
                            int idx = tagMatcher.end();
                            while (idx < tLine.length()) {
                                Matcher attMatcher = ATT_PATTERN.matcher(tLine);
                                if (attMatcher.find(idx)) {
                                    attList.add(attMatcher.group(1));
                                    attMap.put(attMatcher.group(1), attMatcher.group(2));
                                    idx = attMatcher.end();
                                } else {
                                    throw new XSException(String.format("Invalid attribute in tag line #%d '%s'", lineCount, line), lineCount);
                                }
                            }
                        }
                    }

                    // close previous tags of equal or greater indentation
                    while (!idtStack.empty() && idtStack.peek() >= idt) {
                        int previousIdt = idtStack.pop();
                        String previousTag = tagStack.pop();

                        // output XML close tag
                        printIndent(pw, previousIdt);
                        pw.printf("</%s>%n", expandAbv(previousTag));
                    }

                    if (!isEmptyTag) {
                        // push current tag and indent
                        idtStack.push(idt);
                        tagStack.push(tag);
                    }

                    // output XML open tag with attributes
                    printIndent(pw,idt);

                    pw.printf("<%s", expandAbv(tag));
                    int attIdx = 0;
                    for (String att : attList) {
                        if (optionOneAttributePerLine && attIdx > 0) {
                            pw.printf("%n");
                            printIndent(pw, idt + 1);
                        } else {
                            pw.print(" ");
                        }
                        pw.printf("%s=\"%s\"", expandAbv(att), expandAbv(attMap.get(att)));
                        attIdx++;
                    }
                    if (isEmptyTag)
                        pw.print(" /");
                    pw.printf(">%n");

                } else {
                    // text line
                    pw.println(line);
                }
            }
            // end of file

            // close remaining tags
            while (!idtStack.empty()) {
                int previousIdt = idtStack.pop();
                String previousTag = tagStack.pop();

                // output XML close tag
                printIndent(pw, previousIdt);
                pw.printf("</%s>%n", expandAbv(previousTag));
            }

        } catch(IOException ioe) {
            throw new XSException(ioe);
        } finally {
            // close input stream
            if (br != null) br.close();
        }
    }

    /** Convert XS file to XML file - destination file will be overwritten */
    public File convert(File xsFile, File xmlFile) throws XSException, IOException {
        FileReader fr = null;
        PrintWriter pw = null;
        try {
            fr = new FileReader(xsFile);
            pw = new PrintWriter(new FileWriter(xmlFile));
            convert(fr, pw);
        } catch(IOException ioe) {
            throw new XSException(ioe);
        } finally {
            try {
                if (fr != null) fr.close();
            } catch(IOException ioe) {
                // ignore
            }
            if (pw != null) pw.close();
        }
        return xmlFile;
    }

    /** Convert XS file to implicit XML file - destination file will be overwritten */
    public File convert(File xsFile) throws XSException, IOException {
        File xmlFile = xsToXMLFile(xsFile);
        return convert(xsFile, xmlFile);
    }

    /**
     *  Convert XS file to XML file if necessary
     *  i.e. if source file was modified after target file
     */
    public File update(File xsFile, File xmlFile) throws XSException, IOException {
        if (xsFile.exists() && xmlFile.exists() && xsFile.lastModified() < xmlFile.lastModified()) {
            // convertion not necessary
            return xmlFile;
        } else {
            return convert(xsFile, xmlFile);
        }
    }

    /**
     *  Convert XS file to implicit XML file if necessary
     *  i.e. if source file was modified after target file
     */
    public File update(File xsFile) throws XSException, IOException {
        File xmlFile = xsToXMLFile(xsFile);
        return update(xsFile, xmlFile);
    }


    // indent handling ---------------------------------------------------------

    /** Number of spaces that are equivalent to a tab */
    private int tabSpaces = 4;

    /** Get the number of spaces that are equivalent to a tab */
    public int getTabSpaces() {
        return tabSpaces;
    }

    /** Set the number of spaces that are equivalent to a tab */
    public void setTabSpaces(int nr) {
        if (nr < 0) {
            String eMsg = String.format("The number of spaces equivalent to a tab cannot be %d!", nr);
            throw new IllegalArgumentException(eMsg);
        }
        this.tabSpaces = nr;
    }

    /** Check what is the indent level of the given line */
    private int checkIndent(String line) {
        int spc = 0;
        int tab = 0;
        for(int idx = 0; idx < line.length(); idx++) {
            char c = line.charAt(idx);
            if (' ' == c) {
                spc++;
            } else if ('\t' == c) {
                tab++;
            } else {
                // found non whitespace character
                break;
            }
        }
        return indentCount(tab,spc);
    }

    /** Indent count = tabs + spaces/spacesPerTab */
    private int indentCount(int tab, int spc) {
        if (tabSpaces == 0) // avoid divide by zero
            return tab;
        else
            return tab + ((int)spc/tabSpaces);
    }

    /* returns index of first character in line after indent */
    private int skipIndent(String line, int indent) {
        if (indent == 0)
            return 0;
        int spc = 0;
        int tab = 0;
        int idx;
        for (idx=0; idx < line.length(); idx++) {
            char c = line.charAt(idx);
            if (' ' == c) {
                spc++;
            } else if ('\t' == c) {
                tab++;
            } else {
                throw new IllegalStateException("Only space or tab characters were expected");
            }
            if (indentCount(tab, spc) == indent)
                break;
        }
        return idx+1;
    }

    /** prints the indentation using spaces */
    private void printIndent(PrintWriter pw, int indent) {
        if (optionIndentWithSpaces) {
            for (int i=0; i < indent*tabSpaces; i++) {
                pw.print(" ");
            }
        } else {
            for (int i=0; i < indent; i++) {
                pw.print("\t");
            }
        }
    }

    /* returns index of last non-whitespace character in line */
    private int lastNonWhitespace(String line) {
        int idx;
        for (idx=line.length()-1; idx >= 0; idx--) {
            char c = line.charAt(idx);
            if (c != ' ' && c != '\t') {
                break;
            }
        }
        return idx;
    }

    // abbreviations -----------------------------------------------------------

    /** Abbreviations map */
    private Map<String,String> abvMap;

    /** Get abbreviations map - each mapping is used to expand tag, attribute and attribute values */
    public Map<String,String> getAbvMap() {
        return abvMap;
    }

    /** Set abbreviations map */
    public void setAbvMap(Map<String,String> abvMap) {
        this.abvMap = abvMap;
    }

    /** define built-in abbreviations */
    private void initAbvMap() {
        abvMap = new LinkedHashMap<String,String>();

        // XML Schema namespaces
        abvMap.put("ns-xsi","http://www.w3.org/2001/XMLSchema-instance");
        abvMap.put("ns-xs","http://www.w3.org/2001/XMLSchema");
    }


    /** Expand abbreviation to mapping in map and return it or return original value */
    private String expandAbv(String s) throws XSException {
        if (!s.startsWith("$")) {
            return s;
        } else {
            if (s.length() == 1) {
                // "$".equals(s)
                throw new XSException("Invalid abbreviation '$'!");
            }
            String key = s.substring(1, s.length());
            String expanded = abvMap.get(key);
            if (expanded == null) {
                throw new XSException(String.format("Abbreviation '%s' not defined!", key));
            }
            return expanded;
        }
    }

    // options -----------------------------------------------------------------

    /** print indentation as spaces? */
    public boolean optionIndentWithSpaces = true;

    /** write one attribute per line? */
    public boolean optionOneAttributePerLine = false;

}
