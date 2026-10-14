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

/** Splits "if( a ) { b(); }" into several lines, as the RightCurly rule requires (except in default methods). */
final class C_BlockSplitFixer extends C_AstFixer {
    private static final Set<Integer > k_blockTokens = Set.of(
            TokenTypes.LITERAL_IF, TokenTypes.LITERAL_ELSE, TokenTypes.LITERAL_TRY, TokenTypes.LITERAL_CATCH,
            TokenTypes.LITERAL_FINALLY
    );

    @Override
    protected void visit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        if( !k_blockTokens.contains( ast.getType() ) || isInDefaultMethod( ast ) ) {
            return;
        }

        DetailAST block = ast.findFirstToken( TokenTypes.SLIST );
        DetailAST end = (block == null) ? null : block.findFirstToken( TokenTypes.RCURLY );

        if( end == null ) {
            return;
        }

        int open = text.offsetOf( block );
        int close = text.offsetOf( end );

        if( text.hasCharacter( open, '{' ) && text.hasCharacter( close, '}' ) ) {
            collectEdits( text, open, close, edits );
        }
    }

    private static boolean isInDefaultMethod( DetailAST ast ) {
        for( DetailAST parent = ast.getParent(); parent != null; parent = parent.getParent() ) {
            if( parent.getType() == TokenTypes.METHOD_DEF ) {
                DetailAST modifiers = parent.findFirstToken( TokenTypes.MODIFIERS );

                if( (modifiers != null) && (modifiers.findFirstToken( TokenTypes.LITERAL_DEFAULT ) != null) ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void collectEdits( C_SourceText text, int open, int close, List<C_SourceEdit > edits ) {
        String lineBreak = text.getNewline() + text.getIndentation( open );
        int afterOpen = text.skipBlanks( open + 1 );
        int beforeClose = Math.max( text.skipBlanksBackwards( close ), open + 1 );

        if( afterOpen == close ) {
            edits.add( C_SourceEdit.replace( open + 1, close - open - 1, lineBreak ) );
        }
        else {
            if( text.isCodeOnLine( afterOpen ) ) {
                String innerLineBreak = lineBreak + C_SourceText.k_indentation;
                edits.add( C_SourceEdit.replace( open + 1, afterOpen - open - 1, innerLineBreak ) );
            }

            if( !C_SourceText.isLineBreak( text.charAt( beforeClose - 1 ) ) && text.isCode( beforeClose - 1 ) ) {
                edits.add( C_SourceEdit.replace( beforeClose, close - beforeClose, lineBreak ) );
            }
        }

        int afterClose = text.skipBlanks( close + 1 );

        if( text.isCodeOnLine( afterClose ) && !C_LexicalFixer.startsWithContinuationKeyword( text, afterClose ) ) {
            edits.add( C_SourceEdit.replace( close + 1, afterClose - close - 1, lineBreak ) );
        }
    }
}
