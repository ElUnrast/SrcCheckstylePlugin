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

/** A replacement of a source range; insertions have a delete length of 0. */
final class C_SourceEdit {
    private final int m_offset;
    private final int m_deleteLength;
    private final String m_text;

    private C_SourceEdit( int offset, int deleteLength, String text ) {
        m_offset = offset;
        m_deleteLength = deleteLength;
        m_text = text;
    }

    static C_SourceEdit insert( int offset, String text ) {
        return new C_SourceEdit( offset, 0, text );
    }

    static C_SourceEdit delete( int offset, int length ) {
        return new C_SourceEdit( offset, length, "" );
    }

    static C_SourceEdit replace( int offset, int length, String text ) {
        return new C_SourceEdit( offset, length, text );
    }

    /**
     * Applies the edits from the end of the source to its start, so offsets stay valid. Of overlapping edits only
     * the one that comes first in the list is applied; edits at the same offset keep their list order.
     */
    static String apply( String source, List<C_SourceEdit > edits ) {
        edits.sort( ( first, second ) -> Integer.compare( second.m_offset, first.m_offset ) );
        StringBuilder result = new StringBuilder( source );
        int lastOffset = Integer.MAX_VALUE;

        for( C_SourceEdit edit : edits ) {
            if( (edit.m_offset + edit.m_deleteLength) <= lastOffset ) {
                result.replace( edit.m_offset, edit.m_offset + edit.m_deleteLength, edit.m_text );
                lastOffset = edit.m_offset;
            }
        }

        return result.toString();
    }
}
