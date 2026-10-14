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

/**
 * Corrects the blank lines: no double blank lines, none before case labels and chained calls, and one around block
 * statements. Lines of text blocks are content and stay as they are.
 */
final class C_BlankLineFixer implements C_StyleStage {
    @Override
    public String apply( String source ) {
        C_SourceText text = new C_SourceText( source );
        List<String > lines = new ArrayList<>( text.getLines() );
        List<Boolean > textBlockLines = new ArrayList<>( text.getTextBlockLines() );
        boolean endsWithNewline = source.endsWith( "\n" );

        if( endsWithNewline && !lines.isEmpty() && lines.get( lines.size() - 1 ).isEmpty() ) {
            lines.remove( lines.size() - 1 );
            textBlockLines.remove( textBlockLines.size() - 1 );
        }

        removeDoubleBlankLines( lines, textBlockLines );
        removeBlankLinesBeforeContinuations( lines, textBlockLines );
        addBlankLinesAroundBlocks( lines, textBlockLines );
        String fixedSource = String.join( text.getNewline(), lines );
        return endsWithNewline ? (fixedSource + text.getNewline()) : fixedSource;
    }

    private static void removeDoubleBlankLines( List<String > lines, List<Boolean > textBlockLines ) {
        for( int index = 1; index < lines.size(); index++ ) {
            if( lines.get( index ).isBlank() && lines.get( index - 1 ).isBlank()
                    && !textBlockLines.get( index ) && !textBlockLines.get( index - 1 )
                    && !isDeclarationOrAnnotation( getNextNonBlankLine( lines, index + 1 ) )
            ) {
                lines.remove( index );
                textBlockLines.remove( index );
                index--;
            }
        }
    }

    private static void removeBlankLinesBeforeContinuations( List<String > lines, List<Boolean > textBlockLines ) {
        for( int index = 1; index < lines.size(); index++ ) {
            String line = lines.get( index );

            if( !textBlockLines.get( index ) && lines.get( index - 1 ).isBlank()
                    && !textBlockLines.get( index - 1 )
                    && (isCaseLabel( line ) || line.stripLeading().startsWith( "." ))
            ) {
                lines.remove( index - 1 );
                textBlockLines.remove( index - 1 );
                index--;
            }
        }
    }

    private static void addBlankLinesAroundBlocks( List<String > lines, List<Boolean > textBlockLines ) {
        for( int index = lines.size() - 1; index > 0; index-- ) {
            String line = lines.get( index );
            String previousLine = lines.get( index - 1 );

            if( previousLine.isBlank() || textBlockLines.get( index ) || textBlockLines.get( index - 1 ) ) {
                continue;
            }

            if( (endsStatement( previousLine ) && startsControlBlock( line ))
                    || (isStandaloneClosingBrace( previousLine ) && startsStatementAfterBlock( line ))
            ) {
                lines.add( index, "" );
                textBlockLines.add( index, false );
            }
        }
    }

    private static String getNextNonBlankLine( List<String > lines, int startIndex ) {
        for( int index = startIndex; index < lines.size(); index++ ) {
            if( !lines.get( index ).isBlank() ) {
                return lines.get( index );
            }
        }

        return "";
    }

    private static boolean isDeclarationOrAnnotation( String line ) {
        String lStrippedLine = line.stripLeading();
        return lStrippedLine.startsWith( "@" )
                || lStrippedLine.matches( "(?:public\\s+)?(?:class|interface|enum|record)\\b.*" );
    }

    private static boolean isCaseLabel( String line ) {
        return line.stripLeading().matches( "(?:case\\b.*|default\\b.*)" );
    }

    private static boolean endsStatement( String line ) {
        String lStrippedLine = line.stripTrailing();
        return lStrippedLine.endsWith( ";" )
                && !lStrippedLine.stripLeading().startsWith( "//" )
                && !lStrippedLine.stripLeading().endsWith( "{" );
    }

    private static boolean startsControlBlock( String line ) {
        return line.stripLeading().matches( "(?:if|for|while|switch|try)\\b.*" );
    }

    private static boolean isStandaloneClosingBrace( String line ) {
        return line.matches( "[ \\t]*}[ \\t]*" );
    }

    private static boolean startsStatementAfterBlock( String line ) {
        String lStrippedLine = line.stripLeading();
        return !lStrippedLine.isEmpty()
                && !lStrippedLine.startsWith( "}" )
                && !lStrippedLine.startsWith( "else" )
                && !lStrippedLine.startsWith( "catch" )
                && !lStrippedLine.startsWith( "finally" )
                && !isCaseLabel( line )
                && !lStrippedLine.startsWith( ")" )
                && !lStrippedLine.startsWith( "]" )
                && !lStrippedLine.startsWith( "//" )
                && !lStrippedLine.startsWith( "/*" )
                && !lStrippedLine.startsWith( "*" )
                && !lStrippedLine.startsWith( "\"" )
                && !lStrippedLine.startsWith( "'" );
    }
}
