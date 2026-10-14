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

/** Replaces tabs outside of literals and text blocks by spaces up to the next tab stop (one indentation = 4). */
final class C_TabFixer implements C_StyleStage {
    @Override
    public String apply( String source ) {
        if( source.indexOf( '\t' ) < 0 ) {
            return source;
        }

        C_SourceText text = new C_SourceText( source );
        StringBuilder result = new StringBuilder();
        int column = 0;

        for( int index = 0; index < source.length(); index++ ) {
            char character = source.charAt( index );

            if( (character == '\t') && (text.getKind( index ) != C_SourceText.k_literal)
                    && (text.getKind( index ) != C_SourceText.k_textBlock)
            ) {
                int width = C_SourceText.k_indentationWidth - (column % C_SourceText.k_indentationWidth);
                result.append( " ".repeat( width ) );
                column += width;
            }
            else {
                result.append( character );
                column = C_SourceText.isLineBreak( character ) ? 0 : (column + 1);
            }
        }

        return result.toString();
    }
}
