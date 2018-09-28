package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.HeaderPaneButtonEvent;
import org.aion.wallet.events.RefreshEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.components.partials.AddAccountDialog;
import org.aion.wallet.ui.components.partials.ImportAccountDialog;
import org.aion.wallet.ui.components.partials.UnlockMasterAccountDialog;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

public class OverviewController extends AbstractController {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private Button addMasterAccountButton;
    @FXML
    private Button unlockMasterAccountButton;
    @FXML
    private ListView<AccountDTO> accountListView;
    private AddAccountDialog addAccountDialog;
    private ImportAccountDialog importAccountDialog;
    private UnlockMasterAccountDialog unlockMasterAccountDialog;

    private AccountDTO account;


    @Override
    public void internalInit(final URL location, final ResourceBundle resources) {
        addAccountDialog = new AddAccountDialog();
        importAccountDialog = new ImportAccountDialog();
        unlockMasterAccountDialog = new UnlockMasterAccountDialog();
        reloadAccounts();
    }

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    private void displayFooterActions() {
        if (blockchainConnector.hasMasterAccount() && !blockchainConnector.isMasterAccountUnlocked()) {
            unlockMasterAccountButton.setVisible(true);
            addMasterAccountButton.setVisible(false);
        } else {
            unlockMasterAccountButton.setVisible(false);
            addMasterAccountButton.setVisible(true);
        }
    }

    private void reloadAccounts() {
        final Task<List<AccountDTO>> getAccountsTask = getApiTask(o -> blockchainConnector.getAccounts(), null);
        runApiTask(
                getAccountsTask,
                evt -> reloadAccountObservableList(getAccountsTask.getValue()),
                getErrorEvent(t -> {}, getAccountsTask),
                getEmptyEvent()
        );
        displayFooterActions();
    }

    private void reloadAccountObservableList(List<AccountDTO> accounts) {
        for (AccountDTO account : accounts) {
            account.setActive(this.account != null && this.account.equals(account));
        }
        accountListView.setItems(FXCollections.observableArrayList(accounts));
    }

    @Subscribe
    private void handleAccountEvent(final AccountEvent event) {
        final AccountDTO account = event.getPayload();
        if (EnumSet.of(AccountEvent.Type.CHANGED, AccountEvent.Type.ADDED).contains(event.getType())) {
            if (account.isActive()) {
                this.account = account;
            }
            reloadAccounts();
        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (account.equals(this.account)) {
                this.account = null;
            }
            reloadAccounts();
        }
    }

    @Override
    protected void refreshView(final RefreshEvent event) {
        switch (event.getType()) {
            case CONNECTED:
            case TRANSACTION_FINISHED:
                reloadAccounts();
        }
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.OVERVIEW)) {
            reloadAccounts();
        }
    }

    public void unlockMasterAccount(MouseEvent mouseEvent) {
        unlockMasterAccountDialog.open(mouseEvent);
    }

    public void openImportAccountDialog(MouseEvent mouseEvent) {
        importAccountDialog.open(mouseEvent);
    }

    public void openAddAccountDialog(MouseEvent mouseEvent) {
        if (this.blockchainConnector.hasMasterAccount()) {
            try {
                blockchainConnector.createAccount();
                ConsoleManager.addLog("New address created", ConsoleManager.LogType.ACCOUNT);
            } catch (ValidationException e) {
                ConsoleManager.addLog("Address cannot be created", ConsoleManager.LogType.ACCOUNT, ConsoleManager.LogLevel.WARNING);
                log.error(e.getMessage(), e);
                // todo: display on yui
            }
            return;
        }
        addAccountDialog.open(mouseEvent);
    }

    public void attemptHardcodedSwap(final MouseEvent mouseEvent) {
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
                BigInteger.valueOf(53000),
                BigInteger.valueOf(100000),
                "0x37Fe03E7ffeA820fEd48f352047Cb7F63fddE554",
                BigInteger.valueOf(1));

// sign & send our transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = "0x" + Hex.toHexString(signedMessage);
        try {
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            System.out.println(ethSendTransaction.getTransactionHash());
            if(ethSendTransaction.hasError()) {
                System.out.println(ethSendTransaction.getError().getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
// ...
    }
}
