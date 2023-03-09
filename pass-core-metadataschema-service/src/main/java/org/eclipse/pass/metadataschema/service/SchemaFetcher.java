/*
 *
 * Copyright 2023 Johns Hopkins University
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.eclipse.pass.metadataschema.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.pass.object.PassClient;
import org.eclipse.pass.object.PassClientResult;
import org.eclipse.pass.object.PassClientSelector;
import org.eclipse.pass.object.RSQL;
import org.eclipse.pass.object.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.eclipse.pass.object.model.Repository;

/**
 * Fetches the schemas from a list of repository URIs and creates a
 * corresponding list of SchemaInstance objects
 */
public class SchemaFetcher {

    private PassClient passClient;
    private static final Logger LOG = LoggerFactory.getLogger(PassSchemaServiceController.class);

    public SchemaFetcher(PassClient client) {
        this.passClient = client;
    }

    /**
     * Get all SchemaInstance objects corresponding to the repository URIs
     *
     * @return List<SchemaInstance> ArrayList of relevant SchemaInstance objects
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws URISyntaxException
     */
    List<JsonNode> getSchemas(List<String> repository_uris)
            throws IllegalArgumentException, URISyntaxException, IOException {

        List<JsonNode> schemas = new ArrayList<JsonNode>();
        List<SchemaInstance> schema_instances = new ArrayList<SchemaInstance>();

        for (String repository_uri : repository_uris) {
            List<JsonNode> repository_schemas;
            repository_schemas = getRepositorySchemas(repository_uri);
            for (JsonNode schema : repository_schemas) {
                if (!schemas.contains(schema)) {
                    schemas.add(schema);
                }
            }
        }
        // dereference each of the schemas
        for (int i = 0; i < schemas.size(); i++) {
            SchemaInstance s = new SchemaInstance(schemas.get(i));
            s.dereference(s.getSchema(), "");
            schema_instances.add(s);
        }

        Collections.sort(schema_instances);

        schemas = new ArrayList<JsonNode>();
        for (SchemaInstance s : schema_instances) {
            schemas.add(s.getSchema());
        }

        return schemas;
    }

    /**
     * Gets the Repository PASS entity at the URI and generates the corresponding
     * SchemaInstance objects
     *
     * @return List<SchemaInstance> schemas from the repository
     * @throws IOException
     * @throws URISyntaxException
     * @throws IllegalArgumentException
     */
   List<JsonNode> getRepositorySchemas(String repositoryUri)
            throws URISyntaxException, IllegalArgumentException, IOException {
        //URI uriRepo;
        LOG.info("SchemaFetcher - getRepositorySchemas - Repository URI: " + repositoryUri);
        Repository repo = null;
        PassClientResult<Repository> result = null;
        List<JsonNode> repository_schemas = new ArrayList<JsonNode>();
        String filter = RSQL.equals("url", repositoryUri);
        LOG.info("SchemaFetcher - getRepositorySchemas - Repository filter: " + filter);
        try {
            PassClientSelector selector = new PassClientSelector<>(Repository.class, 0, 100, filter,null);
            result = passClient.selectObjects(selector);
            LOG.info("SchemaFetcher - getRepositorySchemas - Repository after result");
        } catch (NullPointerException e){
            throw new IOException("Repository not found at " + repositoryUri);
        }
        if(result == null){
            LOG.info("result is null");
        }
        LOG.info("SchemaFetcher - getRepositorySchemas - Repository result.getTotal(): " + result.getTotal());
        if (result.getTotal() == 1) {
            repo = result.getObjects().get(0);
        } else if (result.getTotal() > 1) {
            throw new IOException("Multiple repositories found at " + repositoryUri);
        } else if (result.getTotal() == 0) {
            throw new IOException("No repository found at " + repositoryUri);
        }
        LOG.info("SchemaFetcher - getRepositorySchemas - Repository getName: " + repo.getName());
        List<URI> schema_uris = repo.getSchemas();
        for (URI schema_uri : schema_uris) {
            LOG.info("SchemaFetcher - Schema URI: " + schema_uri);
            repository_schemas.add(getSchemaFromUri(schema_uri));
        }
        LOG.info("SchemaFetcher - Repository schemas: " + repository_schemas);
        return repository_schemas;
    }

    /**
     * Gets the schema at the URI and creates a corresponding SchemaInstance object
     *
     * @return SchemaInstance schema at URI
     * @throws IOException
     * @throws DatabindException
     * @throws StreamReadException
     */
    JsonNode getSchemaFromUri(URI schema_uri) throws IOException, IllegalArgumentException {
        LOG.info("SchemaFetcher - Schema URI: " + schema_uri);
        // Given the schema's $id url, go to the corresponding local json file
        // by loading it as a resource stream based on the last 2 parts of the $id
        // Create a SchemaInstance object from the json file and return it
        String path = schema_uri.getPath();
        String[] path_segments = path.split("/pass-metadata-schemas");
        String path_to_schema = path_segments[path_segments.length - 1];
        JsonNode schema = null;
        LOG.info("SchemaFetcher - Path to schema: " + path_to_schema);
        schema = getLocalSchema(path_to_schema);
        return schema;
    }

    public static JsonNode getLocalSchema(String path) throws IOException, IllegalArgumentException {
        LOG.info("SchemaFetcher - getLocalSchema - schema path: " + path);
        InputStream schema_json = SchemaFetcher.class.getResourceAsStream(path);
        LOG.info("SchemaFetcher - after schema_json");
        ObjectMapper objmapper = new ObjectMapper();
        LOG.info("SchemaFetcher - after objmapper");
        JsonNode schema_obj = objmapper.readTree(schema_json);
        LOG.info("SchemaFetcher - after schema_obj");
        LOG.info("SchemaFetcher - Schema: " + schema_obj);
        return schema_obj;
    }
}