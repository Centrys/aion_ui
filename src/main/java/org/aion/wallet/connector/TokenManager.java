package org.aion.wallet.connector;

import org.aion.api.IAionAPI;
import org.aion.api.IContract;
import org.aion.api.IContractController;
import org.aion.api.impl.internal.ApiUtils;
import org.aion.api.sol.impl.DynamicBytes;
import org.aion.api.sol.impl.Uint;
import org.aion.api.type.ApiMsg;
import org.aion.api.type.ContractResponse;
import org.aion.base.type.Address;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.exception.ValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TokenManager {

    private static final String SEND = "send";
    private static final String BALANCE = "balanceOf";
    private static final String NAME = "name";
    private static final String SYMBOL = "symbol";
    private static final String ABI_JSON = "token_abi.json";
    private static final long TX_NRG_PRICE = 10000000000L;
    private static final long TX_NRG_LIMIT = 1700000L;

    private final Map<Address, IContract> addressToContract = new HashMap<>();
    private final IContractController contractController;
    private final String abiDescription;

    public TokenManager(final IAionAPI api) {
        this.contractController = api.getContractController();
        this.abiDescription = getAbiDescription();
    }

    private String getAbiDescription() {
        final InputStream abiStream = this.getClass().getResourceAsStream(ABI_JSON);
        final StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(abiStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultStringBuilder.toString();
    }

    public final String getName(final String tokenAddress, final String accountAddress) throws ValidationException {
        final IContract contract = getTokenAtAddress(tokenAddress, accountAddress);
        final ApiMsg nameResponse = contract.newFunction(NAME).build().execute();
        if (nameResponse.isError()) {
            throw new ValidationException(nameResponse.getErrString());
        }
        return getTypeResponse(nameResponse);
    }

    public final String getSymbol(final String tokenAddress, final String accountAddress) throws ValidationException {
        final IContract contract = getTokenAtAddress(tokenAddress, accountAddress);
        final ApiMsg symbolResponse = contract.newFunction(SYMBOL).build().execute();
        if (symbolResponse.isError()) {
            throw new ValidationException(symbolResponse.getErrString());
        }
        return getTypeResponse(symbolResponse);
    }

    public final BigInteger getBalance(final String tokenAddress, final String accountAddress) throws ValidationException {
        final IContract contract = getTokenAtAddress(tokenAddress, accountAddress);
        final ApiMsg balanceOf = contract.newFunction(BALANCE)
                .setParam(getApiAddress(accountAddress))
                .build().execute();
        if (balanceOf.isError()) {
            throw new ValidationException(balanceOf.getErrString());
        }
        return getBigIntegerResponse(balanceOf);
    }

    public final byte[] getEncodedSendTokenData(
            final String tokenAddress,
            final String accountAddress,
            final String destinationAddress,
            final BigInteger value
    ) {
        final IContract contract = getTokenAtAddress(tokenAddress, accountAddress);
        return contract.newFunction(SEND)
                .setParam(getApiAddress(destinationAddress))
                .setParam(getUint(value))
                .setParam(DynamicBytes.copyFrom("".getBytes()))
                .setFrom(Address.wrap(accountAddress))
                .setTxNrgLimit(TX_NRG_LIMIT)
                .setTxNrgPrice(TX_NRG_PRICE)
                .build().getEncodedData()
                .getData();
    }

    private BigInteger getBigIntegerResponse(final ApiMsg balanceOf) {
        return TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex((byte[]) getTypeResponse(balanceOf)));
    }

    private <T> T getTypeResponse(final ApiMsg nameResponse) {
        return (T) ((ContractResponse) nameResponse.getObject()).getData().get(0);
    }

    private org.aion.api.sol.impl.Address getApiAddress(final String accountAddress) {
        return org.aion.api.sol.impl.Address.copyFrom(TypeConverter.StringHexToByteArray(accountAddress));
    }

    private org.aion.api.sol.impl.Uint getUint(BigInteger nr) {
        if (nr.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
            return Uint.copyFrom(ApiUtils.toHexPadded16(TypeConverter.StringHexToByteArray(TypeConverter.toJsonHex(nr))));
        } else {
            return Uint.copyFrom(nr.longValueExact());
        }
    }

    private IContract getTokenAtAddress(final String tokenAddressString, final String accountAddressString) {
        final Address tokenAddress = Address.wrap(tokenAddressString);
        final Address accountAddress = Address.wrap(accountAddressString);
        return addressToContract.computeIfAbsent(tokenAddress, s -> contractController.getContractAt(accountAddress, tokenAddress, abiDescription));
    }
}
