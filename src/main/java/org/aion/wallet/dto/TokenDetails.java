package org.aion.wallet.dto;

public class TokenDetails {
    private final String contractAddress;
    private final String name;
    private final String symbol;

    public TokenDetails(final String contractAddress, final String name, final String symbol) {
        this.contractAddress = contractAddress;
        this.name = name;
        this.symbol = symbol;
    }

    public TokenDetails(final String symbol, final String serializedDetails) {
        this.symbol = symbol;
        try {
            final String[] split = serializedDetails.split(":");
            this.contractAddress = split[0];
            this.name = split[1];
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Invalid token serialization: " + serializedDetails, e);
        }
    }

    public String getContractAddress() { return contractAddress; }

    public String getSymbol() { return symbol; }

    private String getName() {return name;}

    public String serialized() { return String.format("%s:%s", contractAddress, name); }
}
