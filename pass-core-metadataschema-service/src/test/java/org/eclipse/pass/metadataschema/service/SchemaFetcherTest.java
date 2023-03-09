package org.eclipse.pass.metadataschema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.pass.object.PassClient;
import org.eclipse.pass.object.PassClientResult;
import org.eclipse.pass.object.PassClientSelector;
import org.eclipse.pass.object.RSQL;
import org.eclipse.pass.object.model.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class SchemaFetcherTest {

    private static final Logger LOGGER = Logger.getLogger(SchemaFetcherTest.class.getName());
    private PassClient passClientMock;
    private Repository repositoryMock;
    private Repository repositoryMock2;
    private PassClientResult passClientResultMock;
    private PassClientResult passClientResultMock2;
    private SchemaFetcher s;
    private ObjectMapper map;

    @BeforeEach
    void setup() throws Exception {
        passClientMock = Mockito.mock(PassClient.class);
        repositoryMock = Mockito.mock(Repository.class);
        repositoryMock2 = Mockito.mock(Repository.class);
        passClientResultMock = Mockito.mock(PassClientResult.class);
        passClientResultMock2 = Mockito.mock(PassClientResult.class);
        s = new SchemaFetcher(passClientMock);
        map = new ObjectMapper();
    }

    @Test
    void getSchemaFromUriTest() throws Exception {
        URI uri = new URI("https://example.com/pass-metadata-schemas/example/schemas/schema1.json");
        String expectedJsonString = "{\r\n" + "    \"$schema\": \"http://example.org/example/schemas/schema\",\r\n"
                + "    \"$id\": \"http://example.org/example/schemas/foo\",\r\n" + "    \"title\": \"foo\",\r\n"
                + "    \"description\": \"foo schema\",\r\n" + "    \"$comment\": \"one\",\r\n"
                + "    \"a\": \"1\",\r\n" + "    \"x\": {\r\n" + "        \"title\": \"X\",\r\n"
                + "        \"description\": \"a letter\",\r\n" + "        \"$comment\": \"displays good\",\r\n"
                + "        \"type\": \"letter\"\r\n" + "    },\r\n" + "    \"array\": [\"a\", \"b\", \"c\"],\r\n"
                + "    \"aa\": \"b\",\r\n" + "    \"cc\": [\"d\", \"e\"]\r\n" + "}";
        JsonNode expected = map.readTree(expectedJsonString);
        assertEquals(expected, s.getSchemaFromUri(uri));
    }

    @Test
    @DisplayName("Test: Test getRepositorySchemasTest() with two schemas. Should return a list of  both schemas.")
    void getRepositorySchemasTest() throws Exception {
        when(passClientMock.selectObjects(Mockito.any(PassClientSelector.class))).thenReturn(passClientResultMock);
        when(passClientResultMock.getObjects()).thenReturn(Collections.singletonList(repositoryMock));
        when(passClientResultMock.getTotal()).thenReturn(1L);
        List<URI> r1_schemas_list = Arrays.asList(new URI("/example/schemas/schema1.json"),
                new URI("/example/schemas/schema2.json"));
        when(repositoryMock.getSchemas()).thenReturn(r1_schemas_list);

        String expectedJsonSchema1 = "{\r\n" + "    \"$schema\": \"http://example.org/example/schemas/schema\",\r\n"
                + "    \"$id\": \"http://example.org/example/schemas/foo\",\r\n" + "    \"title\": \"foo\",\r\n"
                + "    \"description\": \"foo schema\",\r\n" + "    \"$comment\": \"one\",\r\n"
                + "    \"a\": \"1\",\r\n" + "    \"x\": {\r\n" + "        \"title\": \"X\",\r\n"
                + "        \"description\": \"a letter\",\r\n" + "        \"$comment\": \"displays good\",\r\n"
                + "        \"type\": \"letter\"\r\n" + "    },\r\n" + "    \"array\": [\"a\", \"b\", \"c\"],\r\n"
                + "    \"aa\": \"b\",\r\n" + "    \"cc\": [\"d\", \"e\"]\r\n" + "}";

        String expectedJsonSchema2 = "{\r\n" + "    \"$schema\": \"http://example.org/example/schemas/schema\",\r\n"
                + "    \"$id\": \"http://example.org/example/schemas/schema2\",\r\n" + "    \"title\": \"bar\",\r\n"
                + "    \"description\": \"bar schema\",\r\n" + "    \"$comment\": \"two\",\r\n"
                + "    \"b\": \"2\",\r\n" + "    \"x\": {\r\n" + "        \"title\": \"x\",\r\n"
                + "        \"description\": \"an awesome letter\",\r\n"
                + "        \"$comment\": \"displays nicely\",\r\n" + "        \"type\": \"letter\"\r\n" + "    },\r\n"
                + "    \"array\": [\"b\", \"c\", \"d\"],\r\n"
                + "    \"complexarray\": [{\"a\": [\"b\", {\"c\": \"d\"}]}, \"e\"],\r\n" + "    \"aa\": \"b\",\r\n"
                + "    \"cc\": [\"e\", \"f\", \"g\"]\r\n" + "}";

        JsonNode expectedschema1 = map.readTree(expectedJsonSchema1);
        JsonNode expectedschema2 = map.readTree(expectedJsonSchema2);
        List<JsonNode> expected = new ArrayList<JsonNode>(Arrays.asList(expectedschema1, expectedschema2));
        List<JsonNode> result = s.getRepositorySchemas("repository1");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test: Test getSchemasTest() with two schemas. Should return a list of  both schemas.")
    void getSchemasTest() throws Exception {
        List<String> repository_uris = new ArrayList<String>(Arrays.asList("repository1", "repository2"));
        PassClientSelector selector1 = spy(new PassClientSelector<>(Repository.class, 0, 100, "url='repository1'", null));
        PassClientSelector selector2 = spy(new PassClientSelector<>(Repository.class, 0, 100, "url='repository2'", null));
        when(passClientMock.selectObjects(selector1)).thenReturn(passClientResultMock);
        when(passClientMock.selectObjects(selector2)).thenReturn(passClientResultMock2);
        when(passClientResultMock.getTotal()).thenReturn(1L);
        when(passClientResultMock2.getTotal()).thenReturn(1L);
        when(passClientResultMock.getObjects()).thenReturn(Collections.singletonList(repositoryMock));
        when(passClientResultMock2.getObjects()).thenReturn(Collections.singletonList(repositoryMock2));

        List<URI> r1_schemas_list = Arrays.asList(new URI("/example/schemas/schema1.json"),
                new URI("/example/schemas/schema2.json"));

        // include schema2.json in both repositories to test that it is only returned
        // once
        // include a schema to be dereferenced to make sure that dereferencing is being
        // performed
        List<URI> r2_schemas_list = Arrays.asList(new URI("/example/schemas/schema2.json"),
                new URI("/example/schemas/schema3.json"), new URI("/example/schemas/schema_to_deref.json"));
        when(repositoryMock.getSchemas()).thenReturn(r1_schemas_list);
        when(repositoryMock2.getSchemas()).thenReturn(r2_schemas_list);

        // example/schemas/schema1.json
        String expectedJsonSchema1 = "{\r\n" + "    \"$schema\": \"http://example.org/example/schemas/schema\",\r\n"
                + "    \"$id\": \"http://example.org/example/schemas/foo\",\r\n" + "    \"title\": \"foo\",\r\n"
                + "    \"description\": \"foo schema\",\r\n" + "    \"$comment\": \"one\",\r\n"
                + "    \"a\": \"1\",\r\n" + "    \"x\": {\r\n" + "        \"title\": \"X\",\r\n"
                + "        \"description\": \"a letter\",\r\n" + "        \"$comment\": \"displays good\",\r\n"
                + "        \"type\": \"letter\"\r\n" + "    },\r\n" + "    \"array\": [\"a\", \"b\", \"c\"],\r\n"
                + "    \"aa\": \"b\",\r\n" + "    \"cc\": [\"d\", \"e\"]\r\n" + "}";

        // example/schemas/schema2.json
        String expectedJsonSchema2 = "{\r\n" + "    \"$schema\": \"http://example.org/example/schemas/schema\",\r\n"
                + "    \"$id\": \"http://example.org/example/schemas/schema2\",\r\n" + "    \"title\": \"bar\",\r\n"
                + "    \"description\": \"bar schema\",\r\n" + "    \"$comment\": \"two\",\r\n"
                + "    \"b\": \"2\",\r\n" + "    \"x\": {\r\n" + "        \"title\": \"x\",\r\n"
                + "        \"description\": \"an awesome letter\",\r\n"
                + "        \"$comment\": \"displays nicely\",\r\n" + "        \"type\": \"letter\"\r\n" + "    },\r\n"
                + "    \"array\": [\"b\", \"c\", \"d\"],\r\n"
                + "    \"complexarray\": [{\"a\": [\"b\", {\"c\": \"d\"}]}, \"e\"],\r\n" + "    \"aa\": \"b\",\r\n"
                + "    \"cc\": [\"e\", \"f\", \"g\"]\r\n" + "}";

        // example/schemas/schema3.json
        String expectedJsonSchema3 = "{\r\n" + "    \"$schema\": \"http://example.org/example/schemas/schema\",\r\n"
                + "    \"$id\": \"http://example.org/example/schemas/schema3\",\r\n" + "    \"title\": \"test\",\r\n"
                + "    \"description\": \"test schema\",\r\n" + "    \"$comment\": \"three\",\r\n"
                + "    \"c\": \"3\",\r\n" + "    \"array\": [\"c\", \"d\", \"e\"],\r\n"
                + "    \"complexarray\": [{\"a\": [\"b\", {\"c\": \"d\"}]}, \"f\"],\r\n" + "    \"h\": {\r\n"
                + "        \"i\": \"j\",\r\n" + "        \"k\": [\"l\", \"m\"],\r\n" + "        \"n\": {\r\n"
                + "            \"o\": \"p\"\r\n" + "        }\r\n" + "    }\r\n" + "}";

        // dereferenced version of schema_to_deref.json
        String expectedJsonSchema4 = "{\r\n"
                + "  \"$schema\": \"http://example.org/example/schemas/schema_to_dereference\",\r\n"
                + "  \"$id\": \"http://example.org/example/schemas/deref\",\r\n"
                + "  \"copySchemaName\": \"http://example.org/example/schemas/schema_to_dereference\",\r\n"
                + "  \"schema1_title\": \"X\",\r\n" + "  \"schema2_x\": {\r\n" + "    \"title\": \"x\",\r\n"
                + "    \"description\": \"an awesome letter\",\r\n" + "    \"$comment\": \"displays nicely\",\r\n"
                + "    \"type\": \"letter\"\r\n" + "},\r\n" + "  \"schema3_array\": [\"c\", \"d\", \"e\"],\r\n"
                + "  \"schema4_complexarray\": [\"e\", \"f\", {\"g\": \"h\"}],\r\n"
                + "  \"schema4_hk\": [\"l\", \"m\", \"m'\"]\r\n" + "}";

        JsonNode expectedschema1 = map.readTree(expectedJsonSchema1);
        JsonNode expectedschema2 = map.readTree(expectedJsonSchema2);
        JsonNode expectedschema3 = map.readTree(expectedJsonSchema3);
        JsonNode expectedschema4 = map.readTree(expectedJsonSchema4);
        List<JsonNode> expected = new ArrayList<JsonNode>(
                Arrays.asList(expectedschema1, expectedschema2, expectedschema3, expectedschema4));
        List<JsonNode> result = s.getSchemas(repository_uris);
        assertEquals(expected, result);
    }

    /*@Test
    void invalidSchemaUriTest() throws Exception {
        when(passClientMock.findByAttribute(Repository.class, "@id", new URI("repository1")))
                .thenReturn(new URI("uri_to_repository1"));
        when(passClientMock.readResource(new URI("uri_to_repository1"), Repository.class)).thenReturn(repository1mock);
        List<URI> r1_schemas_list = Arrays.asList(new URI("/example/schemas/invalidschema.json"),
                new URI("/example/schemas/schema2.json"));
        when(repository1mock.getSchemas()).thenReturn(r1_schemas_list);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            s.getRepositorySchemas("repository1");
        });
    }*/

}
