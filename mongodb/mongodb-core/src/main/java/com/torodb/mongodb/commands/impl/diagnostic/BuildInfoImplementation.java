/*
 * ToroDB - ToroDB-poc: MongoDB Core
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
package com.torodb.mongodb.commands.impl.diagnostic;

import javax.inject.Inject;

import com.eightkdata.mongowp.MongoVersion;
import com.eightkdata.mongowp.Status;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.diagnostic.BuildInfoCommand.BuildInfoResult;
import com.eightkdata.mongowp.server.api.Command;
import com.eightkdata.mongowp.server.api.Request;
import com.eightkdata.mongowp.server.api.tools.Empty;
import com.torodb.core.BuildProperties;
import com.torodb.mongodb.commands.impl.ConnectionTorodbCommandImpl;
import com.torodb.mongodb.core.MongoLayerConstants;
import com.torodb.mongodb.core.MongodConnection;

public class BuildInfoImplementation  extends ConnectionTorodbCommandImpl<Empty, BuildInfoResult>{

    private final BuildProperties buildProperties;
    
    @Inject
    public BuildInfoImplementation(BuildProperties buildProperties) {
        super();
        this.buildProperties = buildProperties;
    }

    @Override
    public Status<BuildInfoResult> apply(Request req, Command<? super Empty, ? super BuildInfoResult> command,
            Empty arg, MongodConnection context) {
        return Status.<BuildInfoResult>ok(new BuildInfoResult(
                MongoVersion.V3_0, 0, 
                buildProperties.getGitCommitId(),
                buildProperties.getOsName() + " " + buildProperties.getOsVersion() + " " + buildProperties.getOsArch(), 
                null, 
                null, 
                null, 
                null, 
                "amd64".equals(buildProperties.getOsArch()) ? 64 : 32, false, 
                MongoLayerConstants.MAX_BSON_DOCUMENT_SIZE));
    }
    
}
