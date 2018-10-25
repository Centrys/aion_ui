package org.aion.wallet.dto;

public class TokenDetails {
    private final String contractAddress;
    private final String symbol;
    private final double decimals;

    public TokenDetails(final String contractAddress, final String symbol, final double decimals) {
        this.contractAddress = contractAddress;
        this.symbol = symbol;
        this.decimals = decimals;
    }

    public TokenDetails(final String serializedDetails) {
        try {
            final String[] split = serializedDetails.split(":");
            this.contractAddress = split[0];
            this.symbol = split[1];
            this.decimals = Double.valueOf(split[2]);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Invalid connection string: " + serializedDetails, e);
        }
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getDecimals() {
        return decimals;
    }

    public String serialized() {
        return contractAddress + ":" + symbol + ":" + decimals;
    }
}
