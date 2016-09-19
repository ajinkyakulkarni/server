/*
 *     This file is part of ToroDB.
 *
 *     ToroDB is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ToroDB is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with ToroDB. If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2014, 8Kdata Technology
 *     
 */

package com.torodb.mongodb.repl;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jooq.lambda.fi.util.function.CheckedFunction;

import com.eightkdata.mongowp.bson.BsonDocument;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.admin.CreateCollectionCommand;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.admin.CreateIndexesCommand;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.admin.DropCollectionCommand;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.admin.RenameCollectionCommand;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.general.DeleteCommand;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.general.InsertCommand;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.general.UpdateCommand;
import com.eightkdata.mongowp.server.api.Command;
import com.eightkdata.mongowp.server.api.oplog.DbCmdOplogOperation;
import com.eightkdata.mongowp.server.api.oplog.DbOplogOperation;
import com.eightkdata.mongowp.server.api.oplog.DeleteOplogOperation;
import com.eightkdata.mongowp.server.api.oplog.InsertOplogOperation;
import com.eightkdata.mongowp.server.api.oplog.NoopOplogOperation;
import com.eightkdata.mongowp.server.api.oplog.OplogOperation;
import com.eightkdata.mongowp.server.api.oplog.OplogOperationVisitor;
import com.eightkdata.mongowp.server.api.oplog.UpdateOplogOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.torodb.core.exceptions.SystemException;
import com.torodb.mongodb.repl.oplogreplier.fetcher.FilteredOplogFetcher;
import com.torodb.mongodb.repl.oplogreplier.fetcher.OplogFetcher;

public class ReplicationFilters {

    private final ImmutableMap<Pattern, ImmutableList<Pattern>> whitelist;
    private final ImmutableMap<Pattern, ImmutableList<Pattern>> blacklist;
    private final DatabasePredicate databasePredicate = new DatabasePredicate();
    private final CollectionPredicate collectionPredicate = new CollectionPredicate();
    private final OplogOperationPredicate oplogOperationPredicate = new OplogOperationPredicate(); 
    
    public ReplicationFilters(ImmutableMap<Pattern, ImmutableList<Pattern>> whitelist,
            ImmutableMap<Pattern, ImmutableList<Pattern>> blacklist) {
        super();
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }
    
    public Predicate<String> getDatabasePredicate() {
        return databasePredicate;
    }
    
    public BiPredicate<String, String> getCollectionPredicate() {
        return collectionPredicate;
    }

    public Predicate<OplogOperation> getOperationPredicate() {
        return oplogOperationPredicate;
    }

    public OplogFetcher filterOplogFetcher(OplogFetcher originalFetcher) {
        return new FilteredOplogFetcher(oplogOperationPredicate, originalFetcher);
    }
    
