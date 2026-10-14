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

/** Enforces whitespace before the closing angle bracket of generic types. */
public class C_GenericEndSpacingCheck extends AbstractCheck {
    private static final String k_message = "Vor '>' in Generics ist ein Leerzeichen erforderlich.";

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
                TokenTypes.GENERIC_END
        };
    }

    @Override
    public void visitToken( DetailAST ast ) {
        int columnIndex = ast.getColumnNo();
        String line = getLines()[ast.getLineNo() - 1];

        // The diamond operator "<>" is exempt.
        if( (columnIndex > 0) && !Character.isWhitespace( line.charAt( columnIndex - 1 ) )
                && (line.charAt( columnIndex - 1 ) != '<')
        ) {
            log( ast.getLineNo(), columnIndex, k_message );
        }
    }
}
