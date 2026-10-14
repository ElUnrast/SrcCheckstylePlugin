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

/** Wraps the compound operands of binary and ternary expressions in parentheses ("paranoid parentheses"). */
final class C_ParenthesisFixer extends C_AstFixer {
    private static final Set<Integer > k_binaryOperators = Set.of(
            TokenTypes.LAND, TokenTypes.LOR, TokenTypes.BAND, TokenTypes.BOR, TokenTypes.BXOR, TokenTypes.EQUAL,
            TokenTypes.NOT_EQUAL, TokenTypes.LT, TokenTypes.GT, TokenTypes.LE, TokenTypes.GE, TokenTypes.PLUS,
            TokenTypes.MINUS, TokenTypes.STAR, TokenTypes.DIV, TokenTypes.MOD, TokenTypes.SL, TokenTypes.SR,
            TokenTypes.BSR
    );

    @Override
    protected void visit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        if( !k_binaryOperators.contains( ast.getType() ) && (ast.getType() != TokenTypes.QUESTION) ) {
            return;
        }

        for( DetailAST operand = ast.getFirstChild(); operand != null; operand = operand.getNextSibling() ) {
            if( needsParentheses( operand, ast ) ) {
                int[] range = getRange( operand, text );

                if( range != null ) {
                    edits.add( C_SourceEdit.insert( range[0], "(" ) );
                    edits.add( C_SourceEdit.insert( range[1], ")" ) );
                }
            }
        }
    }

    private static boolean needsParentheses( DetailAST operand, DetailAST parent ) {
        int type = operand.getType();
        boolean isCompound = k_binaryOperators.contains( type ) || (type == TokenTypes.LITERAL_INSTANCEOF);

        if( !isCompound ) {
            return false;
        }

        DetailAST previous = operand.getPreviousSibling();
        DetailAST next = operand.getNextSibling();

        if( (previous != null) && (previous.getType() == TokenTypes.LPAREN)
                && (next != null) && (next.getType() == TokenTypes.RPAREN)
        ) {
            return false;
        }

        // A chain of the same operator, like "a && b && c", stays without parentheses.
        return (parent.getType() == TokenTypes.QUESTION) || (type != parent.getType());
    }

    /** Returns the source range [start, end) covered by the tokens of the subtree, or null if it is unreliable. */
    private static int[] getRange( DetailAST ast, C_SourceText text ) {
        int[] range = { Integer.MAX_VALUE, -1 };
        return includeInRange( ast, text, range ) ? range : null;
    }

    private static boolean includeInRange( DetailAST ast, C_SourceText text, int[] range ) {
        int type = ast.getType();

        if( (type == TokenTypes.TEXT_BLOCK_LITERAL_BEGIN) || (type == TokenTypes.TEXT_BLOCK_CONTENT)
                || (type == TokenTypes.TEXT_BLOCK_LITERAL_END)
        ) {
            return false;
        }

        int offset = text.offsetOf( ast );

        // Imaginary tokens (EXPR, ELIST, ...) do not occur in the source text.
        if( !ast.getText().isEmpty() && text.startsWith( offset, ast.getText() ) ) {
            range[0] = Math.min( range[0], offset );
            range[1] = Math.max( range[1], offset + ast.getText().length() );
        }

        for( DetailAST child = ast.getFirstChild(); child != null; child = child.getNextSibling() ) {
            if( !includeInRange( child, text, range ) ) {
                return false;
            }
        }

        return true;
    }
}
