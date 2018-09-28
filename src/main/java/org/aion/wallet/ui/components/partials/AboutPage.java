package org.aion.wallet.ui.components.partials;

import javafx.scene.input.MouseEvent;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

public class AboutPage {
    private final AboutDialog aboutDialog = new AboutDialog();

    public void openAboutDialog(MouseEvent mouseEvent) {
        aboutDialog.open(mouseEvent);
        Web3j web3j = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/00912daf483e4a7fb8d2e8f320d3393c"));  // defaults to http://localhost:8545/
        Credentials credentials = null;
        try {
            credentials = WalletUtils.loadCredentials("testtesttest", "/home/cristi-i-workstation/Downloads/UTC--2018-09-18T06-59-01.849Z--bed34249b87049526a90592ad4719dc1bf92a0f7");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CipherException e) {
            e.printStackTrace();
        }

// get the next available nonce
        EthGetTransactionCount ethGetTransactionCount = null;
        try {
            ethGetTransactionCount = web3j.ethGetTransactionCount(
                    "0xbEd34249b87049526A90592AD4719Dc1Bf92A0F7", DefaultBlockParameterName.LATEST).sendAsync().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

// create our transaction
        RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(
                nonce,
                BigInteger.valueOf(22000),
                BigInteger.valueOf(41),
                "0x37Fe03E7ffeA820fEd48f352047Cb7F63fddE554",
                BigInteger.valueOf(1));

// sign & send our transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Hex.toHexString(signedMessage);
        try {
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            System.out.println(ethSendTransaction.getTransactionHash());
        } catch (IOException e) {
            e.printStackTrace();
        }
// ...
    }
}
