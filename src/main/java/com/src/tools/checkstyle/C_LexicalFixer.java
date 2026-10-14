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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Corrects the rules that only need the lexical context: no whitespace between a control keyword and its
 * parenthesis, comma padding, "Type[]", "else"/"catch"/"finally" on their own line and trailing whitespace.
 */
final class C_LexicalFixer implements C_StyleStage {
    static final List<String > k_continuationKeywords = List.of( "else", "catch", "finally" );

    private static final Pattern k_controlParenthesis = Pattern.compile(
            "(?<![\\w$])(?:if|for|while|switch|catch|synchronized)([ \\t]+)\\("
    );
    private static final Pattern k_trailingWhitespace = Pattern.compile( "[ \\t]+(?=[\\r\\n]|\\z)" );

    /** Whether "else", "catch" or "finally" starts at the offset. */
    static boolean startsWithContinuationKeyword( C_SourceText text, int offset ) {
        return k_continuationKeywords.stream().anyMatch( keyword -> text.startsWithWord( offset, keyword ) );
    }

    @Override
    public String apply( String source ) {
        C_SourceText text = new C_SourceText( source );
        List<C_SourceEdit > edits = new ArrayList<>();

        for( int index = 0; index < source.length(); index++ ) {
            if( text.isCode( index ) ) {
                switch( source.charAt( index ) ) {
                    case ',' -> collectCommaEdits( text, index, edits );
                    case '[' -> collectArrayBracketEdit( text, index, edits );
                    case '}' -> collectClosingCurlyEdit( text, index, edits );
                    default -> {
                    }
                }
            }
        }

        collectControlParenthesisEdits( text, edits );
        collectTrailingWhitespaceEdits( text, edits );
        return C_SourceEdit.apply( source, edits );
    }

    private static void collectControlParenthesisEdits( C_SourceText text, List<C_SourceEdit > edits ) {
        Matcher matcher = k_controlParenthesis.matcher( text.getSource() );

        while( matcher.find() ) {
            if( text.isCode( matcher.start() ) ) {
                edits.add( C_SourceEdit.delete( matcher.start( 1 ), matcher.end( 1 ) - matcher.start( 1 ) ) );
            }
        }
    }

    /** Trailing whitespace inside text blocks is content and stays. */
    private static void collectTrailingWhitespaceEdits( C_SourceText text, List<C_SourceEdit > edits ) {
        Matcher matcher = k_trailingWhitespace.matcher( text.getSource() );

        while( matcher.find() ) {
            if( text.getKind( matcher.start() ) != C_SourceText.k_textBlock ) {
                edits.add( C_SourceEdit.delete( matcher.start(), matcher.end() - matcher.start() ) );
            }
        }
    }

    private static void collectCommaEdits( C_SourceText text, int index, List<C_SourceEdit > edits ) {
        int start = text.skipBlanksBackwards( index );

        if( (start < index) && (start > 0) && !C_SourceText.isLineBreak( text.charAt( start - 1 ) ) ) {
            edits.add( C_SourceEdit.delete( start, index - start ) );
        }

        int next = index + 1;

        if( (next < text.length()) && !Character.isWhitespace( text.charAt( next ) ) ) {
            edits.add( C_SourceEdit.insert( next, " " ) );
        }
    }

    private static void collectArrayBracketEdit( C_SourceText text, int index, List<C_SourceEdit > edits ) {
        int start = text.skipBlanksBackwards( index );

        if( (start == index) || (start == 0) || !text.isCode( start - 1 ) ) {
            return;
        }

        char before = text.charAt( start - 1 );

        if( (Character.isJavaIdentifierPart( before ) || (before == '>') || (before == ']'))
                && text.hasCharacter( text.skipBlanks( index + 1 ), ']' )
        ) {
            edits.add( C_SourceEdit.delete( start, index - start ) );
        }
    }

    /** Puts "else", "catch" and "finally" on a new line when they follow a closing curly of their own line. */
    private static void collectClosingCurlyEdit( C_SourceText text, int index, List<C_SourceEdit > edits ) {
        if( !text.isFirstOnLine( index ) ) {
            return;
        }

        int next = text.skipBlanks( index + 1 );

        if( text.isCode( next ) && startsWithContinuationKeyword( text, next ) ) {
            String lineBreak = text.getNewline() + text.getIndentation( index );
            edits.add( C_SourceEdit.replace( index + 1, next - index - 1, lineBreak ) );
        }
    }
}
