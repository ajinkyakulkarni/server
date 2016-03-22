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

package com.toro.torod.connection.update;

import com.torodb.torod.core.config.DocumentBuilderFactory;
import com.torodb.torod.core.connection.UpdateResponse;
import com.torodb.torod.core.language.update.*;
import com.torodb.torod.core.language.update.utils.UpdateActionVisitor;
import com.torodb.torod.core.subdocument.ToroDocument;
import javax.annotation.Nullable;

/**
 *
 */
public class Updator {

    private static final MyVisitor VISITOR = new MyVisitor();
    
    @Nullable
    public static ToroDocument update(
            ToroDocument candidate,
            UpdateAction updateAction,
            UpdateResponse.Builder responseBuilder,
            DocumentBuilderFactory documentBuilderFactory
    ) {
        KVDocumentBuilder copyBuilder = KVDocumentBuilder.from(
                candidate.getRoot()
        );

        Boolean isUpdated = updateAction.accept(VISITOR, copyBuilder);

        if (isUpdated == null || !isUpdated) {
            return null;
        }
        
        responseBuilder.incrementModified(1);
        
        ToroDocument.DocumentBuilder newDocBuilder = documentBuilderFactory.newDocBuilder();
        newDocBuilder.setRoot(candidate.getRoot());
        newDocBuilder.setRoot(copyBuilder.build());
        
        return newDocBuilder.build();
    }

    private static class MyVisitor implements UpdateActionVisitor<Boolean, KVDocumentBuilder> {

        @Override
        public Boolean visit(CompositeUpdateAction action, KVDocumentBuilder arg) {
            boolean result = false;
            for (UpdateAction subAction : action.getActions().values()) {
                result |= subAction.accept(this, arg);
            }
            return result;
        }

        @Override
        public Boolean visit(IncrementUpdateAction action, KVDocumentBuilder builder) {
            return IncrementUpdateActionExecutor.increment(
                    new ObjectBuilderCallback(builder),
                    action.getModifiedField(),
                    action.getDelta()
            );
        }

        @Override
        public Boolean visit(MultiplyUpdateAction action, KVDocumentBuilder builder) {
            return MultiplyUpdateActionExecutor.multiply(
                    new ObjectBuilderCallback(builder),
                    action.getModifiedField(),
                    action.getMultiplier()
            );
        }

        @Override
        public Boolean visit(
                MoveUpdateAction action,
                KVDocumentBuilder builder
        ) {
            return MoveUpdateActionExecutor.move(
                    new ObjectBuilderCallback(builder),
                    action.getModifiedField(),
                    action.getNewField()
            );
        }

        @Override
        public Boolean visit(SetCurrentDateUpdateAction action, KVDocumentBuilder arg) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Boolean visit(SetFieldUpdateAction action, KVDocumentBuilder builder) {
            return SetFieldUpdateActionExecutor.set(
                    new ObjectBuilderCallback(builder),
                    action.getModifiedField(),
                    action.getNewValue()
            );
        }

        @Override
        public Boolean visit(SetDocumentUpdateAction action, KVDocumentBuilder arg) {
            return SetDocumentUpdateActionExecutor.set(
                    arg,
                    action.getNewValue()
            );
        }

        @Override
        public Boolean visit(UnsetFieldUpdateAction action, KVDocumentBuilder builder) {
            return UnsetUpdateActionExecutor.unset(
                    new ObjectBuilderCallback(builder),
                    action.getModifiedField()
            );
        }
    }
}
