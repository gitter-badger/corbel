package com.bq.oss.corbel.iam.service;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.bq.oss.corbel.iam.exception.ScopeNameException;
import com.bq.oss.corbel.iam.model.Scope;
import com.bq.oss.corbel.iam.repository.ScopeRepository;
import com.bq.oss.corbel.iam.scope.ScopeFillStrategy;
import com.bq.oss.lib.ws.auth.repository.AuthorizationRulesRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Alexander De Leon
 * 
 */
public class DefaultScopeServiceTest {

    private final Instant now = Instant.now();
    private static final JsonParser JSON_PARSER = new JsonParser();

    private static final long TEST_TOKEN_EXPIRATION_TIME = System.currentTimeMillis() + 36000;
    private static final String TEST_TOKEN = "token";
    private static final String MODULE_A = "moduleA";

    private static final Set<JsonObject> RULES_1 = new HashSet<>(Arrays.asList(new JsonObject()));
    private static final String MODULE_B = "moduleB";
    private static final Set<JsonObject> RULES_2 = new HashSet<>(Arrays.asList(new JsonObject()));
    private static final String TEST_USER_ID = "the_user";
    private static final String TEST_CLIENT_ID = "the_client";

    private static final String TEST_SCOPE_1 = "test_scope1";
    private static final String TEST_SCOPE_2 = "test_scope2";
    private static final String TEST_COMPOSITE_SCOPE = "test_composite_scope";

    private static final String IAM_AUDIENCE = "iamAudience";

    private static final String SCOPE_1_CUSTOM_PARAM_VALUE = "custom";
    private static final String TEST_SCOPE_1_WITH_PARAMS = "test_scope1;testId=" + SCOPE_1_CUSTOM_PARAM_VALUE;
    private static final String TEST_SCOPE_1_WITH_PARAMS_AND_ERRORS = "test_scope1;error;testId=" + SCOPE_1_CUSTOM_PARAM_VALUE;
    private static final String TEST_SCOPE_1_WITHOUT_PARAMS = "test_scope1";
    private static final String TEST_SCOPE_1_WITH_NOT_EXIST_PARAMS = "test_scope1;testId=123456";
    private static final String TEST_SCOPE_1_WITH_WRONG_PARAMS = "test_scope1;testId";
    private static final JsonObject RULE_WITH_PARAMS = JSON_PARSER.parse("{\"uri\" : \"{{testId}}\"}").getAsJsonObject();
    private static final JsonObject RULE_PARAMS = JSON_PARSER.parse("{\"testId\" : \"custom\"}").getAsJsonObject();
    private static final JsonObject RULE_WITH_PARAMS_FILLED = JSON_PARSER.parse("{\"testId\" : \"custom\"}").getAsJsonObject();
    private static final Set<JsonObject> RULES_3 = new HashSet<>(Arrays.asList(RULE_WITH_PARAMS));

    private DefaultScopeService service;
    private ScopeRepository scopeRepositoryMock;
    private AuthorizationRulesRepository authorizationRulesRepositoryMock;
    private ScopeFillStrategy fillStrategyMock;

