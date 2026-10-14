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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/** Applies the safe source corrections for the SRC Checkstyle rules. */
public class C_JavaStyleFixer {
    /** The stages only change whitespace and parentheses; this removes both to compare the remaining content. */
    private static final Pattern k_ignoredCharacters = Pattern.compile( "[\\s()]" );
    private static final List<C_NamedStage > k_stages = List.of(
            new C_NamedStage( "spacing", new C_AstSpacingFixer() ),
            new C_NamedStage( "parentheses", new C_ParenthesisFixer() ),
            new C_NamedStage( "blocks", new C_BlockSplitFixer() ),
            new C_NamedStage( "tabs", new C_TabFixer() ),
            new C_NamedStage( "tokens", new C_LexicalFixer() ),
            new C_NamedStage( "closing parentheses", new C_ClosingParenFixer() ),
            new C_NamedStage( "blank lines", new C_BlankLineFixer() )
    );

    public static void main( String[] args ) throws IOException, CheckstyleException {
        if( args.length != 1 ) {
            throw new IllegalArgumentException( "Usage: C_JavaStyleFixer <Java file>" );
        }

        Path file = Path.of( args[0] );
        String source = Files.readString( file, StandardCharsets.UTF_8 );
        String fixedSource = fix( source );

        if( !source.equals( fixedSource ) ) {
            Files.writeString( file, fixedSource, StandardCharsets.UTF_8 );
            System.out.println( "Corrected Checkstyle formatting in " + file );
        }
        else {
            System.out.println( "No supported Checkstyle corrections needed in " + file );
        }
    }

    static String fix( String source ) throws CheckstyleException {
        String result = source;

        for( C_NamedStage stage : k_stages ) {
            result = verify( stage.m_name, result, stage.m_stage.apply( result ) );
        }

        return result;
    }

    /** Returns the corrected source, or the original one if the stage broke the code or changed more than layout. */
    private static String verify( String name, String original, String corrected ) {
        if( corrected.equals( original ) ) {
            return original;
        }

        try {
            new C_SourceText( corrected ).parse();
        }
        catch( CheckstyleException exception ) {
            System.err.println( "Skipped the " + name + " corrections: result does not parse." );
            return original;
        }

        if( !k_ignoredCharacters.matcher( corrected ).replaceAll( "" )
                .equals( k_ignoredCharacters.matcher( original ).replaceAll( "" ) )
        ) {
            System.err.println( "Skipped the " + name + " corrections: unexpected content change." );
            return original;
        }

        return corrected;
    }

    private static final class C_NamedStage {
        private final String m_name;
        private final C_StyleStage m_stage;

        C_NamedStage( String name, C_StyleStage stage ) {
            m_name = name;
            m_stage = stage;
        }
    }
}
