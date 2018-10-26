package org.aion.wallet.connector;

import org.aion.api.IAionAPI;
import org.aion.api.IContract;
import org.aion.api.IContractController;
import org.aion.api.impl.internal.ApiUtils;
import org.aion.api.sol.impl.Bytes;
import org.aion.api.sol.impl.Uint;
import org.aion.api.type.ApiMsg;
import org.aion.api.type.ContractResponse;
import org.aion.base.type.Address;
import org.aion.base.util.ByteArrayWrapper;
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
    private final IContractController contractController;

    private final Map<Address, IContract> addressToContract = new HashMap<>();
    private final String abiDescription;

    public TokenManager(final IAionAPI api) {
        contractController = api.getContractController();
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
        final IContract contract = getContractAtAddress(tokenAddress, accountAddress);
        final ApiMsg nameResponse = contract.newFunction(NAME).build().execute();
        if (nameResponse.isError()) {
            throw new ValidationException(nameResponse.getErrString());
        }
        return nameResponse.getObject();
    }

    public final String getSymbol(final String tokenAddress, final String accountAddress) throws ValidationException {
        final IContract contract = getContractAtAddress(tokenAddress, accountAddress);
        final ApiMsg symbolResponse = contract.newFunction(SYMBOL).build().execute();
        if (symbolResponse.isError()) {
            throw new ValidationException(symbolResponse.getErrString());
        }
        return (String) ((ContractResponse) symbolResponse.getObject()).getData().get(0);
    }

    public final BigInteger getBalance(final String tokenAddress, final String accountAddress) throws ValidationException {
        final IContract contract = getContractAtAddress(tokenAddress, accountAddress);
        final ApiMsg balanceOf = contract.newFunction(BALANCE)
                .setParam(getApiAddress(accountAddress))
                .build().execute();
        if (balanceOf.isError()) {
            throw new ValidationException(balanceOf.getErrString());
        }
        ContractResponse contractResponse = balanceOf.getObject();
        return TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex((byte[]) contractResponse.getData().get(0)));
    }

    private org.aion.api.sol.impl.Address getApiAddress(final String accountAddress) {
        return org.aion.api.sol.impl.Address.copyFrom(TypeConverter.StringHexToByteArray(accountAddress));
    }

    public final byte[] getEncodedSendTokenData(
            final String tokenAddress,
            final String accountAddress,
            final String destinationAddress,
            final BigInteger value
    ) {
        final IContract contract = getContractAtAddress(tokenAddress, accountAddress);
        return contract.newFunction(SEND)
                .setParam(getApiAddress(destinationAddress))
                .setParam(getUint(value))
                .setParam(Bytes.copyFrom(ByteArrayWrapper.NULL_BYTE))
                .build().getEncodedData()
                .getData();
    }

    private org.aion.api.sol.impl.Uint getUint(BigInteger nr) {
        if (nr.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
            return Uint.copyFrom(ApiUtils.toHexPadded16(TypeConverter.StringHexToByteArray(TypeConverter.toJsonHex(nr))));
        } else {
            return Uint.copyFrom(nr.longValueExact());
        }
    }

    private IContract getContractAtAddress(final String tokenAddressString, final String accountAddressString) {
        final Address tokenAddress = Address.wrap(tokenAddressString);
        final Address accountAddress = Address.wrap(accountAddressString);
        return addressToContract.computeIfAbsent(tokenAddress, s -> contractController.getContractAt(accountAddress, tokenAddress, abiDescription));
    }
}
