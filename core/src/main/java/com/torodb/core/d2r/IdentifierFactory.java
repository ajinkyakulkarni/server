package com.torodb.core.d2r;

import javax.annotation.Nonnull;

import com.torodb.core.TableRef;
import com.torodb.core.transaction.metainf.FieldType;
import com.torodb.core.transaction.metainf.MetaDatabase;
import com.torodb.core.transaction.metainf.MetaDocPart;
import com.torodb.core.transaction.metainf.MetaField;
import com.torodb.core.transaction.metainf.MetaSnapshot;

public interface IdentifierFactory {

    @Nonnull String toDatabaseIdentifier(@Nonnull MetaSnapshot metaSnapshot, @Nonnull String database);

    @Nonnull String toCollectionIdentifier(@Nonnull MetaSnapshot metaSnapshot, @Nonnull String database, @Nonnull String collection);

    @Nonnull String toDocPartIdentifier(@Nonnull MetaDatabase metaDatabase, @Nonnull String collection, @Nonnull TableRef tableRef);

    @Nonnull String toFieldIdentifier(@Nonnull MetaDocPart metaDocPart, @Nonnull String field, @Nonnull FieldType fieldType);

    @Nonnull String toFieldIdentifierForScalar(@Nonnull FieldType fieldType);

    @Nonnull String toIndexIdentifier(@Nonnull MetaDatabase metaSnapshot, @Nonnull String indexName, @Nonnull Iterable<String> identifiers);
}