    private boolean databaseWhiteFilter(String database) {
        if (whitelist.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<Pattern, ImmutableList<Pattern>> filterEntry : whitelist.entrySet()) {
            Matcher databaseMatcher = filterEntry.getKey().matcher(database);
            if (databaseMatcher.matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean databaseBlackFilter(String database) {
        if (blacklist.isEmpty()) {
            return true;
        }

        for (Map.Entry<Pattern, ImmutableList<Pattern>> filterEntry : blacklist.entrySet()) {
            Matcher databaseMatcher = filterEntry.getKey().matcher(database);
            if (databaseMatcher.matches()) {
                if (filterEntry.getValue().isEmpty()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean collectionWhiteFilter(String database, String collection) {
        if (whitelist.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<Pattern, ImmutableList<Pattern>> filterEntry : whitelist.entrySet()) {
            Matcher databaseMatcher = filterEntry.getKey().matcher(database);
            if (databaseMatcher.matches()) {
                if (filterEntry.getValue().isEmpty()) {
                    return true;
                }
                
                for (Pattern collectionPattern : filterEntry.getValue()) {
                    Matcher collectionMatcher = collectionPattern.matcher(collection);
                    if (collectionMatcher.matches()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean collectionBlackFilter(String database, String collection) {
        if (blacklist.isEmpty()) {
            return true;
        }

        for (Map.Entry<Pattern, ImmutableList<Pattern>> filterEntry : blacklist.entrySet()) {
            Matcher databaseMatcher = filterEntry.getKey().matcher(database);
            if (databaseMatcher.matches()) {
                if (filterEntry.getValue().isEmpty()) {
                    return false;
                }
                
                for (Pattern collectionPattern : filterEntry.getValue()) {
                    Matcher collectionMatcher = collectionPattern.matcher(collection);
                    if (collectionMatcher.matches()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private class DatabasePredicate implements Predicate<String> {
        @Override
        public boolean test(String database) {
            return databaseWhiteFilter(database) &&
                    databaseBlackFilter(database);
        }
        
    }
    
    private class CollectionPredicate implements BiPredicate<String, String> {
        @Override
        public boolean test(String database, String collection) {
            return collectionWhiteFilter(database, collection) &&
                    collectionBlackFilter(database, collection);
        }
        
    }
    
    private static final ImmutableMap<Command<?, ?>, CheckedFunction<BsonDocument, String>> collectionRelatedCommands = 
        ImmutableMap.<Command<?, ?>, CheckedFunction<BsonDocument, String>>builder()
            .put(CreateCollectionCommand.INSTANCE, d -> CreateCollectionCommand.INSTANCE.unmarshallArg(d).getCollection())
            .put(CreateIndexesCommand.INSTANCE, d -> CreateIndexesCommand.INSTANCE.unmarshallArg(d).getCollection())
            .put(DropCollectionCommand.INSTANCE, d -> DropCollectionCommand.INSTANCE.unmarshallArg(d).getCollection())
            .put(RenameCollectionCommand.INSTANCE, d -> RenameCollectionCommand.INSTANCE.unmarshallArg(d).getFromCollection())
            .put(DeleteCommand.INSTANCE, d -> DeleteCommand.INSTANCE.unmarshallArg(d).getCollection())
            .put(InsertCommand.INSTANCE, d -> InsertCommand.INSTANCE.unmarshallArg(d).getCollection())
            .put(UpdateCommand.INSTANCE, d -> UpdateCommand.INSTANCE.unmarshallArg(d).getCollection())
            .build();
    
    private class OplogOperationPredicate implements OplogOperationVisitor<Boolean, Void>, Predicate<OplogOperation> {

        @Override
        public Boolean visit(DbCmdOplogOperation op, Void arg) {
            Optional<Command<?, ?>> collectionRelatedCommand = collectionRelatedCommands.keySet().stream().filter(
                    c -> op.getCommandName().isPresent() && 
                        c.getCommandName().equals(op.getCommandName().get())).findFirst();
            if (collectionRelatedCommand.isPresent()) {
                try {
                    assert op.getRequest() != null;
                    
                    String collection = collectionRelatedCommands.get(collectionRelatedCommand.get())
                            .apply(op.getRequest());
                    return collectionPredicate.test(op.getDatabase(), collection);
                } catch (Throwable e) {
                    throw new SystemException("Error while parsing argument for command " + op.getCommandName(), e);
                }
            }
            return databasePredicate.test(op.getDatabase());
        }

        @Override
        public Boolean visit(DbOplogOperation op, Void arg) {
            return databasePredicate.test(op.getDatabase());
        }

        @Override
        public Boolean visit(DeleteOplogOperation op, Void arg) {
            return collectionPredicate.test(op.getDatabase(), op.getCollection());
        }

        @Override
        public Boolean visit(InsertOplogOperation op, Void arg) {
            return collectionPredicate.test(op.getDatabase(), op.getCollection());
        }

        @Override
        public Boolean visit(NoopOplogOperation op, Void arg) {
            return databasePredicate.test(op.getDatabase());
        }

        @Override
        public Boolean visit(UpdateOplogOperation op, Void arg) {
            return collectionPredicate.test(op.getDatabase(), op.getCollection());
        }

        @Override
        public boolean test(OplogOperation t) {
            return t.accept(this, null);
        }
        
    }
}
