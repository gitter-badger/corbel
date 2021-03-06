package com.bq.oss.corbel.resources.rem.resmi;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.bq.oss.corbel.resources.rem.request.CollectionParameters;
import com.bq.oss.corbel.resources.rem.request.RequestParameters;
import com.bq.oss.corbel.resources.rem.resmi.exception.StartsWithUnderscoreException;
import com.bq.oss.lib.token.TokenInfo;
import com.google.gson.JsonObject;

/**
 * @author Rubén Carrasco
 * 
 */
public class ResmiPostRemTest extends ResmiRemTest {

    private AbstractResmiRem postRem;

    @Override
    @Before
    public void setup() {
        super.setup();
        postRem = new ResmiPostRem(resmiServiceMock);
    }

    @Test
    public void testPostCollectionWithUserToken() throws URISyntaxException, StartsWithUnderscoreException {
        RequestParameters<CollectionParameters> requestParameters = mock(RequestParameters.class);
        TokenInfo tokenInfo = mock(TokenInfo.class);
        when(requestParameters.getTokenInfo()).thenReturn(tokenInfo);
        when(tokenInfo.getUserId()).thenReturn("userId");

        JsonObject testResource = getTestResource();
        when(resmiServiceMock.save(eq(TEST_TYPE), eq(testResource), any())).thenReturn(testResource);

        Response response = postRem.collection(TEST_TYPE, requestParameters, new URI("test"), Optional.of(testResource));

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    public void testPostCollectionWithBadAttributeName() throws URISyntaxException, StartsWithUnderscoreException {
        RequestParameters<CollectionParameters> requestParameters = mock(RequestParameters.class);
        TokenInfo tokenInfo = mock(TokenInfo.class);
        when(requestParameters.getTokenInfo()).thenReturn(tokenInfo);
        when(tokenInfo.getUserId()).thenReturn("userId");

        JsonObject testResource = getTestResource();

        doThrow(new StartsWithUnderscoreException("_any")).when(resmiServiceMock).save(any(), eq(testResource), any());

        Response response = postRem.collection(TEST_TYPE, requestParameters, new URI("test"), Optional.of(testResource));

        assertThat(response.getStatus()).isEqualTo(422);
    }

    @Test
    public void testPostCollectionWithClientToken() throws URISyntaxException, StartsWithUnderscoreException {
        RequestParameters<CollectionParameters> requestParameters = mock(RequestParameters.class);
        TokenInfo tokenInfo = mock(TokenInfo.class);
        when(requestParameters.getTokenInfo()).thenReturn(tokenInfo);
        when(tokenInfo.getUserId()).thenReturn(null);

        JsonObject testResource = getTestResource();
        when(resmiServiceMock.save(eq(TEST_TYPE), eq(testResource), any())).thenReturn(testResource);

        Response response = postRem.collection(TEST_TYPE, requestParameters, new URI("test"), Optional.of(testResource));

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    public void testInvalidPostCollection() {
        Response response = postRem.collection(TEST_TYPE, null, null, Optional.empty());
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    public void testMethodNotAllowed() {
        Response response = postRem.resource(TEST_TYPE, TEST_ID, null, Optional.empty());
        assertThat(response.getStatus()).isEqualTo(405);
    }

}
