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

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

/** Base of the stages that are decided by the syntax tree: subclasses inspect each token and add edits. */
abstract class C_AstFixer implements C_StyleStage {
    @Override
    public String apply( String source ) throws CheckstyleException {
        C_SourceText text = new C_SourceText( source );
        List<C_SourceEdit > edits = new ArrayList<>();
        visitTree( text.parse(), text, edits );
        return C_SourceEdit.apply( source, edits );
    }

    protected abstract void visit( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits );

    private void visitTree( DetailAST ast, C_SourceText text, List<C_SourceEdit > edits ) {
        visit( ast, text, edits );

        for( DetailAST child = ast.getFirstChild(); child != null; child = child.getNextSibling() ) {
            visitTree( child, text, edits );
        }
    }
}