    @Before
    public void setup() {
        scopeRepositoryMock = mock(ScopeRepository.class);
        authorizationRulesRepositoryMock = mock(AuthorizationRulesRepository.class);
        fillStrategyMock = mock(ScopeFillStrategy.class);
        service = new DefaultScopeService(scopeRepositoryMock, authorizationRulesRepositoryMock, fillStrategyMock, IAM_AUDIENCE,
                Clock.fixed(now, ZoneId.systemDefault()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishAuthorizationRulesIllegalArgument() {
        service.publishAuthorizationRules(TEST_TOKEN, TEST_TOKEN_EXPIRATION_TIME, null, null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPublishAuthorizationRules() {
        Set<String> scopes = new HashSet<>(Arrays.asList("TOKEN_1", "TOKEN_2"));
        String principalId = "principalId";
        String issuerClientId = "issuerClientId";

        Scope scope1 = mock(Scope.class);
        when(scope1.getId()).thenReturn("TOKEN_1");
        when(scope1.getAudience()).thenReturn(MODULE_A);
        when(scope1.getRules()).thenReturn(RULES_1);
        when(scope1.isComposed()).thenReturn(false);

        Scope scope2 = mock(Scope.class);
        when(scope2.getId()).thenReturn("TOKEN_2");
        when(scope2.getAudience()).thenReturn(MODULE_B);
        when(scope2.getRules()).thenReturn(RULES_2);
        when(scope2.isComposed()).thenReturn(false);

        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());

        when(scopeRepositoryMock.findOne(Mockito.eq("TOKEN_1"))).thenReturn(scope1);
        when(scopeRepositoryMock.findOne(Mockito.eq("TOKEN_2"))).thenReturn(scope2);

        when(authorizationRulesRepositoryMock.getKeyForAuthorizationRules(TEST_TOKEN, MODULE_A)).thenReturn(key(TEST_TOKEN, MODULE_A));
        when(authorizationRulesRepositoryMock.getKeyForAuthorizationRules(TEST_TOKEN, MODULE_B)).thenReturn(key(TEST_TOKEN, MODULE_B));

        service.publishAuthorizationRules(TEST_TOKEN, TEST_TOKEN_EXPIRATION_TIME, scopes, principalId, issuerClientId);

        verify(authorizationRulesRepositoryMock).save(key(TEST_TOKEN, MODULE_A), TEST_TOKEN_EXPIRATION_TIME - now.toEpochMilli(),
                array(RULES_1));
        verify(authorizationRulesRepositoryMock).save(key(TEST_TOKEN, MODULE_B), TEST_TOKEN_EXPIRATION_TIME - now.toEpochMilli(),
                array(RULES_2));
    }

    @Test
    public void testGetScope() {
        String id = "scope_id";
        Scope expectedScope = new Scope();
        when(scopeRepositoryMock.findOne(id)).thenReturn(expectedScope);
        assertThat(service.getScope(id)).isSameAs(expectedScope);

    }

    @Test
    public void testFillScope() {
        Scope scope1 = mock(Scope.class);
        when(scope1.getAudience()).thenReturn(MODULE_A);
        when(scope1.getRules()).thenReturn(RULES_1);
        service.fillScope(scope1, TEST_USER_ID, TEST_CLIENT_ID);
        Map<String, String> params = new HashMap<>();
        params.put("userId", TEST_USER_ID);
        params.put("clientId", TEST_CLIENT_ID);
        verify(fillStrategyMock).fillScope(Mockito.same(scope1), Mockito.eq(params));
    }

    @Test
    public void testFillScopeWithCustomParameters() {
        Scope scope1 = mock(Scope.class);
        when(scope1.getAudience()).thenReturn(MODULE_A);
        when(scope1.getRules()).thenReturn(RULES_3);
        when(scope1.getParameters()).thenReturn(RULE_PARAMS);
        service.fillScope(scope1, TEST_USER_ID, TEST_CLIENT_ID);
        Map<String, String> params = new HashMap<>();
        params.put("userId", TEST_USER_ID);
        params.put("clientId", TEST_CLIENT_ID);
        params.put("testId", SCOPE_1_CUSTOM_PARAM_VALUE);
        verify(fillStrategyMock).fillScope(Mockito.same(scope1), Mockito.eq(params));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAuthorizationRules() {
        Set<String> scopes = new HashSet<>(Arrays.asList("TOKEN_1", "TOKEN_2"));

        Scope scope1 = mock(Scope.class);
        when(scope1.getId()).thenReturn("TOKEN_1");
        when(scope1.getAudience()).thenReturn(MODULE_A);
        when(scope1.getRules()).thenReturn(RULES_1);
        when(scope1.isComposed()).thenReturn(false);

        Scope scope2 = mock(Scope.class);
        when(scope2.getId()).thenReturn("TOKEN_2");
        when(scope2.getAudience()).thenReturn(MODULE_B);
        when(scope2.getRules()).thenReturn(RULES_2);
        when(scope2.isComposed()).thenReturn(false);

        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());

        when(scopeRepositoryMock.findOne(Mockito.eq("TOKEN_1"))).thenReturn(scope1);
        when(scopeRepositoryMock.findOne(Mockito.eq("TOKEN_2"))).thenReturn(scope2);

        when(authorizationRulesRepositoryMock.getKeyForAuthorizationRules(TEST_TOKEN, MODULE_A)).thenReturn(key(TEST_TOKEN, MODULE_A));
        when(authorizationRulesRepositoryMock.getKeyForAuthorizationRules(TEST_TOKEN, MODULE_B)).thenReturn(key(TEST_TOKEN, MODULE_B));
        when(authorizationRulesRepositoryMock.getKeyForAuthorizationRules(TEST_TOKEN, IAM_AUDIENCE)).thenReturn(
                key(TEST_TOKEN, IAM_AUDIENCE));
        when(authorizationRulesRepositoryMock.getTimeToExpire(key(TEST_TOKEN, IAM_AUDIENCE))).thenReturn(TEST_TOKEN_EXPIRATION_TIME);

        when(authorizationRulesRepositoryMock.existsRules(key(TEST_TOKEN, MODULE_A))).thenReturn(true);
        when(authorizationRulesRepositoryMock.existsRules(key(TEST_TOKEN, MODULE_B))).thenReturn(false);

        service.addAuthorizationRules(TEST_TOKEN, scopes, TEST_USER_ID, TEST_CLIENT_ID);

        verify(authorizationRulesRepositoryMock).addRules(key(TEST_TOKEN, MODULE_A), array(RULES_1));
        verify(authorizationRulesRepositoryMock).save(key(TEST_TOKEN, MODULE_B), TimeUnit.SECONDS.toMillis(TEST_TOKEN_EXPIRATION_TIME),
                array(RULES_2));
    }

    private String key(String token, String audience) {
        return token + "|" + audience;
    }

    private JsonObject[] array(Set<JsonObject> rules) {
        return rules.toArray(new JsonObject[rules.size()]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testComposedScopes() {
        Scope scope1 = new Scope();
        scope1.setId(TEST_SCOPE_1);
        Scope scope2 = new Scope();
        scope2.setId(TEST_SCOPE_2);
        Scope compositeScope = new Scope();
        compositeScope.setId(TEST_COMPOSITE_SCOPE);
        compositeScope.setType(Scope.COMPOSITE_SCOPE_TYPE);
        HashSet<String> scopesFromCompositScopes = new HashSet<>(Arrays.asList(TEST_COMPOSITE_SCOPE, TEST_SCOPE_1, TEST_SCOPE_2,
                TEST_COMPOSITE_SCOPE));
        compositeScope.setScopes(scopesFromCompositScopes);

        Set<String> requestScopes = new HashSet<>(Arrays.asList(TEST_COMPOSITE_SCOPE));

        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_COMPOSITE_SCOPE))).thenReturn(compositeScope);
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_1))).thenReturn(scope1);
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_2))).thenReturn(scope2);

        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());

        Set<Scope> expandedScopes = service.expandScopes(requestScopes);

        assertThat(expandedScopes).contains(scope1);
        assertThat(expandedScopes).contains(scope2);
        assertThat(expandedScopes).doesNotContain(compositeScope);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testScopeWithCustomParameters() {
        Scope scope1 = new Scope();
        scope1.setId(TEST_SCOPE_1);
        scope1.setAudience(IAM_AUDIENCE);
        scope1.setRules(RULES_3);
        scope1.setParameters(RULE_PARAMS);

        Set<String> requestScopes = new HashSet<>(Arrays.asList(TEST_SCOPE_1_WITH_PARAMS));
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_1))).thenReturn(scope1);
        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());
        Set<Scope> scopes = service.getScopes(requestScopes);
        assertThat(scopes.iterator().next().getParameters()).isEqualTo(RULE_WITH_PARAMS_FILLED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testScopeWithErrorsInCustomParameters() {
        Scope scope1 = new Scope();
        scope1.setId(TEST_SCOPE_1);
        scope1.setAudience(IAM_AUDIENCE);
        scope1.setRules(RULES_3);
        scope1.setParameters(RULE_PARAMS);

        Set<String> requestScopes = new HashSet<>(Arrays.asList(TEST_SCOPE_1_WITH_PARAMS_AND_ERRORS));
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_1))).thenReturn(scope1);
        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());
        Set<Scope> scopes = service.getScopes(requestScopes);
        assertThat(scopes.iterator().next().getParameters()).isEqualTo(RULE_WITH_PARAMS_FILLED);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testScopeWithoutCustomParametersDefined() {
        Scope scope1 = new Scope();
        scope1.setId(TEST_SCOPE_1);
        scope1.setAudience(IAM_AUDIENCE);
        scope1.setRules(RULES_3);
        scope1.setParameters(RULE_PARAMS);

        Set<String> requestScopes = new HashSet<>(Arrays.asList(TEST_SCOPE_1_WITHOUT_PARAMS));
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_1))).thenReturn(scope1);
        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());
        Set<Scope> scopes = service.getScopes(requestScopes);
        assertThat(scopes.iterator().next().getParameters()).isEqualTo(RULE_WITH_PARAMS_FILLED);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testScopeWithNotExistCustomParameters() {
        Scope scope1 = new Scope();
        scope1.setId(TEST_SCOPE_1);
        scope1.setAudience(IAM_AUDIENCE);
        scope1.setRules(RULES_3);
        scope1.setParameters(RULE_PARAMS);

        Set<String> requestScopes = new HashSet<>(Arrays.asList(TEST_SCOPE_1_WITH_NOT_EXIST_PARAMS));
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_1))).thenReturn(scope1);
        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());
        Set<Scope> scopes = service.getScopes(requestScopes);
        assertThat(scopes.iterator().next().getParameters()).isEqualTo(RULE_WITH_PARAMS_FILLED);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testScopeWithWrongCustomParameters() {
        Scope scope1 = new Scope();
        scope1.setId(TEST_SCOPE_1);
        scope1.setAudience(IAM_AUDIENCE);
        scope1.setRules(RULES_3);
        scope1.setParameters(RULE_PARAMS);

        Set<String> requestScopes = new HashSet<>(Arrays.asList(TEST_SCOPE_1_WITH_WRONG_PARAMS));
        when(scopeRepositoryMock.findOne(Mockito.eq(TEST_SCOPE_1))).thenReturn(scope1);
        doAnswer(returnsFirstArg()).when(fillStrategyMock).fillScope(Matchers.<Scope>any(), anyMap());
        Set<Scope> scopes = service.getScopes(requestScopes);
        assertThat(scopes.iterator().next().getParameters()).isEqualTo(RULE_WITH_PARAMS_FILLED);
    }

    @Test
    public void testCreateScope() throws ScopeNameException {
        Scope scope = new Scope();
        scope.setId(TEST_SCOPE_1);

        service.create(scope);
        verify(scopeRepositoryMock).save(scope);
    }


    @Test(expected = ScopeNameException.class)
    public void testCreateIncorrectScope() throws ScopeNameException {
        Scope scope = new Scope();
        scope.setId(";");

        service.create(scope);
    }

    @Test
    public void testDeleteScope() {
        service.delete(TEST_SCOPE_1);
        verify(scopeRepositoryMock).delete(TEST_SCOPE_1);
    }
}
