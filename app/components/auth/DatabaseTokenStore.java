package components.auth;

import com.google.inject.Inject;
import de.uniulm.omi.cloudiator.persistance.entities.ApiAccessToken;
import de.uniulm.omi.cloudiator.persistance.entities.FrontendUser;
import de.uniulm.omi.cloudiator.persistance.repositories.ApiAccessTokenService;
import de.uniulm.omi.cloudiator.persistance.repositories.FrontendUserService;
import play.Logger;
import util.logging.Loggers;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link TokenStore} implementation using the attached database for persisting tokens.
 */
public class DatabaseTokenStore implements TokenStore {

    private Logger.ALogger LOGGER = Loggers.of(Loggers.AUTH);
    private final FrontendUserService frontendUserService;
    private final ApiAccessTokenService apiAccessTokenService;
    private final TokenValidity tokenValidity;

    @Inject DatabaseTokenStore(FrontendUserService frontendUserService,
        ApiAccessTokenService apiAccessTokenService, TokenValidity tokenValidity) {
        this.frontendUserService = frontendUserService;
        this.apiAccessTokenService = apiAccessTokenService;
        this.tokenValidity = tokenValidity;
    }

    @Override public void store(Token token) {
        LOGGER.trace(String.format("%s is storing new token %s.", this, token));
        FrontendUser frontendUser = frontendUserService.getById(token.userId());
        checkState(frontendUser != null, "user in token not in db");
        ApiAccessToken apiAccessToken = new ApiAccessToken(frontendUser, token.token());
        apiAccessTokenService.save(apiAccessToken);
    }

    @Override public Optional<Token> retrieve(String token) {
        LOGGER.trace(String.format("%s is retrieving token %s", this, token));
        if (tokenValidity.validity() != TokenValidity.INFINITE_VALIDITY) {
            long deadline = tokenValidity.deadline();
            LOGGER.trace(
                String.format("%s is deleting expired tokens after deadline %s.", this, deadline));
            apiAccessTokenService.deleteExpiredTokens(deadline);
        }
        final ApiAccessToken apiAccessToken = apiAccessTokenService.findByToken(token);
        if (apiAccessToken == null) {
            LOGGER.debug(String.format("%s did not find token %s.", this, token));
            return Optional.empty();
        }
        Optional<Token> tokenOptional = Optional
            .of(Token.builder().createdOn(apiAccessToken.getCreatedOn())
                .expiresAt(apiAccessToken.getExpiresAt()).token(apiAccessToken.getToken())
                .userId(apiAccessToken.getFrontendUser().getId()).build());
        LOGGER.trace(String.format("%s retrieved token %s.", this, tokenOptional));
        return tokenOptional;
    }
}
