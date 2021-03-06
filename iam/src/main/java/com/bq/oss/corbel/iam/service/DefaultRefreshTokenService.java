package com.bq.oss.corbel.iam.service;

import java.text.MessageFormat;
import java.util.Optional;

import com.bq.oss.corbel.iam.auth.AuthorizationRequestContext;
import com.bq.oss.corbel.iam.model.User;
import com.bq.oss.corbel.iam.repository.UserRepository;
import com.bq.oss.lib.token.TokenInfo;
import com.bq.oss.lib.token.exception.TokenVerificationException;
import com.bq.oss.lib.token.factory.TokenFactory;
import com.bq.oss.lib.token.model.TokenType;
import com.bq.oss.lib.token.parser.TokenParser;
import com.bq.oss.lib.token.reader.TokenReader;
import com.bq.oss.lib.token.repository.OneTimeAccessTokenRepository;

/**
 * @author Alberto J. Rubio
 */
public class DefaultRefreshTokenService implements RefreshTokenService {

    private static final String ACCESS_TOKEN_TAG_TEMPLATE = "access_token:{0}";
    private static final String USER_TAG_TEMPLATE = "user:{0}";

    private final TokenParser tokenParser;
    private final UserRepository userRepository;
    private final TokenFactory refreshTokenFactory;
    private final long refreshTokenDurationInSeconds;
    private final OneTimeAccessTokenRepository oneTimeAccessTokenRepository;

    public DefaultRefreshTokenService(TokenParser tokenParser, UserRepository userRepository, TokenFactory refreshTokenFactory,
            long refreshTokenDurationInSeconds, OneTimeAccessTokenRepository oneTimeAccessTokenRepository) {
        this.tokenParser = tokenParser;
        this.userRepository = userRepository;
        this.refreshTokenFactory = refreshTokenFactory;
        this.refreshTokenDurationInSeconds = refreshTokenDurationInSeconds;
        this.oneTimeAccessTokenRepository = oneTimeAccessTokenRepository;
    }

    @Override
    public String createRefreshToken(AuthorizationRequestContext context, String accessToken) {
        String refreshToken = null;
        if (context.hasPrincipal()) {
            refreshToken = refreshTokenFactory.createToken(
                    TokenInfo.newBuilder().setType(TokenType.REFRESH).setState(Long.toString(System.currentTimeMillis()))
                            .setClientId(context.getIssuerClientId()).setOneUseToken(true).setUserId(context.getPrincipal().getId())
                            .build(), refreshTokenDurationInSeconds, userTag(context), accessTokenTag(accessToken)).getAccessToken();
        }
        return refreshToken;
    }

    @Override
    public User getUserFromRefreshToken(String refreshToken) throws TokenVerificationException {
        TokenReader tokenReader = tokenParser.parseAndVerify(refreshToken);
        return userRepository.findOne(tokenReader.getInfo().getUserId());
    }

    @Override
    public void invalidateRefreshToken(String user, Optional<String> accessToken) {
        if (accessToken.isPresent()) {
            oneTimeAccessTokenRepository.deleteByTags(userTag(user), accessTokenTag(accessToken.get()));
        } else {
            oneTimeAccessTokenRepository.deleteByTags(userTag(user));
        }
    }

    private String accessTokenTag(String accessToken) {
        return MessageFormat.format(ACCESS_TOKEN_TAG_TEMPLATE, accessToken);
    }

    private String userTag(AuthorizationRequestContext context) {
        return userTag(context.getPrincipal().getId());
    }

    private String userTag(String userId) {
        return MessageFormat.format(USER_TAG_TEMPLATE, userId);
    }
}
