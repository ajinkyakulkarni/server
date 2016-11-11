/*
 * ToroDB - ToroDB-poc: Backend PostgreSQL
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
package com.torodb.backend.postgresql.tables.records;

import com.torodb.backend.postgresql.tables.PostgreSQLKvTable;
import com.torodb.backend.tables.records.KvRecord;

public class PostgreSQLKvRecord extends KvRecord {

    private static final long serialVersionUID = -7220623531622958067L;

    public PostgreSQLKvRecord() {
		super(PostgreSQLKvTable.KV);
	}

	public PostgreSQLKvRecord(String name, String identifier) {
		super(PostgreSQLKvTable.KV);
		
		values(name, identifier);
	}

    @Override
    public PostgreSQLKvRecord values(String key, String value) {
        setKey(key);
        setValue(value);
        return this;
    }
}
