package com.bq.oss.corbel.iam.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.bq.oss.corbel.iam.exception.ScopeNameException;
import com.bq.oss.corbel.iam.model.Scope;
import com.bq.oss.corbel.iam.service.ClientService;
import com.bq.oss.corbel.iam.service.DomainService;
import com.bq.oss.corbel.iam.service.ScopeService;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.ResourceTestRule;

public class ScopeResourceTest {
    private final static ScopeService scopeService = mock(ScopeService.class);
    private final static ClientService clientService = mock(ClientService.class);
    private final static DomainService domainService = mock(DomainService.class);

    private final static String SCOPE_ID = "testId";

    @ClassRule public static ResourceTestRule RULE = ResourceTestRule.builder().addResource(new ScopeResource(scopeService)).build();

    @Before
    public void setup() {
        reset(clientService, domainService, scopeService);
    }

    @Test
    public void testCreateScopeWithoutParameters() throws ScopeNameException {

        String scope = "{\"id\": \"testId\", \"audience\" : \"test\", \"rules\": [{\"rule\":\"rule\"}, {\"rule2\":\"rule2\"}]}";

        ArgumentCaptor<Scope> scopeCaptor = ArgumentCaptor.forClass(Scope.class);

        ClientResponse response = RULE.client().resource("/v1.0/scope").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, scope);

        verify(scopeService).create(scopeCaptor.capture());
        assertEquals(SCOPE_ID, scopeCaptor.getValue().getId());
        assertEquals(ClientResponse.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getHeaders().getFirst("Location").endsWith(SCOPE_ID));
    }

    @Test
    public void testCreateScopeWithParameters() throws ScopeNameException {

        String scope = "{\"id\": \"testId\", \"audience\" : \"test\", \"rules\": [{\"rule\":\"rule\"}, "
                + "{\"rule2\":\"rule2\"}], \"parameters\": { \"resourceId\" : \"id\"}}";

        ArgumentCaptor<Scope> scopeCaptor = ArgumentCaptor.forClass(Scope.class);

        ClientResponse response = RULE.client().resource("/v1.0/scope").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, scope);

        verify(scopeService).create(scopeCaptor.capture());
        assertEquals(SCOPE_ID, scopeCaptor.getValue().getId());
        assertEquals(ClientResponse.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getHeaders().getFirst("Location").endsWith(SCOPE_ID));
    }

    @Test
    public void testCreateBadScope() throws ScopeNameException {

        String scope = "{\"id\": \"testId\", \"audience\" : \"test\", \"rules\": [{\"rule\":\"rule\"}, {\"rule2\":\"rule2\"}]}";

        ArgumentCaptor<Scope> scopeCaptor = ArgumentCaptor.forClass(Scope.class);

        doThrow(ScopeNameException.class).when(scopeService).create(any());

        ClientResponse response = RULE.client().resource("/v1.0/scope").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, scope);

        verify(scopeService).create(scopeCaptor.capture());
        assertEquals(SCOPE_ID, scopeCaptor.getValue().getId());
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetScope() {
        Scope expectedScope = new Scope();
        expectedScope.setId(SCOPE_ID);

        when(scopeService.getScope(SCOPE_ID)).thenReturn(expectedScope);

        Scope scope = RULE.client().resource("/v1.0/scope/" + SCOPE_ID).accept(MediaType.APPLICATION_JSON_TYPE).get(Scope.class);

        verify(scopeService).getScope(eq(SCOPE_ID));
        assertEquals(SCOPE_ID, scope.getId());
    }

    @Test
    public void testGetUnknownScope() {
        when(scopeService.getScope(SCOPE_ID)).thenReturn(null);

        ClientResponse response = RULE.client().resource("/v1.0/scope/" + SCOPE_ID).accept(MediaType.APPLICATION_JSON_TYPE)
                .get(ClientResponse.class);

        verify(scopeService).getScope(eq(SCOPE_ID));
        assertEquals(ClientResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteScope() {
        ClientResponse response = RULE.client().resource("/v1.0/scope/" + SCOPE_ID).delete(ClientResponse.class);

        verify(scopeService).delete(eq(SCOPE_ID));
        assertEquals(ClientResponse.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
}