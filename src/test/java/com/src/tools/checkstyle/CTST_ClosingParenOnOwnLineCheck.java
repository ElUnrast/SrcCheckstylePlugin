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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the enforcement of multiline parenthesis closing lines.
 */
public class CTST_ClosingParenOnOwnLineCheck {
    @Test
    public void testAllowsClosingParenthesesOnOwnLineWithOpeningIndentation() throws Exception {
        assertEquals(
                0,
                getViolationCount( """
                class C_Example {
                    void test() {
                        consume(
                                nested(
                                        \"value\"
                                )
                        );
                    }
                }
                """
                )
        );
    }

    @Test
    public void testAllowsLambdaBlockBeforeClosingParenthesis() throws Exception {
        assertEquals(
                0,
                getViolationCount( """
                class C_Example {
                    void test() {
                        consume( value -> {
                            use( value );
                        } );
                    }
                }
                """
                )
        );
    }

    @Test
    public void testAllowsLambdaBlockBeforeMultipleClosingParentheses() throws Exception {
        assertEquals(
                0,
                getViolationCount( """
                class C_Example {
                    void test() {
                        consume( nested( value -> {
                            use( value );
                        } ) );
                    }
                }
                """
                )
        );
    }

    @Test
    public void testRejectsClosingParenthesisAfterAnArgument() throws Exception {
        assertEquals(
                1,
                getViolationCount( """
                class C_Example {
                    void test() {
                        consume(
                                \"value\");
                    }
                }
                """
                )
        );
    }

    private int getViolationCount( String source ) throws Exception {
        Path sourceFile = Files.createTempFile( "C_ClosingParenOnOwnLineCheck", ".java" );

        try {
            Files.writeString( sourceFile, source );
            return runCheck( sourceFile );
        }
        finally {
            Files.deleteIfExists( sourceFile );
        }
    }

    private int runCheck( Path sourceFile ) throws Exception {
        Checker checker = new Checker();

        try {
            checker.setModuleClassLoader( getClass().getClassLoader() );
            checker.configure( createConfiguration() );
            return checker.process( List.of( sourceFile.toFile() ) );
        }
        finally {
            checker.destroy();
        }
    }

    private DefaultConfiguration createConfiguration() {
        DefaultConfiguration checker = new DefaultConfiguration( "Checker" );
        DefaultConfiguration treeWalker = new DefaultConfiguration( "TreeWalker" );
        DefaultConfiguration check = new DefaultConfiguration( C_ClosingParenOnOwnLineCheck.class.getName() );

        treeWalker.addChild( check );
        checker.addChild( treeWalker );
        return checker;
    }
}
