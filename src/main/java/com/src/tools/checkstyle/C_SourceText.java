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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * A Java source with a map that classifies every character as code, comment, string/char literal or text block, plus
 * the line geometry. All fixers share this single scanner instead of tracking the lexical state on their own.
 */
final class C_SourceText {
    static final byte k_code = 0;
    static final byte k_comment = 1;
    static final byte k_literal = 2;
    static final byte k_textBlock = 3;
    static final int k_indentationWidth = 4;
    static final String k_indentation = " ".repeat( k_indentationWidth );

    private final String m_source;
    private final byte[] m_kinds;
    private final List<Integer > m_lineOffsets;
    private List<String > m_lines;

    C_SourceText( String source ) {
        m_source = source;
        m_kinds = scanKinds( source );
        m_lineOffsets = scanLineOffsets( source );
    }

    static boolean isLineBreak( char character ) {
        return (character == '\n') || (character == '\r');
    }

    static boolean isBlank( char character ) {
        return (character == ' ') || (character == '\t');
    }

    String getSource() {
        return m_source;
    }

    int length() {
        return m_source.length();
    }

    char charAt( int offset ) {
        return m_source.charAt( offset );
    }

    boolean hasCharacter( int offset, char expected ) {
        return (offset >= 0) && (offset < m_source.length()) && (m_source.charAt( offset ) == expected);
    }

    boolean startsWith( int offset, String prefix ) {
        return m_source.startsWith( prefix, offset );
    }

    /** Whether the word starts at the offset and is not just the prefix of a longer identifier. */
    boolean startsWithWord( int offset, String word ) {
        int end = offset + word.length();

        return m_source.startsWith( word, offset )
                && !((end < m_source.length()) && Character.isJavaIdentifierPart( m_source.charAt( end ) ));
    }

    byte getKind( int offset ) {
        return m_kinds[offset];
    }

    boolean isCode( int offset ) {
        return (offset >= 0) && (offset < m_source.length()) && (m_kinds[offset] == k_code);
    }

    /** Whether the offset holds code (not a comment or literal) and is not a line break. */
    boolean isCodeOnLine( int offset ) {
        return isCode( offset ) && !isLineBreak( m_source.charAt( offset ) );
    }

    String getNewline() {
        return m_source.contains( "\r\n" ) ? "\r\n" : "\n";
    }

    /** First offset after the run of blanks (spaces and tabs) that starts at the offset. */
    int skipBlanks( int offset ) {
        int result = offset;

        while( (result < m_source.length()) && isBlank( m_source.charAt( result ) ) ) {
            result++;
        }

        return result;
    }

    /** Start of the run of blanks that ends right before the offset. */
    int skipBlanksBackwards( int offset ) {
        int result = offset;

        while( (result > 0) && isBlank( m_source.charAt( result - 1 ) ) ) {
            result--;
        }

        return result;
    }

    /** Whether only blanks precede the offset on its line. */
    boolean isFirstOnLine( int offset ) {
        int start = skipBlanksBackwards( offset );
        return (start == 0) || isLineBreak( m_source.charAt( start - 1 ) );
    }

    int getLineStart( int offset ) {
        int result = offset;

        while( (result > 0) && !isLineBreak( m_source.charAt( result - 1 ) ) ) {
            result--;
        }

        return result;
    }

    /** The leading blanks of the line that contains the offset. */
    String getIndentation( int offset ) {
        int lineStart = getLineStart( offset );
        return m_source.substring( lineStart, skipBlanks( lineStart ) );
    }

    int getLineCount() {
        return m_lineOffsets.size();
    }

    int getLineOffset( int lineIndex ) {
        return m_lineOffsets.get( lineIndex );
    }

    /** Offset after the line including its terminator. */
    private int getLineEnd( int lineIndex ) {
        return ((lineIndex + 1) < m_lineOffsets.size()) ? m_lineOffsets.get( lineIndex + 1 ) : m_source.length();
    }

    /** The lines without their terminators; like String.split with a negative limit, a final newline adds "". */
    List<String > getLines() {
        if( m_lines == null ) {
            m_lines = new ArrayList<>();

            for( int lineIndex = 0; lineIndex < m_lineOffsets.size(); lineIndex++ ) {
                String line = m_source.substring( m_lineOffsets.get( lineIndex ), getLineEnd( lineIndex ) );
                m_lines.add( line.replaceAll( "[\\r\\n]+$", "" ) );
            }
        }

        return m_lines;
    }

    /** Per line: whether it contains text block characters (opening, content or closing delimiter). */
    List<Boolean > getTextBlockLines() {
        List<Boolean > result = new ArrayList<>();

        for( int lineIndex = 0; lineIndex < m_lineOffsets.size(); lineIndex++ ) {
            int end = getLineEnd( lineIndex );
            boolean isTextBlockLine = false;

            for( int offset = m_lineOffsets.get( lineIndex ); (offset < end) && !isTextBlockLine; offset++ ) {
                isTextBlockLine = m_kinds[offset] == k_textBlock;
            }

            result.add( isTextBlockLine );
        }

        return result;
    }

    DetailAST parse() throws CheckstyleException {
        return JavaParser.parseFileText(
                new FileText( new File( "C_Source.java" ), getLines() ),
                JavaParser.Options.WITHOUT_COMMENTS
        );
    }

    /** Source offset of the first character of a token. */
    int offsetOf( DetailAST ast ) {
        return m_lineOffsets.get( ast.getLineNo() - 1 ) + ast.getColumnNo();
    }

    private static List<Integer > scanLineOffsets( String source ) {
        List<Integer > lineOffsets = new ArrayList<>();
        lineOffsets.add( 0 );

        for( int index = 0; index < source.length(); index++ ) {
            char character = source.charAt( index );

            if( (character == '\r') && ((index + 1) < source.length()) && (source.charAt( index + 1 ) == '\n') ) {
                index++;
                lineOffsets.add( index + 1 );
            }
            else if( isLineBreak( character ) ) {
                lineOffsets.add( index + 1 );
            }
        }

        return lineOffsets;
    }

    private static byte[] scanKinds( String source ) {
        byte[] kinds = new byte[source.length()];
        int index = 0;

        while( index < source.length() ) {
            char character = source.charAt( index );
            char next = ((index + 1) < source.length()) ? source.charAt( index + 1 ) : '\0';
            int end = index + 1;
            byte kind = k_code;

            if( (character == '/') && (next == '/') ) {
                end = index;

                while( (end < source.length()) && !isLineBreak( source.charAt( end ) ) ) {
                    end++;
                }

                kind = k_comment;
            }
            else if( (character == '/') && (next == '*') ) {
                end = source.indexOf( "*/", index + 2 );
                end = (end < 0) ? source.length() : (end + 2);
                kind = k_comment;
            }
            else if( source.startsWith( "\"\"\"", index ) ) {
                end = index + 3;

                while( (end < source.length()) && !source.startsWith( "\"\"\"", end ) ) {
                    end += (source.charAt( end ) == '\\') ? 2 : 1;
                }

                end = Math.min( end + 3, source.length() );
                kind = k_textBlock;
            }
            else if( (character == '"') || (character == '\'') ) {
                end = index + 1;

                while( (end < source.length()) && (source.charAt( end ) != character)
                        && !isLineBreak( source.charAt( end ) )
                ) {
                    end += (source.charAt( end ) == '\\') ? 2 : 1;
                }

                end = Math.min( end + 1, source.length() );
                kind = k_literal;
            }

            if( kind != k_code ) {
                Arrays.fill( kinds, index, end, kind );
            }

            index = end;
        }

        return kinds;
    }
}
