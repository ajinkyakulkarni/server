/*
 * MongoWP - KVDocument: Core
 * Copyright © 2014 8Kdata Technology (www.8kdata.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.torodb.kvdocument.types;

/**
 *
 */
public class NullType implements KVType {
    private static final long serialVersionUID = 1L;

    public static final NullType INSTANCE = new NullType();
    
    private NullType() {}

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass().equals(this.getClass());
    }

    @Override
    public int hashCode() {
        int hash = 23;
        return hash;
    }
    
    @Override
    public <Result, Arg> Result accept(KVTypeVisitor<Result, Arg> visitor, Arg arg) {
        return visitor.visit(this, arg);
    }
}
