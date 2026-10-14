/*
 * Copyright 2026 SRC Security Research & Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.src.tools.checkstyle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the safe source corrections of the style fixer.
 */
public class CTST_JavaStyleFixer {
    @Test
    public void testAddsSpaceBeforeGenericEndButKeepsDiamond() throws Exception {
        assertFixed(
                "class C_A { java.util.Map<String, java.util.List<String >> m_a = new java.util.HashMap<>(); }",
                "class C_A { java.util.Map<String, java.util.List<String > > m_a = new java.util.HashMap<>(); }"
        );
    }

    @Test
    public void testRemovesSpaceAfterGenericStart() throws Exception {
        assertFixed(
                "class C_A { java.util.List< String > m_a; }",
                "class C_A { java.util.List<String > m_a; }"
        );
    }

    @Test
    public void testPadsControlParentheses() throws Exception {
        assertFixed(
                "class C_A { void a( int b ) { if(b > 0) { b++; } while (b < 3 ) { b++; } } }",
                "class C_A { void a( int b ) { if( b > 0 ) {\n    b++;\n}\n\nwhile( b < 3 ) { b++; } } }"
        );
    }

    @Test
    public void testRemovesSpaceBeforeMethodParenthesis() throws Exception {
        assertFixed(
                "class C_A { C_A () { } void a () { a (); new C_A (); } }",
                "class C_A { C_A() { } void a() { a(); new C_A(); } }"
        );
    }

    @Test
    public void testMovesLeftCurlyToEndOfLine() throws Exception {
        assertFixed(
                "class C_A {\n    void a( int b ) {\n        if( b > 0 )\n        {\n            b++;\n        }\n"
                        + "    }\n}\n",
                "class C_A {\n    void a( int b ) {\n        if( b > 0 ) {\n            b++;\n        }\n"
                        + "    }\n}\n"
        );
    }

    @Test
    public void testKeepsCurlyAfterComment() throws Exception {
        String source = "class C_A {\n    void a( int b ) {\n        if( b > 0 ) // note\n        {\n            b++;\n"
                + "        }\n    }\n}\n";
        assertFixed( source, source );
    }

    @Test
    public void testPutsElseOnNewLine() throws Exception {
        assertFixed(
                "class C_A {\n    void a( int b ) {\n        if( b > 0 ) {\n            b++;\n        } else {\n"
                        + "            b--;\n        }\n    }\n}\n",
                "class C_A {\n    void a( int b ) {\n        if( b > 0 ) {\n            b++;\n        }\n"
                        + "        else {\n            b--;\n        }\n    }\n}\n"
        );
    }

    @Test
    public void testFixesCommasAndArrayBrackets() throws Exception {
        assertFixed(
                "class C_A { void a( String [] b ,int c ) { a( b , c); } }",
                "class C_A { void a( String[] b, int c ) { a( b, c); } }"
        );
    }

    @Test
    public void testReplacesTabsOutsideLiterals() throws Exception {
        assertFixed(
                "class C_A {\n\tString m_a = \"x\ty\";\n}\n",
                "class C_A {\n    String m_a = \"x\ty\";\n}\n"
        );
    }

    @Test
    public void testExpandsTabsToTabStopsOfFour() throws Exception {
        assertFixed(
                "class C_A {\n  \tint m_a;\n\t\tint m_b;\n    int\tm_c;\n}\n",
                "class C_A {\n    int m_a;\n        int m_b;\n    int m_c;\n}\n"
        );
    }

    @Test
    public void testLeavesTextBlocksAndCommentsUntouched() throws Exception {
        String source = "class C_A {\n    String m_a = \"\"\"\n        if (x) , y\n        \"\"\";\n"
                + "    // if (x) , y\n}\n";
        assertFixed( source, source );
    }

    @Test
    public void testSplitsSingleLineBlocks() throws Exception {
        assertFixed(
                "class C_A {\n    void a( int b ) {\n        if( b > 0 ) { b++; } else { b--; }\n"
                        + "        try { b++; } catch( RuntimeException e ) { }\n    }\n}\n",
                "class C_A {\n    void a( int b ) {\n        if( b > 0 ) {\n            b++;\n        }\n"
                        + "        else {\n            b--;\n        }\n\n        try {\n            b++;\n        }\n"
                        + "        catch( RuntimeException e ) {\n        }\n    }\n}\n"
        );
    }

    @Test
    public void testKeepsSingleLineBlocksInDefaultMethods() throws Exception {
        String source = "interface I_A {\n    default void a( int b ) {\n        if( b > 0 ) { b++; }\n    }\n}\n";
        assertFixed( source, source );
    }

    @Test
    public void testAddsParenthesesAroundCompoundOperands() throws Exception {
        assertFixed(
                "class C_A { boolean a( int b, Object c ) { return b == 1 && b + 1 < 3 || c instanceof String; } }",
                "class C_A { boolean a( int b, Object c ) {"
                        + " return ((b == 1) && ((b + 1) < 3)) || (c instanceof String); } }"
        );
    }

    @Test
    public void testKeepsChainsOfTheSameOperatorAndExistingParentheses() throws Exception {
        String source = "class C_A { boolean a( int b ) { return (b == 1) && b > 2 && (b < 5) && b != 3; } }";
        assertFixed(
                source,
                "class C_A { boolean a( int b ) { return (b == 1) && (b > 2) && (b < 5) && (b != 3); } }"
        );
    }

    @Test
    public void testAddsParenthesesInTernaryExpressions() throws Exception {
        assertFixed(
                "class C_A { int a( int b ) { return b == 1 ? b + 1 : b; } }",
                "class C_A { int a( int b ) { return (b == 1) ? (b + 1) : b; } }"
        );
    }

    @Test
    public void testMovesSeveralClosingParenthesesOfOneLine() throws Exception {
        assertFixed(
                "class C_A {\n    void a() {\n        foo( bar(\n                1,\n                2 ) );\n"
                        + "    }\n}\n",
                "class C_A {\n    void a() {\n        foo( bar(\n                1,\n                2\n        ) );\n"
                        + "    }\n}\n"
        );
    }

    @Test
    public void testRemovesDoubleBlankLinesBeforeTextBlockButKeepsThoseInside() throws Exception {
        assertFixed(
                "class C_A {\n    int m_a;\n\n\n    String m_b = \"\"\"\n        x\n\n\n        y\n        \"\"\";\n}\n",
                "class C_A {\n    int m_a;\n\n    String m_b = \"\"\"\n        x\n\n\n        y\n        \"\"\";\n}\n"
        );
    }

    @Test
    public void testIsIdempotent() throws Exception {
        String source = "class C_A {\n\tvoid a (int b)\n\t{\n\t\tif(b > 0){ b++; }\n\t\telse { b--; }\n\t}\n}\n";
        String once = C_JavaStyleFixer.fix( source );
        assertEquals( once, C_JavaStyleFixer.fix( once ) );
    }

    private static void assertFixed( String source, String expected ) throws Exception {
        assertEquals( expected, C_JavaStyleFixer.fix( source ) );
    }
}
