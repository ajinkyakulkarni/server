/*
 * ToroDB - ToroDB-poc: Benchmarks
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
package com.torodb.backend.util;

import com.torodb.core.transaction.metainf.MetainfoRepository.MergerStage;
import com.torodb.core.transaction.metainf.MetainfoRepository.SnapshotStage;
import com.torodb.core.transaction.metainf.MutableMetaSnapshot;
import com.torodb.core.transaction.metainf.UnmergeableException;
import com.torodb.metainfo.cache.mvcc.MvccMetainfoRepository;
import java.util.function.Consumer;

public class MetaInfoOperation {

	public static void executeMetaOperation(MvccMetainfoRepository mvcc, Consumer<MutableMetaSnapshot> consumer){
		MutableMetaSnapshot mutableSnapshot;
		try (SnapshotStage snapshot = mvcc.startSnapshotStage()) {
			mutableSnapshot = snapshot.createMutableSnapshot();
		}
		
		consumer.accept(mutableSnapshot);
		
		try (MergerStage mergeStage = mvcc.startMerge(mutableSnapshot)) {
            mergeStage.commit();
        } catch (UnmergeableException ex) {
            throw new AssertionError("Unmergeable changes", ex);
        }
	}

}
