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

import java.util.List;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Corrects generics, the padding of control parentheses, method parentheses and the position of left curlies. */
final class C_AstSpacingFixer extends C_AstFixer {
    private static final Set<Integer > k_controlTokens = Set.of(
            TokenTypes.LITERAL_IF, TokenTypes.LITERAL_FOR, TokenTypes.LITERAL_WHILE, TokenTypes.LITERAL_SWITCH,
            TokenTypes.LITERAL_CATCH, TokenTypes.LITERAL_SYNCHRONIZED
    );
    private static final Set<Integer > k_curlyTokens = Set.of(
            TokenTypes.LITERAL_ELSE, TokenTypes.LITERAL_DO, TokenTypes.LITERAL_TRY, TokenTypes.LITERAL_FINALLY,
            TokenTypes.CTOR_DEF
    );
    private static final Set<Integer > k_parenthesisOwners = Set.of(
            TokenTypes.METHOD_DEF, TokenTypes.CTOR_DEF, TokenTypes.LITERAL_NEW, TokenTypes.CTOR_CALL,
            TokenTypes.SUPER_CTOR_CALL, TokenTypes.ENUM_CONSTANT_DEF
    );

    @Override
    protected void visit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        int type = ast.getType();

        if( type == TokenTypes.GENERIC_START ) {
            collectGenericStartEdit( ast, text, edits );
        }
        else if( type == TokenTypes.GENERIC_END ) {
            collectGenericEndEdit( ast, text, edits );
        }
        else if( k_controlTokens.contains( type ) ) {
            collectControlParenthesisEdits( ast, text, edits );
            collectLeftCurlyEdit( ast, text, edits );
        }
        else if( k_curlyTokens.contains( type ) ) {
            collectLeftCurlyEdit( ast, text, edits );
        }

        if( type == TokenTypes.METHOD_CALL ) {
            collectMethodParenthesisEdit( ast, text, edits );
        }
        else if( k_parenthesisOwners.contains( type ) ) {
            collectMethodParenthesisEdit( ast.findFirstToken( TokenTypes.LPAREN ), text, edits );
        }
    }

    private static void collectGenericStartEdit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        int offset = text.offsetOf( ast );

        if( !text.hasCharacter( offset, '<' ) ) {
            return;
        }

        int end = text.skipBlanks( offset + 1 );

        if( (end > (offset + 1)) && (end < text.length()) && !C_SourceText.isLineBreak( text.charAt( end ) ) ) {
            edits.add( C_SourceEdit.delete( offset + 1, end - offset - 1 ) );
        }
    }

    private static void collectGenericEndEdit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        int offset = text.offsetOf( ast );

        // The diamond operator stays "<>".
        if( text.hasCharacter( offset, '>' ) && (offset > 0)
                && !Character.isWhitespace( text.charAt( offset - 1 ) )
                && (text.charAt( offset - 1 ) != '<')
        ) {
            edits.add( C_SourceEdit.insert( offset, " " ) );
        }
    }

    private static void collectControlParenthesisEdits(
            DetailAST ast,
            C_SourceText text,
            List<C_SourceEdit > edits
    ) {
        DetailAST left = ast.findFirstToken( TokenTypes.LPAREN );
        DetailAST right = ast.findFirstToken( TokenTypes.RPAREN );

        if( left != null ) {
            int offset = text.offsetOf( left );
            int next = offset + 1;

            if( text.hasCharacter( offset, '(' ) && (next < text.length())
                    && !Character.isWhitespace( text.charAt( next ) )
            ) {
                edits.add( C_SourceEdit.insert( next, " " ) );
            }
        }

        if( right != null ) {
            int offset = text.offsetOf( right );

            if( text.hasCharacter( offset, ')' ) && (offset > 0)
                    && !Character.isWhitespace( text.charAt( offset - 1 ) )
            ) {
                edits.add( C_SourceEdit.insert( offset, " " ) );
            }
        }
    }

    /** Removes whitespace between a method name (or "new Type") and its opening parenthesis. */
    private static void collectMethodParenthesisEdit(
            DetailAST parenthesis,
            C_SourceText text,
            List<C_SourceEdit > edits
    ) {
        if( parenthesis == null ) {
            return;
        }

        int offset = text.offsetOf( parenthesis );

        if( !text.hasCharacter( offset, '(' ) ) {
            return;
        }

        int start = text.skipBlanksBackwards( offset );

        if( (start < offset) && (start > 0) && !C_SourceText.isLineBreak( text.charAt( start - 1 ) ) ) {
            edits.add( C_SourceEdit.delete( start, offset - start ) );
        }
    }

    /** Moves a curly brace that sits on its own line to the end of the preceding line. */
    private static void collectLeftCurlyEdit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        DetailAST curly = ast.findFirstToken( TokenTypes.SLIST );

        if( curly == null ) {
            curly = ast.findFirstToken( TokenTypes.LCURLY );
        }

        if( curly == null ) {
            return;
        }

        int offset = text.offsetOf( curly );

        if( !text.hasCharacter( offset, '{' ) ) {
            return;
        }

        int previous = offset - 1;

        while( (previous >= 0) && Character.isWhitespace( text.charAt( previous ) ) ) {
            previous--;
        }

        // Skip when the previous token is a comment, as joining would swallow the brace.
        if( (previous >= 0) && text.isCode( previous )
                && text.getSource().substring( previous + 1, offset ).matches( "\\s*[\\r\\n]\\s*" )
        ) {
            edits.add( C_SourceEdit.replace( previous + 1, offset - previous - 1, " " ) );
        }
    }
}
