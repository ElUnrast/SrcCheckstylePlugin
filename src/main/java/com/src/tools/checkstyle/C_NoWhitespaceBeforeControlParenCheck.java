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

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Enforces no whitespace between control keywords and the opening parenthesis.
 */
public class C_NoWhitespaceBeforeControlParenCheck extends AbstractCheck {
    private static final String k_message = "Vor '(' bei Kontrollstrukturen ist kein Leerzeichen erlaubt.";

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
                TokenTypes.LITERAL_IF,
                TokenTypes.LITERAL_FOR,
                TokenTypes.LITERAL_WHILE,
                TokenTypes.LITERAL_SWITCH,
                TokenTypes.LITERAL_CATCH,
                TokenTypes.LITERAL_SYNCHRONIZED
        };
    }

    @Override
    public void visitToken( DetailAST ast ) {
        DetailAST lParen = ast.findFirstToken( TokenTypes.LPAREN );

        if( lParen == null ) {
            return;
        }

        int lineIndex = ast.getLineNo() - 1;
        String line = getLines()[lineIndex];

        int keywordEndColumn = ast.getColumnNo() + ast.getText().length();
        int lParenColumn = lParen.getColumnNo();

        if( keywordEndColumn >= lParenColumn ) {
            return;
        }

        String between = line.substring( keywordEndColumn, lParenColumn );

        if( between.chars().anyMatch( Character::isWhitespace ) ) {
            log( lParen.getLineNo(), lParen.getColumnNo(), k_message );
        }
    }
}
