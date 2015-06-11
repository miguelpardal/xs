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

import org.junit.*;
import static org.junit.Assert.*;


/**
 *  XSTest suite
 *
 *  @author Miguel L. Pardal
 */
public class XSTest {

    private XS xs;

    @Before
    public void setUp() {
        xs = new XS();
    }

    @After
    public void tearDown() {
        xs = null;
    }

    // tests -------------------------------------------------------------------

    @Test
    public void testDoc() throws Exception {
        testHelper("doc");
    }

    @Test
    public void testDocAbv() throws Exception {
        xs.getAbvMap().put("tag", "RootTag");
        xs.getAbvMap().put("urn", "urn:expanded");
        xs.getAbvMap().put("attr1", "Attribute1");
        xs.getAbvMap().put("a", "Alpha");
        testHelper("doc-abv");
    }

    @Test
    public void testDocAttrPerLine() throws Exception {
        xs.optionOneAttributePerLine = true;
        testHelper("doc","doc-attr-per-line");
    }

    @Test
    public void testLineContinuation() throws Exception {
        testHelper("line-continuation");
    }

    @Test
    public void testPreamble() throws Exception {
        testHelper("preamble");
    }

    @Test
    public void testTag() throws Exception {
        testHelper("tag");
    }

    @Test
    public void testTagAttrAlternative() throws Exception {
        testHelper("tag-attr-alternative");
    }

    @Test
    public void testTagEmpty() throws Exception {
        testHelper("tag-empty");
    }

    @Test
    public void testTagXsd() throws Exception {
        testHelper("tag-xsd");
    }

    @Test
    public void testText() throws Exception {
        testHelper("text");
    }

    @Test
    public void testTree() throws Exception {
        testHelper("tree");
    }

    @Test
    public void testTreeEmpty() throws Exception {
        testHelper("tree-empty");
    }

    @Test
    public void testTreeSpc2() throws Exception {
        xs.setTabSpaces(2);
        assertEquals(2,xs.getTabSpaces());
        testHelper("tree-spc2");
    }

    @Test
    public void testTreeTabs() throws Exception {
        xs.optionIndentWithSpaces = false;
        testHelper("tree","tree-tabs");
    }

    @Test
    public void testXmlComment() throws Exception {
        testHelper("xml-comment");
    }

    @Test
    public void testXmlCommentTree() throws Exception {
        testHelper("tree-xml-comment");
    }

    @Test
    public void testXsComment() throws Exception {
        testHelper("xs-comment");
    }

    @Test
    public void testHasXSExt() {
        String fileName;

        fileName = "file.xs";
        assertTrue(xs.hasXSFileExt(fileName));

        fileName = "file.XS";
        assertTrue(xs.hasXSFileExt(fileName));

        fileName = "file.xS";
        assertTrue(xs.hasXSFileExt(fileName));

        fileName = "file.xml";
        assertFalse(xs.hasXSFileExt(fileName));
    }


    // helpers -----------------------------------------------------------------

    private void testHelper(String testKey) throws Exception {
        testHelper(testKey,testKey);
    }

    private void testHelper(String inputKey, String expectedKey) throws Exception {

        // open input resource
        InputStream input = XSTest.class.getResourceAsStream("/" + inputKey + ".xs");
        assertNotNull(input);

        // open expected output resource
        InputStream expected = XSTest.class.getResourceAsStream("/" + expectedKey + ".xml");
        assertNotNull(expected);

        // create temp file
        File tempFile = File.createTempFile("XS_" + expectedKey + "_", ".xml");
        System.out.println(tempFile.getCanonicalPath());
        tempFile.deleteOnExit();

        {
            BufferedReader br = null;
            PrintWriter pw = null;
            try {
                br = new BufferedReader(new InputStreamReader(input));
                pw = new PrintWriter(new FileWriter(tempFile));

                xs.convert(br,pw);

            } finally {
                closeQuietly(br);
                closeQuietly(pw);
            }
        }

        {
            BufferedReader br1 = null;
            BufferedReader br2 = null;

            try {
                br1 = new BufferedReader(new InputStreamReader(expected));
                br2 = new BufferedReader(new FileReader(tempFile));

                assertReader(br1,br2);

            } finally {
                closeQuietly(br1);
                closeQuietly(br2);
            }
        }
    }

    // helpers ---------------------------------------------

    // closeQuietly methods inspired by org.apache.commons.io.IOUtil

    public static void closeQuietly(Reader r) {
        if (r != null) {
            try {
                r.close();
            } catch(IOException ioe) {
                // ignore
            }
        }
    }

    public static void closeQuietly(Writer w) {
        if (w != null) {
            try {
                w.close();
            } catch(IOException ioe) {
                // ignore
            }
        }
    }

    // text file comparison --------------------------------

    public static void assertReader(InputStream expectedIS, File actualFile) throws IOException {
        assertReader(new InputStreamReader(expectedIS), new FileReader(actualFile));
    }

    public static void assertReader(Reader expected, Reader actual) throws IOException {
        assertReader(new BufferedReader(expected), new BufferedReader(actual));
    }

    // Compare files, line by line, ignoring system-specific line terminators
    // credits: http://stackoverflow.com/q/466841
    public static void assertReader(BufferedReader expected, BufferedReader actual) throws IOException {
        String line;
        while ((line = expected.readLine()) != null) {
            assertEquals(line, actual.readLine());
        }
        assertNull("Actual had more lines then the expected.", actual.readLine());
        assertNull("Expected had more lines then the actual.", expected.readLine());
    }

}
