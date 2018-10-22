package org.aion.wallet.dto;

import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.util.*;

public class TokenProvider {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final Set<TokenDetails> tokens = new LinkedHashSet<>();

    public TokenProvider(final Properties tokenKeys) {
        for (Map.Entry<Object, Object> tokenToKey : tokenKeys.entrySet()) {
            final String symbol = (String) tokenToKey.getKey();
            final String tokenDetails = (String) tokenToKey.getValue();
            try {
                tokens.add(new TokenDetails(tokenDetails));
            } catch (IllegalArgumentException e) {
                log.error("skipping configured token key for - " + symbol);
                log.error(e.getMessage(), e);
            }
        }
    }

    public Set<TokenDetails> getAllTokens(final String address) {
        return tokens;
    }

    public Optional<TokenDetails> getTokenDetails(final String symbol) {
        return tokens.stream().filter(p -> p.getSymbol().equals(symbol)).findFirst();
    }

    public void addToken(final TokenDetails tokenDetails) {
        this.tokens.add(tokenDetails);
    }
}
