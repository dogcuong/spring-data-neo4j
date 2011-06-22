/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.rest.support;

import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.core.Property;
import org.springframework.data.graph.neo4j.rest.support.index.RestIndexManager;
import org.springframework.data.graph.neo4j.support.query.QueryEngine;

import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.Map;

public class RestGraphDatabase implements GraphDatabaseService, GraphDatabase {

    private RestRequest restRequest;
    private long propertyRefetchTimeInMillis = 1000;


    public RestGraphDatabase( URI uri ) {
        restRequest = new RestRequest( uri );
    }

    public RestGraphDatabase( URI uri, String user, String password ) {
        restRequest = new RestRequest( uri, user, password );
    }

    @Override
    public Node getNodeById(long id) {
        ClientResponse response = restRequest.get("node/" + id);
        if ( restRequest.statusIs(response, Status.NOT_FOUND) ) {
            throw new NotFoundException( "" + id );
        }
        return new RestNode( restRequest.toMap(response), this );
    }

    @Override
    public Node createNode(Property... props) {
        ClientResponse response = restRequest.post("node", JsonHelper.createJsonFrom( Property.toMap(props) ));
        if ( restRequest.statusOtherThan(response, Status.CREATED) ) {
            throw new RuntimeException( "" + response.getStatus() );
        }
        return new RestNode( response.getLocation(), this );
    }

    @Override
    public Relationship getRelationshipById(long id) {
        ClientResponse response = restRequest.get("relationship/" + id);
        if ( restRequest.statusIs( response, Status.NOT_FOUND ) ) {
            throw new NotFoundException( "" + id );
        }
        return new RestRelationship( restRequest.toMap( response ), this );
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Property... props) {
        return RestRelationship.create((RestNode)startNode,(RestNode)endNode,type,props);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        final RestIndexManager index = index();
        if (index.existsForNodes(indexName)) return (Index<T>) index.forNodes(indexName);
        if (index.existsForRelationships(indexName)) return (Index<T>) index.forRelationships(indexName);
        throw new IllegalArgumentException("Index "+indexName+" does not yet exist");
    }

    @Override
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
        if (Node.class.isAssignableFrom(type)) return (Index<T>) index().forNodes(indexName);
        if (Relationship.class.isAssignableFrom(type)) return (Index<T>) index().forRelationships(indexName);
        throw new IllegalArgumentException("Required Node or Relationship types to create index, got "+type);
    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return new RestTraversal();
    }

    @Override
    public QueryEngine queryEngineFor(QueryEngine.Type type) {
        return new RestQueryEngine(restRequest);
    }

    public RestIndexManager index() {
        return new RestIndexManager( restRequest, this );
    }

    @Override
    public Node getReferenceNode() {
        Map<?, ?> map = restRequest.toMap( restRequest.get( "" ) );
        return new RestNode( (String) map.get( "reference_node" ), this );
    }

    public RestRequest getRestRequest() {
        return restRequest;
    }

    public long getPropertyRefetchTimeInMillis() {
        return propertyRefetchTimeInMillis;
	}

    @Override
    public Node createNode() {
        return createNode((Property[])null);
    }

    @Override
    public Iterable<Node> getAllNodes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public Transaction beginTx() {
        return new Transaction() {
            @Override
            public void failure() {
            }

            @Override
            public void success() {
            }

            @Override
            public void finish() {
            }
        };
    }

    @Override
    public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
        return handler;
    }

    @Override
    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
        return handler;
    }

    @Override
    public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
        return handler;
    }

    @Override
    public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
        return handler;
    }
}
