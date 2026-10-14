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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Puts the closing parenthesis of a multiline parenthesis on its own line with the indentation of the line that
 * opened it. One correction per line is made per pass; the passes repeat until nothing is left to correct.
 */
final class C_ClosingParenFixer implements C_StyleStage {
    private static final int k_maxPasses = 100;

    @Override
    public String apply( String source ) {
        String result = source;

        for( int pass = 0; pass < k_maxPasses; pass++ ) {
            List<C_SourceEdit > edits = collectEdits( new C_SourceText( result ) );

            if( edits.isEmpty() ) {
                break;
            }

            result = C_SourceEdit.apply( result, edits );
        }

        return result;
    }

    private static List<C_SourceEdit > collectEdits( C_SourceText text ) {
        Deque<C_OpeningParenthesis > openParentheses = new ArrayDeque<>();
        List<C_SourceEdit > edits = new ArrayList<>();

        for( int lineIndex = 0; lineIndex < text.getLineCount(); lineIndex++ ) {
            String line = text.getLines().get( lineIndex );
            int lineStart = text.getLineOffset( lineIndex );
            int indentationLength = 0;

            while( (indentationLength < line.length()) && Character.isWhitespace( line.charAt( indentationLength ) ) ) {
                indentationLength++;
            }

            String indentation = line.substring( 0, indentationLength );
            boolean isCorrected = false;

            for( int column = 0; column < line.length(); column++ ) {
                if( !text.isCode( lineStart + column ) ) {
                    continue;
                }

                if( line.charAt( column ) == '(' ) {
                    openParentheses.push( new C_OpeningParenthesis( lineIndex, indentation ) );
                }
                else if( (line.charAt( column ) == ')') && !openParentheses.isEmpty() ) {
                    C_OpeningParenthesis opening = openParentheses.pop();

                    if( !isCorrected && isMisplaced( opening, lineIndex, line, column ) ) {
                        edits.add( createEdit( text, lineStart, line, column, indentationLength, opening ) );
                        isCorrected = true;
                    }
                }
            }
        }

        return edits;
    }

    private static boolean isMisplaced( C_OpeningParenthesis opening, int lineIndex, String line, int column ) {
        return (opening.m_lineIndex != lineIndex) && (column != opening.m_indentation.length())
                && !isClosingGroupAtIndentation( line, column, opening.m_indentation );
    }

    private static C_SourceEdit createEdit(
            C_SourceText text,
            int lineStart,
            String line,
            int column,
            int indentationLength,
            C_OpeningParenthesis opening
    ) {
        if( column == indentationLength ) {
            return C_SourceEdit.replace( lineStart, indentationLength, opening.m_indentation );
        }

        int blankStart = column;

        while( (blankStart > 0) && Character.isWhitespace( line.charAt( blankStart - 1 ) ) ) {
            blankStart--;
        }

        String lineBreak = text.getNewline() + opening.m_indentation;
        return C_SourceEdit.replace( lineStart + blankStart, column - blankStart, lineBreak );
    }

    /** Whether only closing parentheses and curlies stand between the indentation and the column. */
    private static boolean isClosingGroupAtIndentation( String line, int column, String indentation ) {
        if( !line.startsWith( indentation ) || (column <= indentation.length()) ) {
            return false;
        }

        for( int index = indentation.length(); index < column; index++ ) {
            char character = line.charAt( index );

            if( (character != ')') && (character != '}') && !Character.isWhitespace( character ) ) {
                return false;
            }
        }

        return true;
    }

    private static final class C_OpeningParenthesis {
        private final int m_lineIndex;
        private final String m_indentation;

        C_OpeningParenthesis( int lineIndex, String indentation ) {
            m_lineIndex = lineIndex;
            m_indentation = indentation;
        }
    }
}
