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
import java.util.Deque;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Enforces that multiline parentheses are closed on their own line with the
 * indentation of their opening line. A preceding closing brace is part of the
 * closing line for lambdas and anonymous classes.
 */
public class C_ClosingParenOnOwnLineCheck extends AbstractCheck {
    private static final String k_message =
            "Mehrzeilige Klammerungen müssen mit gleichem Einzug in einer eigenen Zeile geschlossen werden.";

    private final Deque<C_OpeningParenthesis> m_openParentheses = new ArrayDeque<>();

    @Override
    public void finishTree( DetailAST rootAST ) {
        m_openParentheses.clear();
        boolean inBlockComment = false;
        boolean inCharacterLiteral = false;
        boolean inStringLiteral = false;
        boolean inTextBlock = false;

        for( int lineIndex = 0; lineIndex < getLines().length; lineIndex++ ) {
            String line = getLines()[lineIndex];
            C_LineState lineState = new C_LineState( inBlockComment, inCharacterLiteral, inStringLiteral, inTextBlock );
            scanLine( line, lineIndex, lineState );
            inBlockComment = lineState.m_inBlockComment;
            inCharacterLiteral = lineState.m_inCharacterLiteral;
            inStringLiteral = lineState.m_inStringLiteral;
            inTextBlock = lineState.m_inTextBlock;
        }
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
                TokenTypes.LPAREN
        };
    }

    private void checkClosingParenthesis( int lineIndex, int columnIndex ) {
        if( m_openParentheses.isEmpty() ) {
            return;
        }

        C_OpeningParenthesis lParen = m_openParentheses.pop();

        if( lParen.m_lineIndex == lineIndex ) {
            return;
        }

        String closingLine = getLines()[lineIndex];

        if( columnIndex != lParen.m_indentation.length()
                && !istDirektFolgenderKlammerabschluss( closingLine, columnIndex, lParen.m_indentation )
        ) {
            log( lineIndex + 1, columnIndex, k_message );
        }
    }

    private boolean istDirektFolgenderKlammerabschluss( String line, int columnIndex, String indentation ) {
        if( !line.startsWith( indentation ) || (columnIndex <= indentation.length()) ) {
            return false;
        }

        for( int index = indentation.length(); index < columnIndex; index++ ) {
            char character = line.charAt( index );

            if( (character != ')') && (character != '}') && !Character.isWhitespace( character ) ) {
                return false;
            }
        }

        return true;
    }

    private void scanLine( String line, int lineIndex, C_LineState lineState ) {
        for( int columnIndex = 0; columnIndex < line.length(); columnIndex++ ) {
            char character = line.charAt( columnIndex );
            char nextCharacter = getNextCharacter( line, columnIndex );

            if( lineState.m_inBlockComment ) {
                if( character == '*' && nextCharacter == '/' ) {
                    lineState.m_inBlockComment = false;
                    columnIndex++;
                }

                continue;
            }

            if( lineState.m_inTextBlock ) {
                if( character == '"' && nextCharacter == '"' && getNextCharacter( line, columnIndex + 1 ) == '"' ) {
                    lineState.m_inTextBlock = false;
                    columnIndex += 2;
                }

                continue;
            }

            if( lineState.m_inStringLiteral || lineState.m_inCharacterLiteral ) {
                if( character == '\\' ) {
                    columnIndex++;
                }
                else if( character == '"' ) {
                    lineState.m_inStringLiteral = false;
                }
                else if( character == '\'' ) {
                    lineState.m_inCharacterLiteral = false;
                }

                continue;
            }

            if( character == '/' && nextCharacter == '/' ) {
                return;
            }

            if( character == '/' && nextCharacter == '*' ) {
                lineState.m_inBlockComment = true;
                columnIndex++;
            }
            else if( character == '"' && nextCharacter == '"' && getNextCharacter( line, columnIndex + 1 ) == '"' ) {
                lineState.m_inTextBlock = true;
                columnIndex += 2;
            }
            else if( character == '"' ) {
                lineState.m_inStringLiteral = true;
            }
            else if( character == '\'' ) {
                lineState.m_inCharacterLiteral = true;
            }
            else if( character == '(' ) {
                m_openParentheses.push( new C_OpeningParenthesis( lineIndex, getIndentation( line ) ) );
            }
            else if( character == ')' ) {
                checkClosingParenthesis( lineIndex, columnIndex );
            }
        }
    }

    private char getNextCharacter( String line, int columnIndex ) {
        if( columnIndex + 1 < line.length() ) {
            return line.charAt( columnIndex + 1 );
        }

        return '\0';
    }

    private String getIndentation( String line ) {
        int index = 0;

        while( index < line.length() && Character.isWhitespace( line.charAt( index ) ) ) {
            index++;
        }

        return line.substring( 0, index );
    }

    private static class C_OpeningParenthesis {
        private final int m_lineIndex;
        private final String m_indentation;

        C_OpeningParenthesis( int lineIndex, String indentation ) {
            m_lineIndex = lineIndex;
            m_indentation = indentation;
        }
    }

    private static class C_LineState {
        private boolean m_inBlockComment;
        private boolean m_inCharacterLiteral;
        private boolean m_inStringLiteral;
        private boolean m_inTextBlock;

        C_LineState(
                boolean inBlockComment,
                boolean inCharacterLiteral,
                boolean inStringLiteral,
                boolean inTextBlock
        ) {
            m_inBlockComment = inBlockComment;
            m_inCharacterLiteral = inCharacterLiteral;
            m_inStringLiteral = inStringLiteral;
            m_inTextBlock = inTextBlock;
        }
    }
}
